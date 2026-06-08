# 药材销售管理系统

## 1. How to Run

### Docker 一键启动（推荐）

```bash
docker-compose up --build -d
```

启动后访问：
- 管理后台：http://localhost:8081
- 用户端：http://localhost:8082
- 后端 API：http://localhost:8080/api

停止并删除数据卷：
```bash
docker-compose down -v
```

### 本地开发

**后端**
```bash
cd backend
mvn spring-boot:run
```

**管理后台**
```bash
cd frontend-admin
npm install --registry=https://registry.npmmirror.com
npm run dev
```

**用户端**
```bash
cd frontend-user
npm install --registry=https://registry.npmmirror.com
npm run dev
```

前提条件：本地已安装 MySQL 8.0 和 Redis，并导入 `backend/src/main/resources/schema.sql`。

**已有数据库升级**：若数据库已存在，需执行 `backend/src/main/resources/db/migration-all-new-features.sql` 以添加用户反馈、操作日志等新表。

**测试**：功能测试 `mvn test -Dtest=FunctionalTest`；性能测试见 [docs/TEST_REPORT.md](docs/TEST_REPORT.md)。

## 2. Services

| 服务 | 端口 | 说明 |
|------|------|------|
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| Backend (Spring Boot) | 8080 | 后端 API 服务 |
| Frontend Admin (Vue 3) | 8081 | 管理后台 |
| Frontend User (Vue 3) | 8082 | 用户端（采购商/商户） |

## 3. 测试账号

**管理后台 http://localhost:8081**

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 管理员 | admin | admin123 | 系统启动时自动创建 |

**用户端 http://localhost:8082**（采购商/商户）

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 商户 | tongrentang | merchant123 | 北京同仁堂药业 |
| 商户 | huqingyutang | merchant123 | 杭州胡庆余堂 |
| 商户 | yunnanbaiyao | merchant123 | 云南白药集团 |
| 采购商 | buyer001 | purchaser123 | 仁心药房 |
| 采购商 | buyer002 | purchaser123 | 济世堂连锁 |
| 采购商 | buyer003 | purchaser123 | 康泰药业 |

**说明**：管理员由系统启动时自动创建；商户和采购商为 schema 默认数据，首次启动时 DataInitializer 会重置其密码为上述值，确保可正常登录。采购商和商户也可通过用户端注册页面自行注册，商户注册后需管理员审核通过方可登录。**用户名规则**：仅允许字母、数字和下划线，不能包含中文或特殊字符。

## 4. 题目内容

# 基于Spring Boot的药材销售管理系统设计与实现方案 
## 一、技术综述 
本系统沿用“好农物商城”成熟的前后端分离技术架构，结合药材销售行业特性优化技术选型，确保系统稳定、高效、可扩展。 

### 核心技术栈 
- **Spring Boot**：作为后端核心框架，简化配置流程与依赖管理，内置Tomcat服务器实现快速部署，通过自动配置功能减少重复编码，适配药材销售全流程的业务逻辑开发。 
- **Vue.js + Element UI**：前端采用Vue.js框架实现MVVM模式开发，利用数据双向绑定简化界面与数据的联动操作；搭配Element UI组件库快速构建简洁易用的交互界面，适配管理员、商户、采购商三类用户的操作场景。 
- **MyBatis-Plus**：基于MyBatis升级的持久层框架，提供通用Mapper和Service封装，简化药材信息、订单、库存等核心数据的CRUD操作，支持复杂条件查询，提升数据库操作效率。 
- **MySQL + Redis**：MySQL作为主数据库，存储药材基础信息、交易记录、用户数据等结构化数据，保障数据一致性；Redis缓存热点药材信息、用户登录状态及推荐算法结果，降低数据库访问压力，提升系统响应速度。 
- **协同过滤算法**：参考推荐系统技术方案，融合采购商历史采购记录与相似用户行为偏好，实现个性化药材推荐功能，提升交易匹配效率。 

## 二、系统功能性分析 
系统面向管理员、商户、采购商三类用户，分为前台交易场景与后台管理场景，覆盖药材销售全流程需求。 

### 前台功能（面向商户与采购商） 
1. **药材信息查询与展示**：采购商可通过关键词搜索、分类筛选（如药材品类、产地、质量等级）查询药材信息，系统通过轮播图展示热门药材、新品药材，支持查看药材详情（含基础信息、质量追溯记录、商户资质、用户评价）。 
2. **商户入驻与管理**：商户可提交入驻申请（上传经营资质、营业执照），审核通过后管理店铺信息、维护药材库存、处理订单、回复采购商咨询。 
3. **交易全流程操作**：采购商可将药材加入购物车、生成订单、在线支付，实时查看订单状态（待支付、待发货、待收货、已完成）；商户接收订单通知，处理发货流程并更新订单状态。 
4. **个性化采购推荐**：基于协同过滤算法，系统为采购商推送匹配其采购偏好的药材，同时展示相似采购商的热门采购品类。 
5. **辅助功能**：采购商可收藏常用药材、管理收货地址、评价交易药材；商户可查看店铺交易数据统计（销量、营收）、接收库存预警通知。 

### 后台功能（面向管理员） 
1. **用户管理**：审核商户入驻申请、管理采购商与商户账号（启用/禁用、权限配置），维护用户信息。 
2. **药材信息监管**：审核商户发布的药材信息，确保质量描述真实，管理药材分类体系，维护质量追溯标准。 
3. **交易与库存监管**：查看全平台交易数据统计（订单量、交易额、热门药材），监控商户库存状态，处理交易纠纷。 
4. **价格与数据管理**：查看药材价格走势数据，设置价格异常波动预警阈值；管理系统公告、消息推送，接收用户反馈。 
5. **系统配置**：维护系统基础参数（如交易手续费、库存预警阈值），管理前端展示的轮播图、热门推荐列表。 

## 三、系统模块设计 
参考“好农物商城”的模块化设计思路，结合药材销售业务特性，划分以下核心模块： 

### 1. 前台模块 
- **用户中心模块**：包含采购商/商户注册登录、个人信息维护、收货地址管理、收藏夹管理、账号安全设置。 
- **药材展示与查询模块**：药材分类展示、关键词搜索、详情页展示、质量追溯信息查询、用户评价展示。 
- **交易管理模块**：购物车操作、订单创建、支付对接、订单状态查询、交易评价提交。 
- **商户运营模块**：店铺信息管理、药材上下架、库存维护、订单处理、经营数据统计。 
- **智能推荐模块**：基于协同过滤算法的个性化药材推荐、热门药材榜单、相似采购商推荐。 

### 2. 后台模块 
- **系统管理模块**：管理员账号管理、角色权限配置、系统参数设置、日志管理。 
- **用户监管模块**：商户入驻审核、用户账号管理、经营资质核验、动态评分管理。 
- **药材监管模块**：药材信息审核、分类管理、质量追溯数据维护、违规药材下架。 
- **交易与库存模块**：全平台订单监控、交易数据统计、库存预警管理、物流信息对接。 
- **价格分析模块**：价格走势可视化、异常波动预警、价格数据导出。 

## 四、系统数据库设计 
参考“好农物商城”的实体设计逻辑，结合药材销售业务，设计核心实体及关联关系，确保数据完整性与关联性。 

### 核心实体（关键表设计） 
1. **药材信息表（medicine_info）**：存储药材基础数据，字段包括药材ID、名称、品类ID、产地、质量等级、规格、单价、商户ID、质量追溯码、上架状态、创建时间。 
2. **商户信息表（merchant_info）**：字段包括商户ID、账号、密码、企业名称、营业执照号、经营资质图片、联系人、联系方式、入驻状态、动态评分、创建时间。 
3. **采购商信息表（purchaser_info）**：字段包括采购商ID、账号、密码、姓名、联系方式、收货地址ID、注册时间、登录状态。 
4. **订单表（order_info）**：字段包括订单ID、采购商ID、商户ID、订单金额、支付状态、发货状态、收货状态、支付时间、发货时间、收货时间、物流单号、创建时间。 
5. **订单明细表（order_detail）**：字段包括明细ID、订单ID、药材ID、采购数量、单价、小计金额、质量评价、评价时间。 
6. **库存表（inventory_info）**：字段包括库存ID、药材ID、商户ID、库存数量、预警阈值、入库时间、更新时间。 
7. **价格记录表（price_record）**：字段包括记录ID、药材ID、商户ID、价格、记录时间、是否异常。 
8. **推荐记录表（recommend_record）**：字段包括记录ID、采购商ID、推荐药材ID、推荐时间、点击状态、下单状态。 

### 实体关联关系 
- 商户与药材：一对多（一个商户可上架多个药材）。 
- 采购商与订单：一对多（一个采购商可创建多个订单）。 
- 订单与订单明细：一对多（一个订单包含多个药材明细）。 
- 药材与库存：一对一（一个药材对应一条库存记录，关联商户）。 
- 药材与价格记录：一对多（一个药材对应多条价格变动记录）。 

## 五、核心模块实现方案 
### 1. 架构设计 
采用前后端分离架构，具体交互流程： 
- 前端通过Vue.js发起HTTP请求，调用后端RESTful API接口。 
- 后端Spring Boot接收请求，通过MyBatis-Plus操作数据库，处理业务逻辑（如订单创建、库存更新）。 
- 热点数据（如热门药材、用户登录态）通过Redis缓存，减少数据库查询次数。 
- 推荐模块通过定时任务计算用户偏好，将推荐结果存入Redis，前端直接调用缓存数据。 

### 2. 关键模块实现 
- **质量追溯模块**：通过“质量追溯码”关联药材种植、加工、流通全流程数据，商户上传各环节信息后，采购商可通过追溯码查询完整链路，实现数据透明化。 
- **采购推荐模块**：基于协同过滤算法，首先计算用户相似度（通过历史采购记录、浏览行为），再根据相似用户的采购偏好，结合药材销量、评分，生成个性化推荐列表，算法核心代码如下（简化版）： 
```java 
// 计算用户相似度 
public Map<Long, Double> calculateUserSimilarity(Long purchaserId) { 
// 1. 获取当前用户历史采购记录 
List<Long> purchaseMedicineIds = orderMapper.getPurchaseMedicineIds(purchaserId); 
// 2. 匹配有相似采购记录的用户 
List<Long> similarPurchasers = orderMapper.getSimilarPurchasers(purchaseMedicineIds); 
// 3. 计算余弦相似度，返回用户ID与相似度映射 
Map<Long, Double> similarityMap = new HashMap<>(); 
for (Long similarPurchaser : similarPurchasers) { 
List<Long> similarPurchaseIds = orderMapper.getPurchaseMedicineIds(similarPurchaser); 
double similarity = cosineSimilarity(purchaseMedicineIds, similarPurchaseIds); 
similarityMap.put(similarPurchaser, similarity); 
} 
return similarityMap; 
} 
// 生成推荐列表 
public List<MedicineVO> generateRecommendList(Long purchaserId) { 
Map<Long, Double> userSimilarity = calculateUserSimilarity(purchaserId); 
// 筛选高相似度用户，获取其采购的药材 
List<Long> recommendMedicineIds = orderMapper.getHighSimilarityPurchaseMedicines(userSimilarity); 
// 过滤当前用户已采购的药材，按销量排序 
return medicineMapper.getRecommendMedicines(recommendMedicineIds, purchaserId); 
} 
```
> **实现说明**：`MedicineServiceImpl` 中 `calculateUserSimilarity`、`generateRecommendListFromSimilarity` 与上述逻辑一致。`OrderMapper.getPurchaseMedicineIds`、`getSimilarPurchasers`、`getHighSimilarityPurchaseMedicines` 及 `MedicineMapper.getRecommendMedicines` 均已实现。`getSimilarPurchasers` 需传入 `purchaserId` 以排除当前用户。 
- **库存预警模块**：商户设置库存预警阈值，系统定时查询库存表，当库存数量低于阈值时，通过消息推送通知商户，同时在后台管理界面标记预警状态。 

### 3. 系统测试 
参考“好农物商城”的测试思路，进行功能测试、性能测试、兼容性测试： 
- 功能测试：验证各模块核心功能（如商户入驻审核、订单创建、推荐算法准确性）是否正常。 
- 性能测试：通过模拟1000+并发用户访问，测试系统响应时间（要求≤3秒）、数据库吞吐量，确保Redis缓存有效降低数据库压力。 
- 兼容性测试：适配主流浏览器（Chrome、Firefox、Edge）及移动设备端，确保界面展示与操作流畅。 

## 六、系统特色与预期效果 
### 1. 系统特色 
- 贴合药材行业需求：突出质量追溯、资质审核、价格分析等核心功能，解决传统交易信息不透明问题。 
- 技术架构成熟稳定：复用Spring Boot+Vue的轻量级架构，模块化设计便于后续功能扩展（如新增跨境交易、物流对接）。 
- 智能推荐提升效率：协同过滤算法实现精准匹配，降低采购商筛选成本，提升商户销量。 

### 2. 预期效果 
- 交易效率提升：减少中间环节，实现药材在线交易，缩短采购周期。 
- 质量监管强化：全流程追溯药材来源，保障用药安全，规范商户经营。 
- 信息透明化：采购商可直观查看药材信息、价格走势、商户资质，降低交易风险。

## 5. 项目介绍

### Docker 多架构支持

所有基础镜像（MySQL、Redis、eclipse-temurin、node、nginx）均为官方镜像，支持 linux/amd64、linux/arm64。**苹果电脑（M1/M2/M3 芯片）可正常构建和运行**。

### 安全说明
- **密码存储**：用户密码使用 BCrypt 加密存储，不存明文
- **Redis 缓存**：所有缓存均设置过期时间，避免内存占用过高

### 项目结构

| 目录 | 说明 |
|------|------|
| backend | 后端（Spring Boot） |
| frontend-admin | 管理后台（Vue 3） |
| frontend-user | 用户端（Vue 3） |

### 技术架构
- **后端**：Spring Boot 2.7 + MyBatis-Plus 3.5 + MySQL 8.0 + Redis
- **前端**：Vue 3 + Element Plus + Vite + Pinia
- **部署**：Docker + Docker Compose

### 核心功能
- **三种角色**：管理员（后台管理）、商户（药材上架/订单处理）、采购商（浏览/购买）
- **药材管理**：分类管理、上下架审核、质量追溯、价格记录
- **交易流程**：购物车 → 下单 → 支付 → 发货 → 收货 → 评价
- **智能推荐**：基于协同过滤算法的个性化药材推荐
- **库存预警**：定时检测库存，低于阈值自动告警
- **价格分析**：价格走势记录与异常波动检测
- **系统配置**：轮播图、公告、系统参数可动态配置

### 设计文档

系统架构、ER 图、接口清单、UI/UX 规范见 [docs/project_design.md](docs/project_design.md)。

### 限制说明
- **支付模块**：当前为 **Mock 模拟支付**，点击「立即支付」后直接更新订单状态，不涉及真实支付渠道（支付宝/微信等）。
- **物流信息**：仅支持商户手动填写快递单号，未对接物流 API，无法查询物流轨迹。
