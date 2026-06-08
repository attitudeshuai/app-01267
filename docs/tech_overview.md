# 技术架构与核心模块说明

> 本文档基于 `app-01267-summer` 仓库当前代码整理，覆盖三端权限模型与数据隔离、交易全流程链路、质量追溯模块的数据结构与查询链路。
> 架构保持「前后端分离 + 三端独立部署」的现状（管理后台 / 用户端 / 后端 + MySQL + Redis），不在本文中提出架构变更建议。

---

## 1. 整体架构与部署形态

通过 [docker-compose.yml](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/docker-compose.yml) 可见系统由 5 个独立服务组成：

| 服务 | 镜像/源码 | 端口 |
|---|---|---|
| `mysql` | mysql:8.0，初始化脚本来自 [schema.sql](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/resources/schema.sql) | 3306 |
| `redis` | redis:7-alpine | 6379 |
| `backend` | Spring Boot，入口 [MedicineApplication.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/MedicineApplication.java) | 8080 |
| `frontend-admin` | Vue 3 + Vite，Nginx 静态托管 | 8081 |
| `frontend-user` | Vue 3 + Vite，Nginx 静态托管 | 8082 |

两个前端通过 Nginx 反代到后端 `/api/`（参考 [frontend-admin/nginx.conf](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/frontend-admin/nginx.conf#L11-L17)），后端按角色把 REST 接口拆分到三个 Controller：
- 管理端 → [AdminController.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/controller/AdminController.java)（`/admin/**`）
- 商户端 → [MerchantController.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/controller/MerchantController.java)（`/merchant/**`）
- 采购商端 → [PurchaserController.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/controller/PurchaserController.java)（`/purchaser/**`）
- 公共/未登录访问 → [CommonController.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/controller/CommonController.java)（`/common/**`）、[AuthController.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/controller/AuthController.java)（`/auth/**`）

---

## 2. 三端的权限模型与数据隔离

### 2.1 角色与账户表
角色常量在 [Constants.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/common/Constants.java#L8-L10) 中定义为 `ADMIN / MERCHANT / PURCHASER`，分别对应三张独立的账户表（[schema.sql](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/resources/schema.sql#L11-L56)）：

- `admin_info`（管理员）
- `merchant_info`（商户，含审核状态：0 待审核 / 1 已通过 / 2 已拒绝 / 3 已禁用）
- `purchaser_info`（采购商）

三张表的 `id` 命名空间彼此独立，登录后写入 token 中的 `userId` 仅在「自身角色对应的表」里有意义，这是后端做数据隔离的基础。

### 2.2 登录与令牌签发
登录入口 [AuthServiceImpl#login](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/service/impl/AuthServiceImpl.java#L44-L114)：
- 根据 `LoginDTO.role`（大写）分支匹配 `admin / merchant / purchaser` 三张表，分别使用 `PasswordUtil.matches`（BCrypt）校验密码；
- 商户登录会显式拒绝 `MERCHANT_STATUS_PENDING / REJECTED / DISABLED` 状态；
- 校验通过后调用 [JwtUtil#generateToken](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/util/JwtUtil.java#L22-L34) 把 `userId / username / role` 写入 JWT；
- 同时把 token 写入 Redis：key 为 `Constants.REDIS_TOKEN_PREFIX + userId`，TTL 24 小时，作为强制下线/失效的依据。

### 2.3 后端鉴权拦截器
所有非白名单接口都被 [AuthInterceptor](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/interceptor/AuthInterceptor.java#L29-L64) 处理：
1. 校验 `Authorization: Bearer <token>` 格式；
2. 校验 JWT 是否过期；
3. 校验 Redis 中 `token:<userId>` 是否仍存在（登出/被踢会被清除）；
4. 解析出 `userId / username / role`，写入 [UserContext](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/common/UserContext.java)（基于 `ThreadLocal`）。

放行路径在 [WebMvcConfig](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/config/WebMvcConfig.java#L22-L31)：`/auth/**`、`/common/**`、`/uploads/**`、`/error`。

### 2.4 角色到接口的映射
当前实现没有显式的「角色 → URL」白名单（即拦截器只校验「已登录」），角色边界是通过 **Controller 划分 + 业务层使用 `UserContext.getUserId()` 作为强制过滤条件** 来保证的：

- 商户 Controller 永远以登录态的 `merchantId = UserContext.getUserId()` 作为查询条件，例如商户分页商品 [MerchantController#medicinePage](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/controller/MerchantController.java#L126-L134) 会把当前登录用户作为 `merchantId` 传给分页查询。
- 采购商 Controller 同样以 `purchaserId = UserContext.getUserId()` 过滤购物车、订单、地址（参考 [PurchaserController.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/controller/PurchaserController.java#L94-L196)）。
- 在写操作的 Service 中还会做 **归属校验**：例如下单地址必须属于当前用户（[OrderServiceImpl#createOrder](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/service/impl/OrderServiceImpl.java#L100-L103)）；商户更新药材必须是自己的（[MedicineServiceImpl#update](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/service/impl/MedicineServiceImpl.java#L130-L137)）；订单状态变更必须是订单的归属方（[OrderServiceImpl#payOrder](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/service/impl/OrderServiceImpl.java#L191-L210)、`shipOrder`、`receiveOrder`、`cancelOrder`、`reviewOrder`）。

注：这种「Controller 路由 + 业务层强归属过滤」的组合意味着鉴权依赖业务代码自觉传 `UserContext.getUserId()`，本文不评价该设计的优劣，仅描述现状。

### 2.5 前端的角色门禁
两个前端都通过 `localStorage` + Vue Router 守卫做前置拦截：

- 管理端 [frontend-admin/router/index.js](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/frontend-admin/src/router/index.js#L37-L46)：除 `/login` 外都要求 `admin_token` 存在。
- 用户端 [frontend-user/router/index.js](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/frontend-user/src/router/index.js#L66-L92)：以路由 `meta.role` 为 `PURCHASER` 或 `MERCHANT` 进一步区分用户中心与商户中心；登录态用户访问 `/login` 会按角色重定向到对应首页。
- 真正的安全依赖仍在后端拦截器与业务层归属校验，前端守卫只是 UX 优化。

### 2.6 数据隔离边界一览

| 资源 | 隔离字段 | 关键代码 |
|---|---|---|
| 商品 | `medicine_info.merchant_id` | [MedicineMapper#selectMedicinePage](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/mapper/MedicineMapper.java#L17-L35) 接受 `merchantId` 参数 |
| 库存 | `inventory_info.merchant_id + medicine_id` 唯一键 | [schema.sql 的 inventory_info](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/resources/schema.sql#L135-L144) |
| 价格记录 | `price_record.merchant_id` | [MerchantController#priceHistory](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/controller/MerchantController.java#L221-L228) 同时按 `medicineId + merchantId` 查 |
| 订单 | `order_info.purchaser_id` 与 `merchant_id` | [OrderMapper#selectOrderPage](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/mapper/OrderMapper.java#L19-L35) |
| 购物车 | `cart_info.purchaser_id` | [CartServiceImpl](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/service/impl/CartServiceImpl.java) |
| 收货地址 | `address_info.purchaser_id` | [PurchaserController](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/controller/PurchaserController.java#L168-L196) |
| 商户消息 | `merchant_notification.merchant_id` | [MerchantController#notifications](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/controller/MerchantController.java#L102-L123) |

### 2.7 操作日志（横切层）
[OperationLogAspect](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/aspect/OperationLogAspect.java) 切入所有 Controller 的非 GET 方法，把 `userId / role / controller / method / IP` 写入 `operation_log`，作为审计日志。`role` 与 `operator_type` 的映射见 [`roleToType`](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/aspect/OperationLogAspect.java#L91-L99)。

---

## 3. 交易全流程链路

订单状态机定义在 [Constants.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/common/Constants.java#L17-L22)：

```
ORDER_STATUS_UNPAID    = 0   待支付
ORDER_STATUS_PAID      = 1   已支付/待发货
ORDER_STATUS_SHIPPED   = 2   已发货
ORDER_STATUS_RECEIVED  = 3   已收货
ORDER_STATUS_COMPLETED = 4   已完成
ORDER_STATUS_CANCELLED = 5   已取消
```

> 注：代码中没有把 `RECEIVED → COMPLETED` 自动推进的逻辑，完成态目前来自历史数据/管理动作。评价校验只要求 `>= ORDER_STATUS_RECEIVED`。

### 3.1 购物车
- 表：`cart_info`（按 `purchaser_id` 聚合）。
- 加购：[CartServiceImpl#add](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/service/impl/CartServiceImpl.java#L33-L55) 校验药材必须 `MEDICINE_STATUS_ON_SHELF`，已存在则累加数量，新建时同时记录 `merchantId`。
- 改/删/清空：[CartServiceImpl#updateQuantity / delete / clear](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/service/impl/CartServiceImpl.java#L57-L83)；改写前都会校验 `cart.purchaserId == 当前登录 ID`。
- 前端入口：[Cart.vue](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/frontend-user/src/views/purchaser/Cart.vue)，结算前选中的项通过 `sessionStorage.checkoutItems` 透传到 [Checkout.vue](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/frontend-user/src/views/purchaser/Checkout.vue#L86-L96)。

### 3.2 下单（购物车 → 订单）
入口 [PurchaserController#createOrder](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/controller/PurchaserController.java#L125-L128) 调用 [OrderServiceImpl#createOrder](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/service/impl/OrderServiceImpl.java#L97-L189)（`@Transactional`）：

1. 校验地址归属（地址必须属于当前 `purchaserId`）；
2. 按 `merchantId` 对入参 `items` 分组——**每个商户生成一张独立订单**（多商户购物车会拆单）；
3. 对每个商品：
   - 校验状态为 `MEDICINE_STATUS_ON_SHELF`；
   - 按 `(medicineId, merchantId)` 查 `inventory_info`，库存不足直接抛 `BusinessException`；
   - 计算 `subtotal = price * quantity`；
   - **扣减库存** 与 **增加 `medicine_info.sales_count`**；
4. 写入 `order_info`（订单号 `ORD + yyyyMMddHHmmss + 4 位随机数`，`order_status = UNPAID`，地址直接打平为字符串）和 `order_detail`；
5. 生成商户通知（`merchant_notification.type=3` 新订单），失败仅 warn，不阻断主流程；
6. 返回首张订单 ID（前端由 [Checkout.vue](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/frontend-user/src/views/purchaser/Checkout.vue#L110-L130) 在成功后跳转到「我的订单」）。

下单 DTO 结构见 [OrderCreateDTO.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/dto/OrderCreateDTO.java)，同时注意购物车记录在下单后并未自动清理，由前端控制。

### 3.3 支付
- API：`PUT /purchaser/orders/{id}/pay`
- 实现：[OrderServiceImpl#payOrder](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/service/impl/OrderServiceImpl.java#L191-L210)
  - 必须 `purchaserId` 匹配，状态必须是 `UNPAID`；
  - 写 `order_status = PAID`、`pay_time = now`；
  - 调 `medicineService.recordRecommendOrdered(...)`，把推荐记录里这些 `medicineId` 标记为「已下单」（用于推荐效果回流，参见第 4.4 节）。

> 当前没有真实支付网关，`payOrder` 是一次性原子状态推进。

### 3.4 发货（商户）
- API：`PUT /merchant/orders/{id}/ship?trackingNo=...`
- 实现：[OrderServiceImpl#shipOrder](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/service/impl/OrderServiceImpl.java#L212-L226)
  - 必须 `merchantId` 匹配（来自 `UserContext.getUserId()`），订单状态必须是 `PAID`；
  - 写 `order_status = SHIPPED`、`ship_time = now`、`tracking_no` 落库。
- 商户端入口：[merchant/Orders.vue](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/frontend-user/src/views/merchant/Orders.vue#L46-L56) 的发货弹窗。

### 3.5 收货
- API：`PUT /purchaser/orders/{id}/receive`
- 实现：[OrderServiceImpl#receiveOrder](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/service/impl/OrderServiceImpl.java#L228-L241)
  - 必须 `purchaserId` 匹配，状态必须是 `SHIPPED`；
  - 写 `order_status = RECEIVED`、`receive_time = now`。

### 3.6 取消
- API：`PUT /purchaser/orders/{id}/cancel`
- 实现：[OrderServiceImpl#cancelOrder](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/service/impl/OrderServiceImpl.java#L243-L269)
  - 仅 `UNPAID` 可取消；
  - **回退库存**：遍历 `order_detail`，对每条按 `(medicineId, merchantId)` 累加回 `inventory_info.stock_quantity`；
  - 写 `order_status = CANCELLED`。

### 3.7 评价
- API：`POST /purchaser/orders/review`，DTO：[ReviewDTO.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/dto/ReviewDTO.java)
- 实现：[OrderServiceImpl#reviewOrder](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/service/impl/OrderServiceImpl.java#L271-L289)
  - 评价以 **`order_detail.id` 为粒度**（即每条明细单独评分/留言）；
  - 校验明细所属订单的 `purchaserId` 必须等于当前用户；
  - 状态必须 `>= ORDER_STATUS_RECEIVED`；
  - 写入 `order_detail.rating / review / review_time`。

### 3.8 链路图

```
[Cart.vue] --addToCart--> POST /purchaser/cart
                                   |
                                   v
[Checkout.vue] --createOrder--> POST /purchaser/orders
                                   |  OrderServiceImpl#createOrder
                                   |   - 校验地址、商品上架、库存
                                   |   - 按 merchantId 拆单
                                   |   - 扣减 inventory + 累计 salesCount
                                   |   - insert order_info / order_detail
                                   |   - merchant_notification(type=3)
                                   v
                            order_status = 0 (UNPAID)
                                   |
                purchaser pay  --> PUT /purchaser/orders/{id}/pay
                                   |   payTime, recordRecommendOrdered
                                   v
                            order_status = 1 (PAID)
                                   |
                merchant ship  --> PUT /merchant/orders/{id}/ship?trackingNo=
                                   |   shipTime, trackingNo
                                   v
                            order_status = 2 (SHIPPED)
                                   |
                purchaser receive -> PUT /purchaser/orders/{id}/receive
                                   |   receiveTime
                                   v
                            order_status = 3 (RECEIVED)
                                   |
                purchaser review -> POST /purchaser/orders/review
                                   |   写 order_detail.rating/review
                                   v
                            order_status 不变（保持 3，至完成态由历史数据/外部赋值）

  ↑ purchaser cancel（仅 UNPAID）→ PUT /purchaser/orders/{id}/cancel
       回退 inventory_info.stock_quantity，order_status = 5
```

### 3.9 列表查询
管理端 / 商户端 / 采购商端的订单列表共用 [OrderMapper#selectOrderPage](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/mapper/OrderMapper.java#L19-L35)，差异只在 Controller 传入的 `purchaserId / merchantId`：

| 调用方 | `purchaserId` | `merchantId` |
|---|---|---|
| `AdminController#orderPage` | null | null |
| `MerchantController#orderPage` | null | `UserContext.getUserId()` |
| `PurchaserController#orderPage` | `UserContext.getUserId()` | null |

[OrderServiceImpl#page](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/service/impl/OrderServiceImpl.java#L49-L73) 在分页结果上批量补充 `order_detail`，并填充 `statusText`。

---

## 4. 质量追溯模块的数据结构与查询链路

「质量追溯」当前由三类相互配合的数据组成：**药材自身的追溯字段**、**价格记录链路**、**库存记录与预警**。它们都按 `(medicineId, merchantId)` 形成可追溯的时间线。

### 4.1 数据模型
核心实体（持久化字段以 `schema.sql` 为准）：

- 药材主体：[MedicineInfo](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/entity/MedicineInfo.java)
  - `traceCode`：质量追溯码（创建时若空则自动生成 `TC + UUID 16 位`，见 [MedicineServiceImpl#create](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/service/impl/MedicineServiceImpl.java#L105-L107)）；
  - `traceInfo`：自由文本/JSON 形式的追溯信息（产地、批号、检测报告链接等，由商户在 [merchant/Medicines.vue](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/frontend-user/src/views/merchant/Medicines.vue#L114) 维护）；
  - 元数据：`origin / qualityGrade / specification / unit`；
  - `stockQuantity / warningThreshold` 在实体上标注为 `@TableField(exist = false)`，仅作为编辑表单的传输字段，**真实数据落在 `inventory_info`**。
- 库存：[InventoryInfo](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/entity/InventoryInfo.java) / `inventory_info`，`(medicine_id, merchant_id)` 唯一键。
- 价格历史：[PriceRecord](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/entity/PriceRecord.java) / `price_record`，字段 `price / isAbnormal / recordTime`。
- 商户通知：`merchant_notification.type` 取值 `1=库存预警 / 2=价格异常 / 3=新订单 / 4=药材审核 / 5=系统公告`（参见 [schema.sql](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/resources/schema.sql#L262-L273)）。

### 4.2 写入链路（追溯数据如何积累）

**新增药材**（[MedicineServiceImpl#create](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/service/impl/MedicineServiceImpl.java#L98-L126) / `adminCreate`）：
- 同事务内 insert `medicine_info`（含 `traceCode / traceInfo`）+ `inventory_info`（初始库存与阈值）+ 第一条 `price_record`（`isAbnormal=0`）。

**修改药材**（[MedicineServiceImpl#update](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/service/impl/MedicineServiceImpl.java#L128-L173) / `adminUpdate`）：
- 比较旧价 vs 新价：变动则 insert 一条新的 `price_record`；
- 通过 [`isPriceAbnormal`](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/service/impl/MedicineServiceImpl.java#L526-L532) 判定异常（系统配置 `price_abnormal_rate`，默认 0.3 即 30%），异常时同时写入 `merchant_notification(type=2)`，并落库 `is_abnormal=1`。
- 库存数变更同步更新 `inventory_info`。

**下单/取消** 直接更新 `inventory_info.stock_quantity`（见 3.2、3.6）。

**库存预警定时任务** [InventoryWarningTask](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/task/InventoryWarningTask.java)：每 1 小时（`fixedRate = 3600000`）调用 [InventoryMapper#selectLowStockInventories](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/mapper/InventoryMapper.java#L13-L14) 取出所有 `stock_quantity <= warning_threshold` 的记录，按 `merchantId` 聚合后：
- 把汇总数据写到 Redis（`inventory:warning:<merchantId>`，TTL 24h）供前端首页展示；
- 6 小时内未读的同类型通知不存在时，才写入新的 `merchant_notification(type=1)`，避免重复推送。

### 4.3 读取链路

| 场景 | 入口 | 关键 SQL/方法 |
|---|---|---|
| 药材详情（含追溯码、追溯信息、库存） | `GET /common/medicines/{id}` 或 `GET /purchaser/medicines/{id}` | [MedicineServiceImpl#getDetail](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/service/impl/MedicineServiceImpl.java#L60-L95)：先读 `medicine:<id>` Redis 缓存（2h），未命中则查 `medicine_info` 并联表 `inventory_info` 补 `stockQuantity / warningThreshold`，回写缓存 |
| 药材列表（含库存、商户名、分类名） | 三端通用 | [MedicineMapper#selectMedicinePage](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/mapper/MedicineMapper.java#L17-L35) 三表联查 `medicine_info × medicine_category × merchant_info × inventory_info` |
| 价格历史（用于价格分析） | 商户：`GET /merchant/price-history/{id}`；管理员：`GET /admin/price-history/{id}` | 商户端 [MerchantController#priceHistory](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/controller/MerchantController.java#L221-L228) 用 `(medicineId, merchantId)` 双条件过滤；管理员端 [AdminController#priceHistory](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/controller/AdminController.java#L250-L253) 仅按 `medicineId` 查所有商户的历史 |
| 库存预警 | `GET /merchant/inventory/low-stock`（商户）/ `GET /admin/inventory/low-stock`（管理员） | 商户端通过 `merchantId + stock_quantity <= warning_threshold` 过滤后联表补充药材信息；管理员端委托 [InventoryMapper#selectLowStockInventories](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/mapper/InventoryMapper.java#L13-L14) |
| 价格分析页 | 管理端 [PriceAnalysis.vue](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/frontend-admin/src/views/PriceAnalysis.vue) | 选中药材 → 拉 `priceHistory` → 在前端计算 max/min/变化率，并以柱状图展示；同时支持读取/修改 `price_abnormal_rate` 配置 |
| 详情页溯源 | [MedicineDetail.vue](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/frontend-user/src/views/MedicineDetail.vue#L68-L73) | 展示 `medicine.traceInfo` 文本与基础元数据（产地、品质等级、规格、库存、销量、商家） |

### 4.4 推荐与质量数据的交叉
推荐功能（与质量追溯相邻但独立）使用 `recommend_record` 与订单数据：[MedicineServiceImpl#getRecommendList](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/service/impl/MedicineServiceImpl.java#L309-L346) 基于历史采购计算用户余弦相似度，[OrderMapper](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/mapper/OrderMapper.java#L48-L73) 中三段 SQL（`getPurchaseMedicineIds / getSimilarPurchasers / getHighSimilarityPurchaseMedicines`）+ [MedicineMapper#getRecommendMedicines](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/mapper/MedicineMapper.java#L37-L54) 完成查询；点击与下单回流分别由 [recordRecommendClick](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/service/impl/MedicineServiceImpl.java#L385-L401) 与 [recordRecommendOrdered](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/service/impl/MedicineServiceImpl.java#L403-L423) 触发——后者由「支付成功」事件驱动，构成「订单 → 推荐效果」的闭环。

### 4.5 追溯查询时序（以「采购商查看某药材的完整追溯」为例）

```
Frontend (MedicineDetail.vue)
    │  GET /api/common/medicines/{id} 或 /purchaser/medicines/{id}
    ▼
CommonController / PurchaserController#medicineDetail
    │   → MedicineService.getDetail(id)
    ▼
MedicineServiceImpl.getDetail
    │   1) Redis: medicine:<id>  (TTL 2h) ── hit ─▶ 返回
    │                            └── miss ─▶
    │   2) medicine_info.selectById        → traceCode / traceInfo / 商户/分类
    │   3) inventory_info (medicineId, merchantId) → 当前库存与阈值
    │   4) 写回 Redis
    ▼
返回 MedicineVO (含 traceCode / traceInfo / stockQuantity / merchantName)

[管理员/商户路径] GET /merchant/price-history/{medicineId}
                        → price_record where medicine_id = ? and merchant_id = ? order by record_time
                          形成「价格 + isAbnormal 标记」的时间序列
```

### 4.6 一致性与缓存
- 写操作（`update / updateStatus / delete`）会主动 `redisTemplate.delete(REDIS_MEDICINE_PREFIX + id)`，避免缓存陈旧。
- `inventory_info` 与 `medicine_info.sales_count` 的同步是在下单事务内完成的，订单取消时按相同主键回退；价格历史只追加不更新，是天然的不可变审计流。

---

## 5. 关键约定速查

- 时间字段统一通过 [MyMetaObjectHandler](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/config/MyMetaObjectHandler.java) 的 `@TableField(fill=...)` 自动填充。
- 异常通过 [BusinessException](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/exception/BusinessException.java) 抛出，由 [GlobalExceptionHandler](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/exception/GlobalExceptionHandler.java) 统一转 `Result.code != 200`，前端 [request.js](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/frontend-user/src/api/request.js#L21-L33) 对非 200 提示 `ElMessage.error` 并在 401 时清空登录态。
- 后端常量、枚举（订单/商户/药材状态、Redis key 前缀）集中在 [Constants.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-summer/backend/src/main/java/com/medicine/sales/common/Constants.java)。

