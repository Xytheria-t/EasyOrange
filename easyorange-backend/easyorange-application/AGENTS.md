# easyorange-application 模块指南

应用启动入口，聚合所有业务模块，包含配置、Flyway 迁移、架构测试。

## 目录结构

```
application/
├── src/main/java/
│   └── com/cartethyia/easyorange/
│       ├── EasyOrangeApplication.java     # Spring Boot 主类
│       ├── adapter/
│       │   ├── event/                     # 跨模块事件监听器
│       │   │   ├── PaymentEventListener.java
│       │   │   └── ProductEventListener.java
│       │   └── outbound/                  # 跨模块适配器实现
│       │       ├── payment/
│       │       │   └── OrderPaymentGatewayAdapter.java
│       │       ├── product/
│       │       │   ├── FavoriteProductInfoAdapter.java
│       │       │   ├── OrderProductInventoryAdapter.java
│       │       │   └── OrderProductQueryAdapter.java
│       │       └── user/
│       │           ├── MessageUserInfoAdapter.java
│       │           ├── OrderUserInfoAdapter.java
│       │           └── SellerInfoAdapter.java
│       └── controller/
│           ├── HealthController.java      # 健康检查
│           └── PlatformStatsController.java # 平台统计
├── src/main/resources/
│   ├── application.yaml                   # 基础配置
│   ├── application-dev.yaml               # 开发环境
│   ├── application-prod.yaml              # 生产环境
│   ├── application-test.yaml              # 测试环境
│   ├── logback-spring.xml                 # 日志配置
│   ├── openapi.yaml                       # OpenAPI 文档
│   └── db/
│       ├── migration/                     # Flyway DDL 迁移
│       │   ├── V1__init_schema.sql
│       │   ├── V2__seed_categories.sql
│       │   └── V3__payment_infrastructure.sql
│       └── dev/                           # 开发环境数据
│           └── test_data.sql
└── src/test/java/
    └── com/cartethyia/easyorange/
        ├── architecture/
        │   └── ArchitectureRulesTest.java # ArchUnit 架构守卫
        └── controller/
            └── HealthControllerTest.java
```

## 模块依赖

```
easyorange-application
├── easyorange-framework
├── easyorange-user
├── easyorange-product
├── easyorange-order
├── easyorange-payment
├── easyorange-message
├── easyorange-favorite
├── spring-boot-starter-actuator
├── micrometer-registry-prometheus
└── flyway-core + flyway-mysql
```

## 配置管理

### 多环境配置

| 文件 | 用途 | 关键差异 |
|------|------|---------|
| `application.yaml` | 基础配置 | 数据源、Redis、MyBatis-Plus、线程池 |
| `application-dev.yaml` | 开发环境 | 小连接池、详细日志、JWT 开发密钥 |
| `application-prod.yaml` | 生产环境 | 大连接池、SSL、Swagger 关闭、优雅停机 |
| `application-test.yaml` | 测试环境 | Testcontainers 配置 |

### 关键配置项

- `easyorange.jwt.secret` / `easyorange.jwt.access-token-expiration` — JWT 配置
- `easyorange.security` — 安全相关 (白名单路径等)
- `easyorange.rate-limiter` — 限流配置
- `easyorange.thread-pool` — 线程池配置

## Flyway 迁移规范

- 版本号格式: `V{N}__description.sql` (N 为递增整数)
- DDL 放 `db/migration/`，开发数据放 `db/dev/`
- **禁止修改已执行的迁移脚本**
- 新增字段必须可空或有默认值
- 迁移脚本中不写业务逻辑

## 架构守卫测试

`ArchitectureRulesTest.java` 使用 ArchUnit 验证 DDD 分层规则：

- domain 层不依赖 adapter 层
- domain 层不依赖 Spring 框架
- 包依赖方向合规
- 端口接口必须有适配器实现
- 业务模块不直接导入其他模块的领域类
- 已知违规项在白名单中标注，附带演进计划

## 跨模块适配器

`adapter/outbound/` 目录存放跨模块端口接口的适配器实现：

| 适配器 | 端口接口 | 模块 | 功能 |
|--------|---------|------|------|
| `OrderPaymentGatewayAdapter` | `PaymentGatewayPort` | order | 支付网关调用 |
| `OrderProductInventoryAdapter` | `ProductInventoryPort` | order | 商品库存操作 |
| `OrderProductQueryAdapter` | `ProductQueryPort` | order | 商品查询 |
| `OrderUserInfoAdapter` | `UserInfoPort` | order | 用户信息查询 |
| `SellerInfoAdapter` | `SellerInfoPort` | product | 卖家信息查询 |
| `MessageUserInfoAdapter` | `UserInfoPort` | message | 用户信息查询 |
| `FavoriteProductInfoAdapter` | `ProductInfoPort` | favorite | 商品信息查询 |

`adapter/event/` 目录存放跨模块事件监听器：

| 监听器 | 事件 | 功能 |
|--------|------|------|
| `PaymentInitiationEventListener` | `PaymentInitiationRequestedEvent` | 创建支付记录 |
| `StockReservationEventListener` | `StockReservationRequestedEvent` | 扣减库存 |

所有事件监听器使用 `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` + `@Async("domainEventExecutor")` 模式，确保事务提交后异步处理。

## 常见开发任务

### 添加新环境配置

1. 创建 `application-{profile}.yaml`
2. 在 `application.yaml` 中设置 `spring.profiles.active`
3. 测试配置加载

### 添加 Flyway 迁移

1. 确认当前最大版本号
2. 创建 `V{N+1}__description.sql`
3. 本地运行验证
4. 提交前确认迁移可重复执行

### 添加架构守卫规则

1. 在 `ArchitectureRulesTest.java` 添加 `@ArchTest` 规则
2. 运行测试确认现有代码合规
3. 不合规的添加白名单并标注演进计划
