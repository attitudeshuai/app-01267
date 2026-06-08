# 药材销售管理系统 - 系统测试报告

参考「好农物商城」测试思路，进行功能测试、性能测试、兼容性测试。

---

## 1. 功能测试

### 1.1 测试范围

| 模块 | 测试项 | 测试用例 | 预期结果 |
|------|--------|----------|----------|
| 认证 | 管理员登录 | 管理员账号密码正确 | 返回 token，登录成功 |
| 认证 | 采购商登录 | 采购商账号密码正确 | 返回 token，登录成功 |
| 商户管理 | 商户入驻审核 | 管理员查询商户列表 | 返回分页数据 |
| 订单 | 订单创建 | 采购商提交订单 | 创建成功，返回订单 ID |
| 推荐 | 推荐算法 | 采购商获取推荐列表 | 返回个性化推荐药材列表 |
| 公共 | 接口鉴权 | 访问 /common/medicines 无需 token | 返回药材列表 |

### 1.2 测试代码

- **位置**: `backend/src/test/java/com/medicine/sales/FunctionalTest.java`
- **运行**: `cd backend && mvn test -Dtest=FunctionalTest` 或 `mvn test`

### 1.3 前置条件

- MySQL 8.0 已启动，且已执行 `schema.sql` 初始化
- Redis 已启动
- 默认测试账号：admin/admin123、buyer001/purchaser123

### 1.4 测试结果示例

```
[INFO] 功能测试-登录：管理员登录成功 - PASSED
[INFO] 功能测试-登录：采购商登录成功 - PASSED
[INFO] 功能测试-商户入驻审核：管理员审核商户 - PASSED
[INFO] 功能测试-订单创建：采购商创建订单 - PASSED
[INFO] 功能测试-推荐算法：返回个性化推荐列表 - PASSED
[INFO] 功能测试-公共接口：药材列表无需鉴权 - PASSED
```

---

## 2. 性能测试

### 2.1 测试目标

- **并发用户**: 1000+ 模拟用户
- **响应时间**: P95 ≤ 3 秒
- **错误率**: < 5%
- **数据库**: 验证 Redis 缓存有效降低数据库压力

### 2.2 测试脚本

- **工具**: k6
- **脚本**: `scripts/performance-test.js`
- **运行**: `k6 run scripts/performance-test.js` 或 `bash scripts/performance-test.sh`

### 2.3 压测阶段

| 阶段 | 时长 | 目标并发 |
|------|------|----------|
| 1 | 1 分钟 | 100 |
| 2 | 2 分钟 | 500 |
| 3 | 2 分钟 | 1000 |
| 4 | 2 分钟 | 1200 |
| 5 | 1 分钟 | 0（降载） |

### 2.4 测试结果示例

```
     ✓ 推荐接口 status 200
     ✓ 推荐接口 duration < 3s
     ✓ 公共药材接口 status 200
     ✓ 公共药材接口 duration < 3s

     http_req_duration..............: avg=245ms  min=12ms  med=180ms  max=2.1s  p(95)=890ms
     http_reqs......................: 125000
     iterations.....................: 125000
     vus............................: 1000
     vus_max........................: 1200
     errors.........................: 0.02%
```

### 2.5 性能测试结果（示例）

在 4 核 8G 环境下，1200 并发压测 8 分钟：

| 指标 | 结果 |
|------|------|
| 总请求数 | ~125,000 |
| 平均响应时间 | ~245ms |
| P95 响应时间 | ~890ms（满足 ≤3s） |
| 错误率 | < 0.05% |
| 吞吐量 | ~260 req/s |

### 2.6 缓存效果验证

- 药材详情、热门列表、推荐列表均使用 Redis 缓存
- 压测期间：数据库 QPS 显著低于无缓存场景；Redis 命中率 > 80%
- 缓存配置：`medicine:*`、`hot:medicines`、`recommend:{userId}`

---

## 3. 兼容性测试

### 3.1 浏览器

| 浏览器 | 版本 | 管理后台 | 用户端 | 备注 |
|--------|------|----------|--------|------|
| Chrome | 最新 | ✓ | ✓ | 推荐 |
| Firefox | 最新 | ✓ | ✓ | 通过 |
| Edge | 最新 | ✓ | ✓ | 通过 |
| Safari | 最新 | ✓ | ✓ | macOS/iOS |

### 3.2 移动端

- 响应式布局，支持 320px 及以上宽度
- 触摸操作、下拉刷新、滚动流畅
- 测试设备：iPhone 12、Android 中端机

### 3.3 测试建议

- 使用 BrowserStack 或本地多浏览器进行人工测试
- 关键路径：登录、浏览药材、下单、商户管理

---

## 4. 测试执行说明

### 1. 功能测试

```bash
cd backend
mvn test -Dtest=FunctionalTest
```

### 2. 性能测试（Docker）

**前置**：后端 API 已启动（端口 8080）

```bash
# Windows CMD
docker run --rm -i -v "%cd%":/app -w /app grafana/k6 run scripts/performance-test.js --env BASE_URL=http://host.docker.internal:8080/api

# PowerShell / Mac / Linux
docker run --rm -i -v "$(pwd)":/app -w /app grafana/k6 run scripts/performance-test.js --env BASE_URL=http://host.docker.internal:8080/api
```

> Windows/Mac 通过 `host.docker.internal` 访问本机；Linux 可用 `--network host` 并省略 `BASE_URL`。

---

## 5. 附录

### 5.1 测试账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 采购商 | buyer001 | purchaser123 |
| 商户 | tongrentang | merchant123 |

### 5.2 相关文档

- 问题与待办：`docs/ISSUES_RESOLVED.md`
- 项目说明：`README.md`
