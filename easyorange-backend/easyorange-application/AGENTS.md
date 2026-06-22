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
│       │   │   ├── PaymentInitiationEventListener.java
│       │   │   ├── ProductAuditEventListener.java
│       │   │   ├── ReportProcessedEventListener.java
│       │   │   └── StockReservationEventListener.java
│       │   ├── inbound/web/controller/  # Web 控制器
│       │   │   ├── AiController.java                  # AI 服务端点
│       │   │   ├── CreditScoreController.java         # 信用分数端点
│       │   │   ├── AdminSearchReindexController.java  # ES 重索引管理
│       │   │   ├── HealthController.java              # 健康检查
│       │   │   └── PlatformStatsController.java       # 平台统计
│       │   └── outbound/                  # 跨模块适配器实现
│       │       ├── elasticsearch/         # ES 搜索索引适配器
│       │       │   ├── ElasticsearchIndexManager.java
│       │       │   ├── ElasticsearchProductSearchIndexAdapter.java
│       │       │   ├── ElasticsearchProductSearchQueryAdapter.java
│       │       │   ├── ProductDocument.java
│       │       │   └── ReindexService.java
│       │       ├── payment/
│       │       │   └── OrderPaymentGatewayAdapter.java
│       │       ├── product/
│       │       │   ├── FavoriteProductInfoAdapter.java
│       │       │   ├── OrderProductInventoryAdapter.java
│       │       │   ├── OrderProductQueryAdapter.java
│       │       │   └── ProductSearchIndexAdapter.java
│       │       └── user/
│       │           ├── MessageUserInfoAdapter.java
│       │           ├── OrderUserInfoAdapter.java
│       │           └── SellerInfoAdapter.java
├── src/main/resources/
│   ├── application.yaml                   # 基础配置
│   ├── application-dev.yaml               # 开发环境
│   ├── application-prod.yaml              # 生产环境
│   ├── application-test.yaml              # 测试环境
│   ├── logback-spring.xml                 # 日志配置
│       └── db/
│           ├── migration/                     # Flyway 迁移脚本 (V=版本, R=可重复)
│           │   ├── V1__init_schema.sql
│           │   ├── V2__create_eo_order_item.sql
│           │   ├── V3__optimize_indexes_and_migrate_orders.sql
│           │   ├── R__seed_categories.sql
│           │   ├── R__seed_message_templates.sql
│           │   └── R__seed_payment_config.sql
│           └── dev/                           # 开发环境数据
│               └── R__insert_dev_test_data.sql
└── src/test/java/
    └── com/cartethyia/easyorange/
        ├── architecture/
        │   └── ArchitectureRulesTest.java # ArchUnit 架构守卫
        └── adapter/inbound/web/controller/
            ├── HealthControllerTest.java
            └── AiControllerTest.java
```

## 模块依赖

```
easyorange-application
├── easyorange-framework
├── easyorange-admin                    # 管理端 API（需管理员权限）
├── easyorange-user
├── easyorange-product
├── easyorange-favorite
├── easyorange-order
├── easyorange-payment
├── easyorange-message
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
- `rate-limit-filter` — 限流+防重 Filter 配置（规则列表、防重间隔、方法匹配）
- `easyorange.thread-pool` — 线程池配置
- `file.upload.*` — 文件上传路径 (`path`) 和 URL 前缀 (`url-prefix`)
- `easyorange.idgen.*` — Snowflake 分布式 ID 生成器（`enabled`、`data-center-id`）
- `easyorange.cache.*` — 本地缓存配置（`image.max-size`、`image.expire-hours`、`l1.max-size`、`l1.expire-minutes`）
- `http-client.*` — HTTP 客户端超时和协议版本

### 日志配置 (logback-spring.xml)

- `LOG_PATH` — 日志目录，默认 `./logs`，生产环境建议设置为绝对路径
- 输出：CONSOLE + FILE (app.log) + ERROR_FILE (error.log)
- 滚动策略：按天 + 大小（100MB/文件，30 天保留，总量 3GB）
- MDC：`traceId` 贯穿全链路，在 `LoggingInterceptor` 中注入

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
| `ProductNotificationAdapter` | `ProductNotificationPort` | product | 商品事件通知（发布、售出、库存预警） |
| `ProductSearchIndexAdapter` | `ProductSearchIndexPort` | product | MySQL search_text 索引写入 |
| `ElasticsearchProductSearchIndexAdapter` | `ProductSearchIndexPort` | product | ES 搜索索引写入（条件激活） |
| `ElasticsearchProductSearchQueryAdapter` | — | — | ES 商品搜索查询（含分面聚合） |

`adapter/outbound/elasticsearch/` 搜索基础设施组件：

| 组件 | 职责 |
|------|------|
| `ElasticsearchIndexManager` | ES 索引创建/映射管理 |
| `ProductDocument` | ES 索引映射 POJO |
| `ReindexService` | MySQL → ES 全量重建索引 |
| `ElasticsearchProductSearchIndexAdapter` | 索引写入适配器（`@ConditionalOnProperty` 激活） |
| `ElasticsearchProductSearchQueryAdapter` | 索引查询适配器（分类/价格/成色分面聚合） |

`adapter/event/` 目录存放跨模块事件监听器：

| 监听器 | 事件 | 功能 |
|--------|------|------|
| `OrderNotificationEventConsumer` | `OrderCreatedEvent` 等 6 个订单事件 | 订单状态变更→站内消息通知 |
| `PaymentInitiationEventListener` | `PaymentInitiationRequestedEvent` | 创建支付记录 |
| `ProductAuditEventListener` | `ProductAuditedEvent` | 审核结果→站内消息通知 |
| `ReportProcessedEventListener` | `ReportProcessedEvent` | 举报处理结果→站内消息通知 |
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
