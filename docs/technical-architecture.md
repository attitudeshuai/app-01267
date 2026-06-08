# 中药材销售平台 — 技术架构文档

> 基于现有代码分析，覆盖三端权限模型与数据隔离、交易全流程、质量追溯模块三大主题。

---

## 目录

- [1. 项目架构概览](#1-项目架构概览)
- [2. 三端权限模型与数据隔离](#2-三端权限模型与数据隔离)
  - [2.1 角色体系与三表分离](#21-角色体系与三表分离)
  - [2.2 JWT + Redis 双重认证](#22-jwt--redis-双重认证)
  - [2.3 认证拦截器](#23-认证拦截器)
  - [2.4 角色区分与 Token 写入](#24-角色区分与-token-写入)
  - [2.5 后端数据隔离实现](#25-后端数据隔离实现)
  - [2.6 前端路由守卫与请求拦截](#26-前端路由守卫与请求拦截)
  - [2.7 数据库层隔离支撑](#27-数据库层隔离支撑)
  - [2.8 权限模型架构总图](#28-权限模型架构总图)
- [3. 交易全流程](#3-交易全流程)
  - [3.1 订单状态机](#31-订单状态机)
  - [3.2 购物车](#32-购物车)
  - [3.3 结算与下单](#33-结算与下单)
  - [3.4 支付](#34-支付)
  - [3.5 发货](#35-发货)
  - [3.6 收货](#36-收货)
  - [3.7 评价](#37-评价)
  - [3.8 取消订单与库存回退](#38-取消订单与库存回退)
  - [3.9 交易链路时序图](#39-交易链路时序图)
  - [3.10 前后端接口对照表](#310-前后端接口对照表)
- [4. 质量追溯模块](#4-质量追溯模块)
  - [4.1 追溯实体与表结构](#41-追溯实体与表结构)
  - [4.2 药品追溯码与追溯信息](#42-药品追溯码与追溯信息)
  - [4.3 价格变动追踪](#43-价格变动追踪)
  - [4.4 库存预警机制](#44-库存预警机制)
  - [4.5 推荐系统与追溯关联](#45-推荐系统与追溯关联)
  - [4.6 操作审计日志](#46-操作审计日志)
  - [4.7 用户反馈闭环](#47-用户反馈闭环)
  - [4.8 追溯数据链路关系图](#48-追溯数据链路关系图)
  - [4.9 管理端追溯查询接口](#49-管理端追溯查询接口)
  - [4.10 前端追溯展示页面](#410-前端追溯展示页面)

---

## 1. 项目架构概览

项目采用前后端分离 + 三端部署架构：

| 端 | 前端目录 | 后端路径前缀 | 角色 |
|----|---------|-------------|------|
| 管理端 | `frontend-admin/` | `/admin/**` | ADMIN |
| 用户端（商户） | `frontend-user/` → `/merchant/**` | `/merchant/**` | MERCHANT |
| 用户端（采购方） | `frontend-user/` → `/purchaser/**` | `/purchaser/**` | PURCHASER |

后端为单体 Spring Boot 应用，三端共用同一后端，通过 URL 路径前缀区分角色。前端管理端为独立 Vue SPA，用户端为统一 Vue SPA，内部通过路由区分商户/采购方视图。

技术栈：
- 后端：Spring Boot + MyBatis-Plus + MySQL + Redis
- 前端：Vue 3 + Element Plus + Vite
- 认证：JWT (HS512) + Redis 双重校验

---

## 2. 三端权限模型与数据隔离

### 2.1 角色体系与三表分离

三种角色存储在完全独立的数据库表中，ID 各自独立自增：

| 角色 | 常量 | 数据库表 | 实体类 |
|------|------|---------|--------|
| 管理员 | `ROLE_ADMIN` | `admin_info` | `AdminInfo` |
| 商户 | `ROLE_MERCHANT` | `merchant_info` | `MerchantInfo` |
| 采购方 | `ROLE_PURCHASER` | `purchaser_info` | `PurchaserInfo` |

常量定义于 `Constants.java`：

```java
public static final String ROLE_ADMIN = "ADMIN";
public static final String ROLE_MERCHANT = "MERCHANT";
public static final String ROLE_PURCHASER = "PURCHASER";
```

**关键设计**：三张表的 ID 独立自增，`userId=1` 可能同时存在于三张表中。角色区分完全依赖 JWT Claims 中的 `role` 字段，而非 userId 的唯一性。

商户额外拥有审核状态机制：

```java
public static final int MERCHANT_STATUS_PENDING = 0;   // 待审核
public static final int MERCHANT_STATUS_APPROVED = 1;   // 已通过
public static final int MERCHANT_STATUS_REJECTED = 2;   // 已拒绝
public static final int MERCHANT_STATUS_DISABLED = 3;   // 已禁用
```

登录时校验商户状态，非 APPROVED 状态会拒绝登录并给出相应提示。

### 2.2 JWT + Redis 双重认证

#### Token 生成

`JwtUtil.generateToken(userId, username, role)` 生成 Token，Payload 包含三个自定义字段：

```java
claims.put("userId", userId);
claims.put("username", username);
claims.put("role", role);
```

- 签名算法：HS512
- 过期时间：`jwt.expiration = 86400000`（24小时），配置于 `application.yml`
- 密钥：配置于 `application.yml` 的 `jwt.secret`

#### Token 存储

登录成功时，Token 写入 Redis：

```java
String redisKey = Constants.REDIS_TOKEN_PREFIX + vo.getUserId();  // "token:{userId}"
redisTemplate.opsForValue().set(redisKey, token, 24, TimeUnit.HOURS);
```

同一用户重复登录会覆盖旧 Token，实现单点登录效果。

#### Token 注销

```java
public void logout() {
    Long userId = UserContext.getUserId();
    if (userId != null) {
        redisTemplate.delete(Constants.REDIS_TOKEN_PREFIX + userId);
    }
}
```

注销后旧 Token 因 Redis 中无对应记录而失效。

#### Token 刷新

当前未实现 Token 自动刷新机制。Token 过期后用户必须重新登录。JWT 过期时间与 Redis 缓存过期时间均为 24 小时，任一过期均导致认证失败。

### 2.3 认证拦截器

`AuthInterceptor` 实现 `HandlerInterceptor`，在 `preHandle` 中执行双重校验：

1. 放行 OPTIONS 预检请求
2. 检查 `Authorization` Header 是否以 `Bearer ` 开头
3. 验证 JWT Token 是否过期
4. **关键步骤**：用 `userId` 去 Redis 查找 `token:{userId}`，不存在则拒绝
5. 解析 Claims，将 `userId`、`username`、`role` 封装到 `UserContext.UserInfo` 并存入 ThreadLocal

```java
Claims claims = jwtUtil.parseToken(token);
Long userId = Long.valueOf(claims.get("userId").toString());
String username = claims.get("username").toString();
String role = claims.get("role").toString();
UserContext.set(new UserContext.UserInfo(userId, username, role));
```

请求完成后在 `afterCompletion` 中清理 ThreadLocal：

```java
UserContext.clear();
```

拦截器注册于 `WebMvcConfig`，排除路径：

```java
.excludePathPatterns("/auth/**", "/common/**", "/uploads/**", "/error")
```

**重要说明**：拦截器只做认证（Authentication），不做授权（Authorization）。不校验角色与接口路径的匹配关系。

### 2.4 角色区分与 Token 写入

登录时角色由前端在 `LoginDTO` 中传入：

```java
@Data
public class LoginDTO {
    @NotBlank private String username;
    @NotBlank private String password;
    @NotBlank private String role;   // 前端指定
}
```

`AuthServiceImpl.login()` 根据 `role` 字段决定查询哪张表：

```java
switch (role) {
    case Constants.ROLE_ADMIN:    // 查 admin_info
    case Constants.ROLE_MERCHANT: // 查 merchant_info（含审核状态判断）
    case Constants.ROLE_PURCHASER: // 查 purchaser_info
    default: throw new BusinessException("无效的用户角色");
}
```

前端角色指定方式：

- 管理端 `frontend-admin/src/api/auth.js`：硬编码 `role: 'ADMIN'`
- 用户端 `frontend-user/src/api/auth.js`：`role` 由用户在登录表单中选择

登录成功后角色写入 JWT Claims 和返回的 `LoginVO`：

```java
String token = jwtUtil.generateToken(vo.getUserId(), vo.getUsername(), role);
vo.setRole(role);
vo.setToken(token);
```

### 2.5 后端数据隔离实现

#### AdminController — 无数据隔离（全局视角）

管理员接口不传 `merchantId` 或 `purchaserId`，可查看所有数据：

```java
@GetMapping("/orders")
public Result<IPage<OrderVO>> orderPage(...) {
    return Result.success(orderService.page(current, size, null, null, orderStatus, orderNo));
}
```

#### MerchantController — 基于 merchantId 隔离

核心模式：`UserContext.getUserId()` 作为 `merchantId` 传入查询条件。

```java
@GetMapping("/medicines")
public Result<IPage<MedicineVO>> medicinePage(...) {
    return Result.success(medicineService.page(current, size, keyword, categoryId,
                                               UserContext.getUserId(), status));
}

@GetMapping("/orders")
public Result<IPage<OrderVO>> orderPage(...) {
    return Result.success(orderService.page(current, size, null,
                                            UserContext.getUserId(), orderStatus, orderNo));
}
```

Service 层的归属校验（`MedicineServiceImpl`）：

```java
public void update(MedicineDTO dto, Long merchantId) {
    MedicineInfo existing = medicineMapper.selectById(dto.getId());
    if (merchantId != null && !existing.getMerchantId().equals(merchantId)) {
        throw new BusinessException("无权限更新此药材，药材ID: " + dto.getId());
    }
}
```

`OrderServiceImpl` 的发货校验：

```java
public void shipOrder(Long orderId, Long merchantId, String trackingNo) {
    OrderInfo order = orderMapper.selectById(orderId);
    if (order == null || !order.getMerchantId().equals(merchantId)) {
        throw new BusinessException("订单不存在或无权操作");
    }
}
```

#### PurchaserController — 基于 purchaserId 隔离

与 Merchant 类似，`UserContext.getUserId()` 作为 `purchaserId`：

```java
@GetMapping("/cart")
public Result<List<CartVO>> cartList() {
    return Result.success(cartService.list(UserContext.getUserId()));
}

@PostMapping("/addresses")
public Result<Void> createAddress(@Valid @RequestBody AddressInfo address) {
    address.setPurchaserId(UserContext.getUserId());  // 强制设置归属
    addressService.create(address);
}
```

`OrderServiceImpl` 中的支付/收货/取消/评价校验：

```java
public void payOrder(Long orderId, Long purchaserId) {
    OrderInfo order = orderMapper.selectById(orderId);
    if (order == null || !order.getPurchaserId().equals(purchaserId)) {
        throw new BusinessException("订单不存在或无权操作");
    }
}
```

创建订单时的地址归属校验：

```java
AddressInfo address = addressMapper.selectById(dto.getAddressId());
if (address == null || !address.getPurchaserId().equals(purchaserId)) {
    throw new BusinessException("收货地址无效或不属于当前用户");
}
```

### 2.6 前端路由守卫与请求拦截

#### 管理端（frontend-admin）

`frontend-admin/src/router/index.js`：

```javascript
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('admin_token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/dashboard')
  } else {
    next()
  }
})
```

- Token 存储键：`admin_token`
- 只检查 Token 是否存在，不校验角色（管理端只有 ADMIN 登录）

#### 用户端（frontend-user）

`frontend-user/src/router/index.js`：

```javascript
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')

  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!token) {
      next({ name: 'Login', query: { redirect: to.fullPath } })
      return
    }
    const requiredRole = to.matched.find(record => record.meta.role)?.meta.role
    if (requiredRole && userInfo?.role !== requiredRole) {
      next({ name: 'Home' })
      return
    }
  }
  // ...
})
```

路由元信息配置：

```javascript
{ path: '/purchaser', meta: { requiresAuth: true, role: 'PURCHASER' }, children: [...] }
{ path: '/merchant',  meta: { requiresAuth: true, role: 'MERCHANT' },  children: [...] }
```

- 公共页面（首页、药材列表、登录、注册）不需要认证
- `/purchaser/**` 需要 PURCHASER 角色
- `/merchant/**` 需要 MERCHANT 角色

#### 请求拦截器

两端均使用 axios 拦截器：

- 请求拦截：自动附加 `Authorization: Bearer {token}`
- 响应拦截：`code === 401` 时清除本地存储并跳转登录页

### 2.7 数据库层隔离支撑

所有业务数据表通过 `merchant_id` 或 `purchaser_id` 字段实现数据归属：

| 表名 | 租户字段 | 唯一约束/索引 |
|------|---------|-------------|
| `medicine_info` | `merchant_id` | `INDEX idx_merchant (merchant_id)` |
| `inventory_info` | `merchant_id` | `UNIQUE uk_medicine_merchant (medicine_id, merchant_id)` |
| `price_record` | `merchant_id` | `INDEX idx_medicine (medicine_id)` |
| `order_info` | `purchaser_id` + `merchant_id` | `INDEX idx_purchaser`, `INDEX idx_merchant` |
| `cart_info` | `purchaser_id` + `merchant_id` | `INDEX idx_purchaser` |
| `address_info` | `purchaser_id` | `INDEX idx_purchaser` |
| `collection_info` | `purchaser_id` | `UNIQUE uk_purchaser_medicine (purchaser_id, medicine_id)` |
| `recommend_record` | `purchaser_id` | `INDEX idx_purchaser`, `INDEX idx_purchaser_medicine` |
| `feedback_info` | `purchaser_id` | `INDEX idx_purchaser` |
| `merchant_notification` | `merchant_id` | `INDEX idx_merchant` |

### 2.8 权限模型架构总图

```
[前端路由守卫] ── 角色校验 + Token 检查
       │
[HTTP Request] ── Authorization: Bearer {jwt}
       │
[AuthInterceptor] ── Token 有效性 + Redis 存在性 ──→ UserContext (ThreadLocal)
       │
[Controller] ── UserContext.getUserId() 作为 merchantId / purchaserId
       │
[Service] ── 归属校验 (merchantId / purchaserId 匹配)
       │
[Database] ── merchant_id / purchaser_id 字段 + 索引
```

核心设计思想：
- 认证靠 JWT + Redis 双重验证
- 角色区分靠 JWT Claims 中的 `role` 字段
- 数据隔离靠 `UserContext.getUserId()` 作为查询条件
- 前端隔离靠路由 meta 中的 `requiresAuth` 和 `role` 配置
- 数据库隔离靠业务表中的 `merchant_id` / `purchaser_id` 字段

---

## 3. 交易全流程

### 3.1 订单状态机

订单状态常量定义于 `Constants.java`：

```java
public static final int ORDER_STATUS_UNPAID = 0;     // 待支付
public static final int ORDER_STATUS_PAID = 1;        // 已支付（待发货）
public static final int ORDER_STATUS_SHIPPED = 2;     // 已发货
public static final int ORDER_STATUS_RECEIVED = 3;    // 已收货
public static final int ORDER_STATUS_COMPLETED = 4;   // 已完成
public static final int ORDER_STATUS_CANCELLED = 5;   // 已取消
```

状态流转图：

```
[0:待支付] ──支付──→ [1:待发货] ──发货──→ [2:已发货] ──收货──→ [3:已收货] ──(评价)──→ [4:已完成]*
     │
     └──取消──→ [5:已取消]
```

> *注：状态 4（已完成）在当前代码中没有可达路径。`receiveOrder` 将状态设为 3，`reviewOrder` 不改变订单状态。前端订单列表 Tab 中展示了"已完成"标签，但后端缺少从 3 到 4 的转换逻辑。

状态转换规则：

| 当前状态 | 允许操作 | 目标状态 | 校验位置 |
|---------|---------|---------|---------|
| 0（待支付） | `payOrder` | 1（待发货） | `OrderServiceImpl:198` |
| 0（待支付） | `cancelOrder` | 5（已取消） | `OrderServiceImpl:249` |
| 1（待发货） | `shipOrder` | 2（已发货） | `OrderServiceImpl:218` |
| 2（已发货） | `receiveOrder` | 3（已收货） | `OrderServiceImpl:234` |
| 3+（已收货及以上） | `reviewOrder` | 不改变状态 | `OrderServiceImpl:281` |

### 3.2 购物车

#### 数据模型

`CartInfo` 实体：

```java
@TableName("cart_info")
public class CartInfo {
    private Long id;
    private Long purchaserId;
    private Long medicineId;
    private Long merchantId;     // 从药材信息冗余
    private Integer quantity;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
```

`CartVO` 展示层（多表 JOIN 聚合）：

```java
public class CartVO {
    private Long id;
    private Long medicineId;
    private String medicineName;     // ← medicine_info
    private String medicineImage;    // ← medicine_info
    private BigDecimal price;        // ← medicine_info
    private Integer quantity;
    private Long merchantId;
    private String merchantName;     // ← merchant_info
    private Integer stockQuantity;   // ← inventory_info
}
```

#### 购物车查询 — 四表联查

`CartMapper` 使用一条 SQL 完成 `cart_info` + `medicine_info` + `merchant_info` + `inventory_info` 的联查：

```sql
SELECT c.id, c.medicine_id, m.name AS medicine_name, m.image AS medicine_image,
       m.price, c.quantity, c.merchant_id, mi.company_name AS merchant_name,
       IFNULL(inv.stock_quantity, 0) AS stock_quantity
FROM cart_info c
LEFT JOIN medicine_info m ON c.medicine_id = m.id
LEFT JOIN merchant_info mi ON c.merchant_id = mi.id
LEFT JOIN inventory_info inv ON c.medicine_id = inv.medicine_id AND c.merchant_id = inv.merchant_id
WHERE c.purchaser_id = #{purchaserId}
ORDER BY c.created_time DESC
```

#### 添加购物车 — 合并同商品

`CartServiceImpl.add()` 逻辑：

1. 校验药材存在且已上架（`status == 1`）
2. 查找该采购商是否已有同药材的购物车项
3. 已存在则累加 `quantity`
4. 不存在则新建，`merchantId` 从 `MedicineInfo.getMerchantId()` 获取

### 3.3 结算与下单

#### 前端数据传递

`Cart.vue` → `Checkout.vue` 通过 `sessionStorage` 传递选中商品：

```javascript
// Cart.vue
function handleCheckout() {
  const items = cartItems.value.filter(i => i.checked)
  sessionStorage.setItem('checkoutItems', JSON.stringify(items))
  router.push('/purchaser/checkout')
}

// Checkout.vue
onMounted(() => {
  const stored = sessionStorage.getItem('checkoutItems')
  if (stored) {
    checkoutItems.value = JSON.parse(stored)
  } else {
    router.push('/purchaser/cart')
  }
  fetchAddresses()
})
```

#### 提交订单

`Checkout.vue` 将 `CartVO` 转换为 `OrderCreateDTO`：

```javascript
async function handleSubmit() {
  const items = checkoutItems.value.map(i => ({
    medicineId: i.medicineId || i.id,
    quantity: i.quantity
  }))
  await createOrder({ addressId: selectedAddressId.value, remark: remark.value, items })
  sessionStorage.removeItem('checkoutItems')
  router.push('/purchaser/orders')
}
```

`OrderCreateDTO` 结构：

```java
@Data
public class OrderCreateDTO {
    @NotNull private Long addressId;
    private String remark;
    @NotEmpty private List<OrderItemDTO> items;
    
    @Data
    public static class OrderItemDTO {
        @NotNull private Long medicineId;
        @NotNull @Min(1) private Integer quantity;
    }
}
```

#### 后端下单 — 按商户拆单

`OrderServiceImpl.createOrder()` 是交易流程的核心方法，标注 `@Transactional`：

1. 校验收货地址归属（`address.purchaserId == purchaserId`）
2. **按商户分组**：同一商户的商品生成一个订单，不同商户自动拆单
3. 遍历每个商户组：
   - 生成订单号：`ORD` + `yyyyMMddHHmmss` + 4 位随机数
   - 遍历商品，**扣减库存** + 计算金额 + 更新销量
   - 创建 `OrderInfo` 主记录（初始状态 = 0 待支付）
   - 创建 `OrderDetail` 明细记录（快照药品名称、图片、价格）
   - 发送商户通知（`type=3` 新订单）
4. 返回第一个订单 ID

**关键设计决策**：
- 库存扣减时机：下单时立即扣减（预扣模式），非支付后扣减
- 下单后不清空购物车，需用户手动清理
- 收货地址以拼接字符串快照存入 `receiver_address`，地址修改不影响历史订单

### 3.4 支付

`OrderServiceImpl.payOrder()`：

```java
@Transactional
public void payOrder(Long orderId, Long purchaserId) {
    OrderInfo order = orderMapper.selectById(orderId);
    // 权限校验：purchaserId 匹配
    // 状态校验：只有待支付(0)才能支付
    order.setOrderStatus(Constants.ORDER_STATUS_PAID);  // 0 → 1
    order.setPayTime(LocalDateTime.now());
    orderMapper.updateById(order);
    // 记录推荐已购买（协同过滤用）
    medicineService.recordRecommendOrdered(purchaserId, medicineIds);
}
```

前端交互（`purchaser/Orders.vue`）：

```javascript
async function handlePay(order) {
  await ElMessageBox.confirm('确认支付该订单？', '支付确认', { type: 'info' })
  await payOrder(order.id)   // PUT /purchaser/orders/{id}/pay
  fetchOrders()
}
```

> 当前为模拟支付系统，无真实支付网关接入。支付流程仅做状态变更和支付时间记录，无金额校验、支付渠道和回调机制。

### 3.5 发货

`OrderServiceImpl.shipOrder()`：

```java
public void shipOrder(Long orderId, Long merchantId, String trackingNo) {
    OrderInfo order = orderMapper.selectById(orderId);
    // 权限校验：merchantId 匹配
    // 状态校验：只有已支付(1)才能发货
    order.setOrderStatus(Constants.ORDER_STATUS_SHIPPED);  // 1 → 2
    order.setShipTime(LocalDateTime.now());
    order.setTrackingNo(trackingNo);
    orderMapper.updateById(order);
}
```

商户端发货对话框需输入快递单号（`trackingNo`），调用 `PUT /merchant/orders/{id}/ship?trackingNo=`。

### 3.6 收货

`OrderServiceImpl.receiveOrder()`：

```java
public void receiveOrder(Long orderId, Long purchaserId) {
    OrderInfo order = orderMapper.selectById(orderId);
    // 权限校验：purchaserId 匹配
    // 状态校验：只有已发货(2)才能确认收货
    order.setOrderStatus(Constants.ORDER_STATUS_RECEIVED);  // 2 → 3
    order.setReceiveTime(LocalDateTime.now());
    orderMapper.updateById(order);
}
```

### 3.7 评价

#### 评价存储在 OrderDetail 中

评价不是独立表，而是 `order_detail` 表的字段：

```java
// OrderDetail 中的评价字段
private Integer rating;        // 评分 1-5
private String review;         // 评价内容
private LocalDateTime reviewTime;
```

`ReviewDTO`：

```java
public class ReviewDTO {
    @NotNull private Long orderDetailId;   // 订单明细ID（非订单ID）
    @NotNull @Min(1) @Max(5) private Integer rating;
    private String review;
}
```

#### 评价逻辑

`OrderServiceImpl.reviewOrder()`：

1. 根据 `orderDetailId` 查找 `OrderDetail`
2. 根据 `detail.orderId` 查找 `OrderInfo`
3. 权限校验：`order.purchaserId == purchaserId`
4. 状态校验：订单状态 >= 3（已收货）
5. 更新 `OrderDetail` 的 `rating`、`review`、`reviewTime`

**评价不改变订单状态**，订单评价后仍为 3（已收货）。

前端评价按钮显示条件（`OrderDetail.vue`）：

```html
<el-button
  v-if="(order.orderStatus === 4 || order.orderStatus === 3) && !item.review"
  @click="openReview(item)"
>评价</el-button>
```

#### Feedback 与评价的区别

`FeedbackInfo` 是独立的平台意见反馈系统，与订单评价无关：

```java
private Integer type;    // 1-功能建议 2-问题反馈 3-投诉 4-其他
private Integer status;  // 0-待处理 1-已处理 2-已关闭
private String reply;    // 管理员回复
```

### 3.8 取消订单与库存回退

`OrderServiceImpl.cancelOrder()`：

1. 状态校验：只有待支付(0)才能取消
2. **回退库存**：遍历 `OrderDetail`，将 `quantity` 加回 `inventory_info.stock_quantity`
3. 状态设为 5（已取消）

> 注意：取消订单时只回退库存，不回退 `medicine_info.sales_count`。

### 3.9 交易链路时序图

```
采购方前端                    后端                         商户前端
   │                          │                            │
   │── GET /cart ────────────→│                            │
   │←─ CartVO列表 ───────────│                            │
   │                          │                            │
   │── 选中商品,sessionStorage │                            │
   │── 跳转Checkout ─────────→│                            │
   │── GET /addresses ───────→│                            │
   │←─ AddressInfo列表 ──────│                            │
   │                          │                            │
   │── POST /orders ─────────→│  (按商户拆单)               │
   │   {addressId,items}      │── 扣减库存                  │
   │                          │── 更新销量                  │
   │                          │── 创建OrderInfo+OrderDetail │
   │                          │── 发送商户通知 ────────────→│
   │←─ orderId ──────────────│                            │
   │                          │                            │
   │── PUT /orders/{id}/pay ─→│── 状态0→1                  │
   │                          │── 记录推荐已购买            │
   │←─ 200 ──────────────────│                            │
   │                          │                            │
   │                          │←─ GET /merchant/orders ────│
   │                          │── OrderVO列表 ────────────→│
   │                          │                            │
   │                          │←─ PUT /orders/{id}/ship ──│
   │                          │── 状态1→2 + trackingNo ───→│
   │                          │                            │
   │── PUT /orders/{id}/receive→│── 状态2→3                │
   │←─ 200 ──────────────────│                            │
   │                          │                            │
   │── POST /orders/review ──→│── 更新OrderDetail评价字段   │
   │   {orderDetailId,rating,review}│                      │
   │←─ 200 ──────────────────│                            │
```

### 3.10 前后端接口对照表

#### 采购方接口

| 功能 | 前端方法 | HTTP | 后端接口 |
|------|---------|------|---------|
| 购物车列表 | `getCart()` | GET | `/purchaser/cart` |
| 添加购物车 | `addToCart(medicineId, quantity)` | POST | `/purchaser/cart?medicineId=&quantity=` |
| 修改数量 | `updateCartItem(id, quantity)` | PUT | `/purchaser/cart/{id}?quantity=` |
| 删除购物车 | `deleteCartItem(id)` | DELETE | `/purchaser/cart/{id}` |
| 清空购物车 | `clearCart()` | DELETE | `/purchaser/cart` |
| 创建订单 | `createOrder(data)` | POST | `/purchaser/orders` |
| 订单列表 | `getOrders(params)` | GET | `/purchaser/orders` |
| 订单详情 | `getOrderDetail(id)` | GET | `/purchaser/orders/{id}` |
| 支付 | `payOrder(id)` | PUT | `/purchaser/orders/{id}/pay` |
| 确认收货 | `receiveOrder(id)` | PUT | `/purchaser/orders/{id}/receive` |
| 取消订单 | `cancelOrder(id)` | PUT | `/purchaser/orders/{id}/cancel` |
| 评价 | `reviewOrder(data)` | POST | `/purchaser/orders/review` |
| 地址列表 | `getAddresses()` | GET | `/purchaser/addresses` |
| 提交反馈 | `submitFeedback(data)` | POST | `/purchaser/feedback` |

#### 商户方接口

| 功能 | 前端方法 | HTTP | 后端接口 |
|------|---------|------|---------|
| 订单列表 | `getOrders(params)` | GET | `/merchant/orders` |
| 订单详情 | `getOrderDetail(id)` | GET | `/merchant/orders/{id}` |
| 发货 | `shipOrder(id, trackingNo)` | PUT | `/merchant/orders/{id}/ship?trackingNo=` |
| 低库存 | `getLowStock()` | GET | `/merchant/inventory/low-stock` |

---

## 4. 质量追溯模块

### 4.1 追溯实体与表结构

| 实体类 | 数据库表 | 追溯角色 |
|--------|----------|----------|
| `MedicineInfo` | `medicine_info` | 药品主数据，含追溯码和追溯信息 |
| `MedicineCategory` | `medicine_category` | 分类归属 |
| `InventoryInfo` | `inventory_info` | 库存追踪与预警 |
| `PriceRecord` | `price_record` | 价格变动追踪 |
| `OrderInfo` | `order_info` | 订单流转记录 |
| `OrderDetail` | `order_detail` | 订单药品明细与评价 |
| `FeedbackInfo` | `feedback_info` | 用户反馈与投诉 |
| `MerchantInfo` | `merchant_info` | 商户资质与信用 |
| `PurchaserInfo` | `purchaser_info` | 采购方信息 |
| `RecommendRecord` | `recommend_record` | 推荐追踪记录 |
| `CollectionInfo` | `collection_info` | 用户收藏关联 |
| `OperationLog` | `operation_log` | 操作审计日志 |
| `MerchantNotification` | `merchant_notification` | 商户通知（预警/审核/价格异常） |
| `SystemConfig` | `system_config` | 追溯参数配置 |

### 4.2 药品追溯码与追溯信息

#### 追溯核心字段

`MedicineInfo` 中的追溯相关字段：

```java
private String traceCode;     // 质量追溯码 — 药品唯一追溯标识
private String traceInfo;     // 追溯信息JSON — 产地、质检等完整追溯链
private String origin;        // 产地 — 地理溯源
private String qualityGrade;  // 质量等级 — 品质追溯
private String specification; // 规格 — 产品规格追溯
```

数据库定义：

```sql
CREATE TABLE IF NOT EXISTS medicine_info (
    ...
    origin VARCHAR(100) COMMENT '产地',
    quality_grade VARCHAR(20) COMMENT '质量等级',
    specification VARCHAR(100) COMMENT '规格',
    trace_code VARCHAR(100) COMMENT '质量追溯码',
    trace_info TEXT COMMENT '追溯信息JSON',
    status TINYINT DEFAULT 0 COMMENT '0-待审核 1-已上架 2-已下架 3-已拒绝',
    ...
);
```

#### 追溯码自动生成

`MedicineServiceImpl.create()` 中，若未提供追溯码则自动生成：

```java
if (medicine.getTraceCode() == null || medicine.getTraceCode().trim().isEmpty()) {
    medicine.setTraceCode("TC" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
}
```

格式：`TC` + 16 位大写 UUID，例如 `TC3A5F7B9C1D2E4F6`。

#### 药品创建时的追溯初始化

创建药品时同步完成三件事：

1. 自动生成追溯码（如上）
2. 同步创建库存记录（`inventory_info`），含初始库存和预警阈值
3. 记录首次价格（`price_record`）

药品初始状态为 0（待审核），管理员审核通过后方可上架销售。

#### 药品审核状态流转

```java
public static final int MEDICINE_STATUS_PENDING = 0;   // 待审核
public static final int MEDICINE_STATUS_ON_SHELF = 1;   // 已上架
public static final int MEDICINE_STATUS_OFF_SHELF = 2;  // 已下架
public static final int MEDICINE_STATUS_REJECTED = 3;   // 已拒绝
```

审核通过/拒绝时，系统自动向商户发送通知（`type=4` 药材审核类型）。

### 4.3 价格变动追踪

#### 价格记录自动写入

`MedicineServiceImpl.update()` 中，检测到价格变更时自动写入 `price_record`：

```java
if (dto.getPrice() != null && oldPrice != null && dto.getPrice().compareTo(oldPrice) != 0) {
    boolean isAbnormal = isPriceAbnormal(oldPrice, dto.getPrice());
    PriceRecord priceRecord = new PriceRecord();
    priceRecord.setMedicineId(existing.getId());
    priceRecord.setMerchantId(existing.getMerchantId());
    priceRecord.setPrice(dto.getPrice());
    priceRecord.setIsAbnormal(isAbnormal ? 1 : 0);
    priceRecord.setRecordTime(LocalDateTime.now());
    priceRecordMapper.insert(priceRecord);
    if (isAbnormal) {
        notifyPriceAbnormal(existing.getMerchantId(), existing.getName(), oldPrice, dto.getPrice());
    }
}
```

#### 价格异常判定算法

```java
private boolean isPriceAbnormal(BigDecimal oldPrice, BigDecimal newPrice) {
    if (oldPrice == null || newPrice == null || oldPrice.compareTo(BigDecimal.ZERO) == 0) return false;
    double rate = getConfigDouble("price_abnormal_rate", 0.3);  // 默认30%
    BigDecimal change = newPrice.subtract(oldPrice).abs();
    BigDecimal threshold = oldPrice.multiply(BigDecimal.valueOf(rate)).setScale(4, RoundingMode.HALF_UP);
    return change.compareTo(threshold) > 0;
}
```

当价格变动幅度超过原价的 30%（可通过 `system_config` 表的 `price_abnormal_rate` 配置），标记为异常。

#### 价格异常通知

异常时自动向商户发送通知（`type=2` 价格异常类型）：

```java
private void notifyPriceAbnormal(Long merchantId, String medicineName, BigDecimal oldPrice, BigDecimal newPrice) {
    MerchantNotification n = new MerchantNotification();
    n.setMerchantId(merchantId);
    n.setType(2);
    n.setTitle("价格异常波动预警");
    n.setContent(String.format("药材「%s」价格波动较大：原价¥%s → 现价¥%s，请核实",
            medicineName, oldPrice, newPrice));
    notificationMapper.insert(n);
}
```

#### 价格记录表结构

```sql
CREATE TABLE IF NOT EXISTS price_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    medicine_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    is_abnormal TINYINT DEFAULT 0 COMMENT '0-正常 1-异常',
    record_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_medicine (medicine_id)
);
```

### 4.4 库存预警机制

#### 库存数据模型

```java
@TableName("inventory_info")
public class InventoryInfo {
    private Long id;
    private Long medicineId;
    private Long merchantId;
    private Integer stockQuantity;
    private Integer warningThreshold;
}
```

联合唯一键：`UNIQUE KEY uk_medicine_merchant (medicine_id, merchant_id)` — 同一药材在不同商户下有独立库存。

#### 定时库存预警任务

`InventoryWarningTask` 每小时执行一次（`@Scheduled(fixedRate = 3600000)`）：

1. 调用 `inventoryMapper.selectLowStockInventories()` 查询所有低库存记录
2. 按商户分组
3. 将预警数据写入 Redis 缓存（24 小时有效）
4. 防重复通知：6 小时内已有未读预警通知则跳过
5. 创建商户通知（`type=1` 库存预警类型）

预警判定 SQL：

```sql
SELECT * FROM inventory_info WHERE stock_quantity <= warning_threshold
```

#### 商户通知类型体系

`MerchantNotification` 的 `type` 字段定义了 5 种通知类型：

| type | 含义 | 触发场景 |
|------|------|---------|
| 1 | 库存预警 | `InventoryWarningTask` 定时检测 |
| 2 | 价格异常 | 价格变动超过阈值 |
| 3 | 新订单 | 采购商下单 |
| 4 | 药材审核 | 管理员审核通过/拒绝 |
| 5 | 系统公告 | 系统公告发布 |

#### 低库存查询接口

- 管理端：`GET /admin/inventory/low-stock` — 返回所有商户的低库存药品
- 商户端：`GET /merchant/inventory/low-stock` — 仅返回该商户的低库存药品

`LowStockVO` 聚合库存和药品信息：

```java
private Long medicineId;
private Long merchantId;
private String name;
private String image;
private Integer stockQuantity;
private Integer warningThreshold;
private BigDecimal price;
private String unit;
private String categoryName;
private String merchantName;
```

### 4.5 推荐系统与追溯关联

#### 协同过滤推荐算法

`MedicineServiceImpl.getRecommendList()` 实现了基于用户的协同过滤：

1. 尝试从 Redis 缓存读取
2. 获取当前用户历史采购记录
3. 无采购历史 → 返回热销榜
4. 有采购历史 → 计算用户相似度（余弦相似度）
5. 基于相似用户生成推荐
6. 推荐为空 → 按分类降级推荐
7. 仍为空 → 返回热销榜
8. 缓存结果并保存推荐记录

#### 余弦相似度计算

将采购记录视为二值向量（1=已购买，0=未购买），计算两个用户采购行为的余弦相似度：

```java
private double cosineSimilarity(List<Long> list1, List<Long> list2) {
    Set<Long> set1 = new HashSet<>(list1);
    Set<Long> set2 = new HashSet<>(list2);
    if (set1.isEmpty() || set2.isEmpty()) return 0;
    int dotProduct = 0;
    for (Long id : set1) {
        if (set2.contains(id)) dotProduct++;
    }
    double norm1 = Math.sqrt(set1.size());
    double norm2 = Math.sqrt(set2.size());
    return dotProduct / (norm1 * norm2);
}
```

#### 推荐记录追踪

`RecommendRecord` 记录推荐效果：

```java
private Long purchaserId;
private Long medicineId;
private LocalDateTime recommendTime;
private Integer isClicked;   // 是否点击
private Integer isOrdered;   // 是否下单
```

- `recordRecommendClick()`：用户点击推荐药品时标记 `isClicked`
- `recordRecommendOrdered()`：用户支付订单时标记 `isOrdered`（在 `payOrder` 中调用）

#### 推荐缓存定时刷新

`RecommendTask` 每小时刷新所有采购商的推荐缓存：

```java
@Scheduled(fixedRate = 3600000)
public void refreshRecommendations() {
    List<PurchaserInfo> purchasers = purchaserMapper.selectList(null);
    for (PurchaserInfo p : purchasers) {
        medicineService.getRecommendList(p.getId(), true);
    }
}
```

#### 推荐相关配置

`system_config` 表中的推荐参数：

| 配置键 | 默认值 | 说明 |
|--------|--------|------|
| `recommend_similar_count` | 10 | 协同过滤相似用户数量上限 |
| `recommend_item_count` | 10 | 推荐结果数量 |
| `recommend_cache_hours` | 1 | 推荐缓存有效期（小时） |

### 4.6 操作审计日志

`OperationLogAspect` 通过 AOP 切面自动记录所有非 GET 请求的操作日志：

```java
@Pointcut("execution(* com.medicine.sales.controller..*.*(..))")
public void controllerPointcut() {}

@Around("controllerPointcut()")
public Object around(ProceedingJoinPoint point) throws Throwable {
    String httpMethod = getHttpMethod(method);
    if (httpMethod != null && !"GET".equals(httpMethod)) {
        saveOperationLog(userId, role, className, methodName, httpMethod, point);
    }
    // 慢 API 检测 (>3s)
}
```

记录内容：操作人类型/ID/名称、模块、操作、请求参数 JSON、IP 地址。管理端可通过 `GET /admin/operation-logs` 查询，支持按模块和关键词筛选。

### 4.7 用户反馈闭环

`FeedbackInfo` 支持四种类型和三种状态：

- 类型：1-功能建议、2-问题反馈、3-投诉、4-其他
- 状态：0-待处理、1-已处理、2-已关闭

管理员可回复反馈（`PUT /admin/feedbacks/{id}/reply`）和关闭反馈，形成完整的质量反馈链路。

### 4.8 追溯数据链路关系图

```
merchant_info (商户资质)
    │
    ├──→ medicine_info (药品主数据 + 追溯码 + 追溯信息)
    │         │              │
    │         │              └──→ medicine_category (分类归属)
    │         │
    │         ├──→ inventory_info (库存 + 预警阈值)
    │         │         │
    │         │         ├──→ InventoryWarningTask (定时预警)
    │         │         │         ├──→ merchant_notification (type=1 库存预警)
    │         │         │         └──→ Redis (缓存预警数据)
    │         │
    │         └──→ price_record (价格变动记录)
    │                   │
    │                   └──→ isPriceAbnormal() (异常判定)
    │                             └──→ merchant_notification (type=2 价格异常)
    │
    ├──→ order_info (订单)
    │         │
    │         └──→ order_detail (药品明细 + 评价)
    │                   └──→ rating / review (质量反馈)
    │
    └──→ merchant_notification (通知中心)

purchaser_info (采购商)
    │
    ├──→ collection_info (收藏)
    ├──→ cart_info (购物车)
    ├──→ order_info (订单)
    ├──→ feedback_info (反馈/投诉)
    └──→ recommend_record (推荐追踪)
              └──→ isClicked / isOrdered (推荐效果追踪)

operation_log (全链路操作审计)
system_config (追溯参数配置)
```

### 4.9 管理端追溯查询接口

| 接口 | 方法 | 追溯用途 |
|------|------|---------|
| `GET /admin/dashboard` | dashboard | 总览：订单/药品/用户/低库存统计 |
| `GET /admin/medicines` | medicinePage | 药品列表（含库存、销量、状态） |
| `GET /admin/medicines/{id}` | medicineDetail | 药品详情（含追溯码、追溯信息、库存） |
| `PUT /admin/medicines/{id}/status` | updateMedicineStatus | 审核药品 |
| `GET /admin/inventory/low-stock` | lowStockList | 低库存预警列表 |
| `GET /admin/price-history/{medicineId}` | priceHistory | 药品价格历史 |
| `GET /admin/orders` | orderPage | 订单列表 |
| `GET /admin/orders/{id}` | orderDetail | 订单详情 |
| `GET /admin/feedbacks` | feedbackPage | 用户反馈列表 |
| `PUT /admin/feedbacks/{id}/reply` | replyFeedback | 回复反馈 |
| `GET /admin/operation-logs` | operationLogPage | 操作日志查询 |
| `GET /admin/configs` | configList | 系统配置（含追溯参数） |
| `PUT /admin/configs` | updateConfig | 修改配置 |

`DashboardVO` 提供全局追溯概览：

```java
private Long totalOrders;
private BigDecimal totalRevenue;
private Long totalMedicines;
private Long totalUsers;
private Long pendingMerchants;
private Long pendingMedicines;
private Long lowStockCount;
private List<HotMedicine> hotMedicines;
private List<OrderTrend> orderTrends;
```

### 4.10 前端追溯展示页面

#### 管理端

| 页面 | 文件 | 展示内容 |
|------|------|---------|
| 药材管理 | `MedicineManage.vue` | 药品列表（名称、分类、产地、价格、库存、销量、状态）、新增/编辑/审核 |
| 价格分析 | `PriceAnalysis.vue` | 预警阈值设置、统计卡片、价格走势柱状图、变更记录表 |
| 订单管理 | `OrderManage.vue` | 订单列表、订单详情弹窗 |
| 反馈管理 | `FeedbackManage.vue` | 反馈列表、回复/关闭反馈 |
| 库存预警 | `InventoryWarning.vue` | 低库存药品列表 |
| 操作日志 | `OperationLogManage.vue` | 操作日志查询 |

#### 用户端

| 页面 | 文件 | 展示内容 |
|------|------|---------|
| 药品详情 | `MedicineDetail.vue` | 三个 Tab：药材详情 / 溯源信息（`traceInfo`）/ 用户评价 |
| 药品列表 | `MedicineList.vue` | 药品卡片网格（图片、名称、规格/产地、价格、销量） |

`MedicineDetail.vue` 的溯源信息 Tab 直接展示 `medicine.traceInfo` 字段内容（JSON 格式的追溯信息），页面顶部还展示产地、品质等级、规格、库存、销量、商家等追溯相关属性。
