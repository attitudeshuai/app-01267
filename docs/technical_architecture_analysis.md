# 中药材销售平台 技术架构分析文档

> 本文档基于现有代码分析整理，所有引用均来自实际代码实现

---

## 一、整体架构概览

项目采用前后端分离架构，三端独立部署：

| 端 | 技术栈 | 代码路径 | 访问前缀 |
|----|--------|---------|---------|
| 后端服务 | Spring Boot + MyBatis-Plus + Redis + MySQL | [backend/](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend) | `/admin/*`, `/merchant/*`, `/purchaser/*`, `/common/*`, `/auth/*` |
| 管理后台前端 | Vue 3 + Vite + Element Plus | [frontend-admin/](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/frontend-admin) | `/`（部署后独立域名/端口） |
| 用户端前端（商家/采购商） | Vue 3 + Vite + Element Plus | [frontend-user/](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/frontend-user) | `/`（部署后独立域名/端口） |

---

## 二、三端权限模型与数据隔离

### 2.1 角色定义与用户表分离

系统三种角色各自对应独立数据库表，无统一用户表：

| 角色 | JWT role值 | 数据库表 | Entity类 | 说明 |
|-----|-----------|---------|---------|-----|
| 管理员 | `ADMIN` | `admin_info` | [AdminInfo.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/entity/AdminInfo.java) | 平台运营方 |
| 商户 | `MERCHANT` | `merchant_info` | [MerchantInfo.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/entity/MerchantInfo.java) | 药材商家，需审核 |
| 采购商 | `PURCHASER` | `purchaser_info` | [PurchaserInfo.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/entity/PurchaserInfo.java) | 采购方，注册即启用 |

角色常量定义在 [Constants.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/common/Constants.java#L8-L10)：
```java
public static final String ROLE_ADMIN = "ADMIN";
public static final String ROLE_MERCHANT = "MERCHANT";
public static final String ROLE_PURCHASER = "PURCHASER";
```

### 2.2 JWT认证流程

认证核心类：

1. **JWT工具类** - [JwtUtil.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/util/JwtUtil.java)
   - Token包含字段：`userId`, `username`, `role`
   - 算法：HS512签名
   - Redis存储Token，24小时过期，支持主动注销

2. **认证拦截器** - [AuthInterceptor.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/interceptor/AuthInterceptor.java)
   - 拦截所有路径，排除：`/auth/**`, `/common/**`, `/uploads/**`, `/error`
   - 请求头：`Authorization: Bearer {token}`
   - 解析后将用户信息存入 ThreadLocal：`UserContext`

3. **用户上下文** - [UserContext.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/common/UserContext.java)
   - 基于 `ThreadLocal<UserInfo>` 存储当前请求用户
   - 提供静态方法：`getUserId()`, `getRole()`, `get()`
   - 请求结束后在 `afterCompletion` 中清理

### 2.3 权限隔离实现（三层防护）

#### 第一层：URL路由前缀隔离（Controller层）

三个Controller使用独立的 `@RequestMapping` 前缀：

| Controller | 前缀 | 代码位置 |
|-----------|------|---------|
| AdminController | `/admin` | [AdminController.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/controller/AdminController.java#L19-L21) |
| MerchantController | `/merchant` | [MerchantController.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/controller/MerchantController.java#L36-L38) |
| PurchaserController | `/purchaser` | [PurchaserController.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/controller/PurchaserController.java#L22-L24) |

> **注意**：现有代码未在拦截器或Controller层显式校验 `role` 与URL前缀的匹配关系，而是依赖前端路由守卫和Service层的数据过滤。

#### 第二层：前端路由守卫（前端侧）

**用户端前端**路由守卫 - [frontend-user/src/router/index.js](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/frontend-user/src/router/index.js#L66-L92)：
- `/purchaser/*` 路由 meta: `{ requiresAuth: true, role: 'PURCHASER' }`
- `/merchant/*` 路由 meta: `{ requiresAuth: true, role: 'MERCHANT' }`
- 守卫逻辑：检查 localStorage 中 `token` 和 `userInfo.role`，不匹配则跳转首页

**管理后台前端**路由守卫 - [frontend-admin/src/router/index.js](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/frontend-admin/src/router/index.js#L37-L46)：
- 使用独立的 `admin_token` localStorage key
- 未登录强制跳转 `/login`

#### 第三层：Service层数据强制过滤（后端核心隔离）

所有业务查询都通过当前登录用户ID进行数据过滤，是数据隔离的核心保障：

**商户端数据隔离示例**（MerchantController）：
```java
// 查询本商户药材列表 - 强制传入 UserContext.getUserId() 作为 merchantId
medicineService.page(current, size, keyword, categoryId, UserContext.getUserId(), status)

// 查询本商户订单列表
orderService.page(current, size, null, UserContext.getUserId(), orderStatus, orderNo)

// 发货操作校验 - 校验订单归属
if (order == null || !order.getMerchantId().equals(merchantId)) {
    throw new BusinessException("订单不存在或无权操作");
}
```

**采购商端数据隔离示例**（PurchaserController）：
```java
// 查询我的购物车
cartService.list(UserContext.getUserId())

// 创建订单 - 校验收货地址归属
if (address == null || !address.getPurchaserId().equals(purchaserId)) {
    throw new BusinessException("收货地址无效或不属于当前用户");
}

// 支付/收货/取消/评价 - 每步都校验 purchaserId
if (order == null || !order.getPurchaserId().equals(purchaserId)) {
    throw new BusinessException("订单不存在或无权操作");
}
```

**管理员端无过滤**（AdminController）：
- 调用 `medicineService.page()` 时传入 `merchantId = null`
- 调用 `orderService.page()` 时传入 `purchaserId = null, merchantId = null`
- Mapper XML中 `<if test='xxxId != null'>` 判断，null时不加WHERE条件，返回所有数据

### 2.4 登录注册流程（AuthServiceImpl）

位置：[AuthServiceImpl.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/service/impl/AuthServiceImpl.java)

**登录逻辑（login方法）**：
1. 根据前端传入的 `role` 参数选择查询对应表
2. 校验用户名密码（BCrypt加密）
3. 校验账号状态：
   - 商户需审核通过（status = 1），待审核/拒绝/禁用均拒绝登录
   - 管理员/采购商需 status = 1
4. 生成JWT Token，存入Redis（key: `token:{userId}`）

**注册逻辑**：
- 采购商注册：直接创建，status = 1（启用）
- 商户注册：创建后 status = 0（待审核），需管理员后台审核通过

---

## 三、交易全流程代码流转

### 3.1 状态机定义

订单状态常量（Constants.java）：
```java
ORDER_STATUS_UNPAID = 0;      // 待支付
ORDER_STATUS_PAID = 1;        // 已支付待发货
ORDER_STATUS_SHIPPED = 2;     // 已发货
ORDER_STATUS_RECEIVED = 3;    // 已收货
ORDER_STATUS_COMPLETED = 4;   // 已完成（代码中未主动设置，评价后仍为3）
ORDER_STATUS_CANCELLED = 5;   // 已取消
```

状态流转：
```
待支付(0) ──支付──> 待发货(1) ──发货──> 已发货(2) ──收货──> 已收货(3) ──评价──> (仍为3，评价字段写入order_detail)
   │
   └──取消──> 已取消(5)
```

---

### 3.2 流程1：购物车阶段

#### 数据表结构
`cart_info` 表：
| 字段 | 说明 |
|-----|-----|
| id | 主键 |
| purchaser_id | 采购商ID（隔离键） |
| medicine_id | 药材ID |
| merchant_id | 商户ID（冗余，来自medicine_info） |
| quantity | 数量 |

#### 后端代码入口
位置：[CartServiceImpl.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/service/impl/CartServiceImpl.java) + [PurchaserController.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/controller/PurchaserController.java#L93-L122)

#### 接口列表
| 操作 | 方法 | 路径 | 说明 |
|-----|------|-----|-----|
| 查看购物车 | GET | `/purchaser/cart` | 关联查询 medicine_info、merchant_info、inventory_info |
| 加入购物车 | POST | `/purchaser/cart?medicineId=&quantity=` | 同药材已存在则数量累加，否则新建 |
| 修改数量 | PUT | `/purchaser/cart/{id}?quantity=` | quantity ≤ 0 时删除该项 |
| 删除项 | DELETE | `/purchaser/cart/{id}` | 校验归属后删除 |
| 清空购物车 | DELETE | `/purchaser/cart` | 删除当前purchaserId所有项 |

#### 关键代码片段（添加购物车）
```java
public void add(Long purchaserId, Long medicineId, Integer quantity) {
    MedicineInfo medicine = medicineMapper.selectById(medicineId);
    if (medicine == null || medicine.getStatus() != Constants.MEDICINE_STATUS_ON_SHELF) {
        throw new BusinessException("药材不存在或已下架");
    }
    CartInfo existing = cartMapper.selectOne(/* purchaser_id + medicine_id 查询 */);
    if (existing != null) {
        existing.setQuantity(existing.getQuantity() + quantity);
        cartMapper.updateById(existing);
    } else {
        CartInfo cart = new CartInfo();
        cart.setMerchantId(medicine.getMerchantId()); // 自动绑定商户ID
        // ...
        cartMapper.insert(cart);
    }
}
```

购物车列表关联SQL（CartMapper）：
```sql
SELECT c.*, m.name as medicine_name, m.image, m.price, mi.company_name as merchant_name, inv.stock_quantity
FROM cart_info c
LEFT JOIN medicine_info m ON c.medicine_id = m.id
LEFT JOIN merchant_info mi ON c.merchant_id = mi.id
LEFT JOIN inventory_info inv ON c.medicine_id = inv.medicine_id AND c.merchant_id = inv.merchant_id
WHERE c.purchaser_id = #{purchaserId}
```

---

### 3.3 流程2：创建订单阶段

#### DTO结构
[OrderCreateDTO.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/dto/OrderCreateDTO.java)：
```java
public class OrderCreateDTO {
    private Long addressId;               // 收货地址ID
    private String remark;                // 订单备注
    private List<OrderItemDTO> items;     // 药材列表[{medicineId, quantity}]
}
```

#### 接口
- 路径：`POST /purchaser/orders`
- Controller：`PurchaserController.createOrder()`
- Service：`OrderServiceImpl.createOrder(dto, purchaserId)`

#### 创建订单核心逻辑（事务）
位置：[OrderServiceImpl.java#L97-L189](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/service/impl/OrderServiceImpl.java#L97-L189)

1. **校验收货地址**：address.getPurchaserId() 必须等于当前用户
2. **按商户拆单**（核心设计）：
   - 将items按 `medicineInfo.getMerchantId()` 分组
   - 跨商户购买会自动拆分为多个独立订单
   - 每个订单只有一个 merchant_id
3. **库存检查与扣减**：
   - 查询 inventory_info 校验库存
   - `inv.stock_quantity - item.quantity` 直接扣减（下单即扣库存）
   - `medicine.sales_count += item.quantity` 增加销量
4. **生成订单号**：`ORD + yyyyMMddHHmmss + 4位随机数`
5. **写入 order_info + order_detail**：
   - order_info：记录 purchaser_id, merchant_id, total_amount, order_status=0, 收货信息快照
   - order_detail：冗余存储 medicine_name, medicine_image, price, subtotal（订单快照，不随商品改价变动）
6. **发送商户通知**：插入 `merchant_notification`（type=3 新订单通知）
7. **返回事务中第一个订单ID**（前端跳转到该订单详情）

> **数据模型说明**：订单明细表 `order_detail` 同时承载「订单商品快照」和「评价」两个职责，评价字段(rating/review/review_time)直接存在该表。

---

### 3.4 流程3：支付阶段

#### 接口
- 路径：`PUT /purchaser/orders/{id}/pay`
- Service：`OrderServiceImpl.payOrder(orderId, purchaserId)`

#### 核心逻辑
位置：[OrderServiceImpl.java#L191-L210](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/service/impl/OrderServiceImpl.java#L191-L210)

1. 归属校验：`order.purchaserId == currentUserId`
2. 状态校验：必须为 `ORDER_STATUS_UNPAID(0)`
3. 更新：`order_status = 1`, `pay_time = now()`
4. **推荐记录回写**：标记 recommend_record 中 is_ordered = 1（协同过滤推荐闭环）

> 说明：当前代码未对接真实第三方支付，是模拟支付，直接状态变更。

---

### 3.5 流程4：发货阶段（商户操作）

#### 接口
- 路径：`PUT /merchant/orders/{id}/ship?trackingNo=xxx`
- 前端入口：商家后台订单管理页
- Service：`OrderServiceImpl.shipOrder(orderId, merchantId, trackingNo)`

#### 核心逻辑
位置：[OrderServiceImpl.java#L212-L226](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/service/impl/OrderServiceImpl.java#L212-L226)

1. 归属校验：`order.merchantId == currentMerchantId`
2. 状态校验：必须为 `ORDER_STATUS_PAID(1)`
3. 更新：`order_status = 2`, `ship_time = now()`, `tracking_no = 物流单号`

---

### 3.6 流程5：收货阶段（采购商操作）

#### 接口
- 路径：`PUT /purchaser/orders/{id}/receive`
- 前端入口：订单详情页「确认收货」按钮
- Service：`OrderServiceImpl.receiveOrder(orderId, purchaserId)`

#### 核心逻辑
位置：[OrderServiceImpl.java#L228-L241](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/service/impl/OrderServiceImpl.java#L228-L241)

1. 归属校验：`order.purchaserId == currentUserId`
2. 状态校验：必须为 `ORDER_STATUS_SHIPPED(2)`
3. 更新：`order_status = 3`, `receive_time = now()`

---

### 3.7 流程6：评价阶段（采购商操作）

#### DTO结构
[ReviewDTO.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/dto/ReviewDTO.java)：
```java
public class ReviewDTO {
    private Long orderDetailId;  // 评价维度：订单明细ID（按单品评价，不是按订单）
    private Integer rating;      // 评分 1-5
    private String review;       // 评价文本
}
```

#### 接口
- 路径：`POST /purchaser/orders/review`
- 前端入口：[OrderDetail.vue](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/frontend-user/src/views/purchaser/OrderDetail.vue#L52-L59) 每个订单项的「评价」按钮
- Service：`OrderServiceImpl.reviewOrder(dto, purchaserId)`

#### 核心逻辑
位置：[OrderServiceImpl.java#L271-L289](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/service/impl/OrderServiceImpl.java#L271-L289)

1. 通过 `orderDetailId` 查 order_detail，获取 order_id
2. 再查 order_info，校验归属：`order.purchaserId == currentUserId`
3. 状态校验：`orderStatus >= ORDER_STATUS_RECEIVED(3)`（即已收货/已完成）
4. 更新 order_detail：`rating`, `review`, `review_time = now()`

> 说明：评价是**按单品（order_detail维度）**评价，不是按订单整体评价。订单主表状态不会因评价改变，仍为3。

---

### 3.8 取消订单逻辑

- 接口：`PUT /purchaser/orders/{id}/cancel`
- 状态限制：仅 `ORDER_STATUS_UNPAID(0)` 待支付订单可取消
- 库存回滚：遍历 order_detail，将 `inventory_info.stock_quantity += detail.quantity`（归还库存）
- 订单状态更新为 `ORDER_STATUS_CANCELLED(5)`
- 位置：[OrderServiceImpl.java#L243-L269](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/service/impl/OrderServiceImpl.java#L243-L269)

---

### 3.9 订单列表查询链路（三端复用）

Service入口：`OrderServiceImpl.page(current, size, purchaserId, merchantId, orderStatus, orderNo)`
Mapper：[OrderMapper.selectOrderPage()](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/mapper/OrderMapper.java#L19-L35)

SQL查询逻辑：
```sql
SELECT o.*, p.nickname as purchaser_name, mi.company_name as merchant_name
FROM order_info o
LEFT JOIN purchaser_info p ON o.purchaser_id = p.id
LEFT JOIN merchant_info mi ON o.merchant_id = mi.id
WHERE 1=1
  -- 参数动态拼接：
  <if test='purchaserId != null'>AND o.purchaser_id = #{purchaserId}</if>
  <if test='merchantId != null'>AND o.merchant_id = #{merchantId}</if>
  <if test='orderStatus != null'>AND o.order_status = #{orderStatus}</if>
  <if test='orderNo != null'>AND o.order_no LIKE CONCAT('%',#{orderNo},'%')</if>
ORDER BY o.created_time DESC
```

查询后在Java内存中二次查询order_detail，按order_id分组后装配进OrderVO.details。

三端调用参数：
| 端 | purchaserId | merchantId | 效果 |
|----|------------|-----------|-----|
| 管理员 /admin/orders | null | null | 查全部订单 |
| 商户 /merchant/orders | null | UserContext.getUserId() | 仅查本商户订单 |
| 采购商 /purchaser/orders | UserContext.getUserId() | null | 仅查我的订单 |

---

## 四、质量追溯模块分析

### 4.1 数据结构

追溯字段存储在 **`medicine_info` 表**中，没有独立的追溯表，是药材实体的一部分。

核心字段（[MedicineInfo.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/entity/MedicineInfo.java#L27-L32)）：

| 字段 | 类型 | 数据库列 | 说明 |
|-----|------|---------|-----|
| traceCode | String | `trace_code` | 追溯码，格式 `TC` + 16位大写UUID |
| traceInfo | String | `trace_info` | 追溯信息JSON，TEXT类型存储 |
| origin | String | `origin` | 产地 |
| qualityGrade | String | `quality_grade` | 质量等级（特级/一级/20头等） |

追溯码自动生成逻辑（[MedicineServiceImpl.create()](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/service/impl/MedicineServiceImpl.java#L105-L107)）：
```java
if (medicine.getTraceCode() == null || medicine.getTraceCode().trim().isEmpty()) {
    medicine.setTraceCode("TC" + UUID.randomUUID().toString()
        .replace("-", "").substring(0, 16).toUpperCase());
}
```
- 商户创建药材和管理员创建药材都会自动生成追溯码
- 用户未传入时自动生成，传入则使用用户值

### 4.2 查询链路

#### 链路1：药材列表页（不含追溯详情）
- 公共接口：`GET /common/medicines`
- 采购商接口：`GET /purchaser/medicines`
- SQL（MedicineMapper.selectMedicinePage）返回基础字段，但不返回traceInfo
- 追溯码traceCode在列表页不展示

#### 链路2：药材详情页（完整追溯信息）
1. **Controller入口**：
   - 未登录/游客：`GET /common/medicines/{id}` → CommonController.medicineDetail()
   - 登录采购商：`GET /purchaser/medicines/{id}` → PurchaserController.medicineDetail()（额外返回isCollected）
   - 商户：`GET /merchant/medicines/{id}` → MerchantController.medicineDetail()
   - 管理员：`GET /admin/medicines/{id}` → AdminController.medicineDetail()

2. **Service层**：[MedicineServiceImpl.getDetail(id)](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/service/impl/MedicineServiceImpl.java#L59-L95)
   - 先查Redis缓存：key `medicine:{id}`，缓存2小时
   - 缓存未命中：查 medicine_info 表，BeanUtils.copyProperties 到 MedicineVO
   - 补充库存信息：关联查 inventory_info 表，填充 stockQuantity, warningThreshold
   - 写入Redis缓存
   - 更新药材/删除药材时删除对应Redis缓存

3. **前端展示**：[MedicineDetail.vue](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/frontend-user/src/views/MedicineDetail.vue#L68-L73)
```vue
<el-tab-pane label="溯源信息" name="trace">
  <div class="tab-content">
    <p v-if="medicine.traceInfo">{{ medicine.traceInfo }}</p>
    <el-empty v-else description="暂无溯源信息" />
  </div>
</el-tab-pane>
```

### 4.3 追溯数据写入入口

追溯数据通过药材编辑接口写入，没有独立的追溯维护接口：
- 商户端：`PUT /merchant/medicines` → MedicineServiceImpl.update(dto, merchantId)
- 管理员端：`PUT /admin/medicines` → MedicineServiceImpl.adminUpdate(medicine)
- 字段随MedicineDTO一起传入，使用BeanUtils.copyProperties复制到实体

> **实现边界说明**：
> 1. `trace_code` 是系统自动生成的字符串标识，非扫码/防伪校验码
> 2. `trace_info` 是TEXT字段，前端直接以纯文本展示，当前代码未定义其内部JSON schema，也无解析逻辑
> 3. 数据库 schema 中公告提到"全面启用质量追溯系统"，但代码层面仅提供存储和展示字段，追溯信息的录入依赖商户/管理员在编辑药材时手动填写
> 4. 产地origin、质量等级qualityGrade作为结构化字段单独存储，在详情顶部信息区直接展示；完整追溯文档在trace_info中自由文本存储

---

## 五、关联模块补充说明

### 5.1 Redis缓存使用清单
位置：[Constants.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/common/Constants.java#L12-L15)

| Key前缀 | 用途 | 过期时间 |
|--------|-----|---------|
| `token:{userId}` | JWT Token会话存储 | 24小时 |
| `medicine:{id}` | 药材详情VO缓存 | 2小时 |
| `hot:medicines` | 热门药材列表 | 1小时 |
| `recommend:{purchaserId}` | 用户个性化推荐结果 | 配置项（默认1小时） |

### 5.2 通知机制
`merchant_notification` 表用于商户站内信，触发场景：
- type=1：库存预警（InventoryWarningTask定时任务扫描）
- type=2：价格异常波动（修改价格超过30%阈值）
- type=3：新订单（创建订单后立即插入）
- type=4：药材审核结果（管理员审核通过/拒绝后）
- type=5：系统公告（未在现有代码中看到使用）

### 5.3 操作日志
`operation_log` 表通过AOP切面 [OperationLogAspect.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/aspect/OperationLogAspect.java) 自动记录，operator_type区分1管理员/2商户/3采购商。

### 5.4 药材审核流程
1. 商户创建药材 → status=0（待审核）
2. 管理员在后台列表看到，调用 `PUT /admin/medicines/{id}/status?status=1` 通过
3. 或 status=3 拒绝
4. 通过/拒绝都会发送 merchant_notification(type=4) 通知商户
5. 采购商端 /common/medicines 和 /purchaser/medicines 强制只查 status=1（已上架）的药材

---

## 六、关键文件索引

### 后端核心文件
| 模块 | 文件 |
|-----|-----|
| 认证授权 | [AuthInterceptor.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/interceptor/AuthInterceptor.java), [JwtUtil.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/util/JwtUtil.java), [UserContext.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/common/UserContext.java) |
| 登录注册 | [AuthServiceImpl.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/service/impl/AuthServiceImpl.java) |
| 订单交易 | [OrderServiceImpl.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/service/impl/OrderServiceImpl.java), [OrderMapper.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/mapper/OrderMapper.java) |
| 购物车 | [CartServiceImpl.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/service/impl/CartServiceImpl.java), [CartMapper.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/mapper/CartMapper.java) |
| 药材/追溯 | [MedicineServiceImpl.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/service/impl/MedicineServiceImpl.java), [MedicineInfo.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/entity/MedicineInfo.java) |
| Web配置 | [WebMvcConfig.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/config/WebMvcConfig.java), [Constants.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/java/com/medicine/sales/common/Constants.java) |
| 数据库结构 | [schema.sql](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/backend/src/main/resources/schema.sql) |

### 前端关键页面
| 功能 | 页面（用户端） | 页面（管理端） |
|-----|--------------|--------------|
| 药材详情/溯源展示 | [MedicineDetail.vue](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/frontend-user/src/views/MedicineDetail.vue) | [MedicineManage.vue](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/frontend-admin/src/views/MedicineManage.vue) |
| 购物车 | [Cart.vue](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/frontend-user/src/views/purchaser/Cart.vue) | - |
| 结算下单 | [Checkout.vue](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/frontend-user/src/views/purchaser/Checkout.vue) | - |
| 订单详情/支付/收货/评价 | [OrderDetail.vue](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/frontend-user/src/views/purchaser/OrderDetail.vue) | [OrderManage.vue](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/frontend-admin/src/views/OrderManage.vue) |
| 商户订单/发货 | [merchant/Orders.vue](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/frontend-user/src/views/merchant/Orders.vue) | - |
| 路由守卫 | [router/index.js（用户端）](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/frontend-user/src/router/index.js) | [router/index.js（admin）](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01267/app-01267-autumn/frontend-admin/src/router/index.js) |

---

## 七、核心设计要点总结

1. **三端数据隔离核心保障**：Service层每步操作都显式校验 `merchantId == UserContext.getUserId()` 或 `purchaserId == UserContext.getUserId()`，这是最终安全屏障，不依赖前端路由和URL前缀约定。

2. **跨商户拆单设计**：一个结算请求包含不同商户商品时，系统自动按商户拆成多个独立订单，每个订单有独立的状态流转，符合平台型电商的多商户架构。

3. **库存扣减时机**：创建订单（下单）即扣减库存，而非支付时扣减；取消订单（仅待支付可取消）时归还库存。

4. **订单快照设计**：order_detail冗余存储 medicine_name, medicine_image, price，订单创建后商品改价/改名/删除不影响历史订单展示。

5. **评价粒度**：按订单明细（单品）评价，非按订单整体评价，同一个订单不同商品可以分别评分写评价。

6. **追溯模块实现状态**：提供了数据存储结构（traceCode、traceInfo、origin、qualityGrade）、查询链路、前端展示入口，但traceInfo是自由文本，未定义结构化JSON schema，无校验和独立维护UI，追溯内容完全依赖药材编辑时手动填写。
