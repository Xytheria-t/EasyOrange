# easyorange-application 模块指南

应用启动入口，聚合所有业务模块，包含配置、Flyway 迁移、架构测试。

## 目录结构

```
application/
├── src/main/java/
│   └── com/cartethyia/easyorange/
│       ├── EasyOrangeApplication.java     # Spring Boot 主类
│       ├── adapter/
│       │   ├── event/                     # 跨模块事件监听器（5 个；其余消费者在各业务模块内）
│       │   │   ├── OrderNotificationEventConsumer.java
│       │   │   ├── ProductAuditEventConsumer.java
│       │   │   ├── ReportProcessedEventConsumer.java
│       │   │   ├── AiProductEventConsumer.java
│       │   │   └── AiCreditEventConsumer.java
│       │   ├── inbound/web/controller/  # Web 控制器
│       │   │   ├── AiController.java                  # AI 服务端点
│       │   │   ├── CreditScoreController.java         # 信用分数端点
│       │   │   ├── AdminSearchReindexController.java  # ES 重索引管理
│       │   │   ├── HealthController.java              # 健康检查
│       │   │   └── PlatformStatsController.java       # 平台统计
│       │   └── outbound/                  # 跨模块适配器实现（完整清单见下方「跨模块适配器」表）
│       │       ├── admin/                 # AdminProductQueryAdapter / AdminUserQueryAdapter / AdminOrderQueryAdapter / AdminRatingQueryAdapter
│       │       ├── elasticsearch/         # ES 搜索索引适配器（ElasticsearchIndexManager / ProductDocument / ReindexService / 索引读写适配器）
│       │       ├── payment/               # OrderPaymentGatewayAdapter
│       │       ├── product/               # ProductOrderAdapter / OrderProductQueryAdapter / ProductNotificationAdapter / ProductSearchIndexAdapter / FavoriteProductInfoAdapter
│       │       └── user/                  # MessageUserInfoAdapter / SellerInfoAdapter
├── src/main/resources/
│   ├── application.yaml                   # 基础配置
│   ├── application-dev.yaml               # 开发环境
│   ├── application-prod.yaml              # 生产环境
│   ├── application-test.yaml              # 测试环境
│   ├── logback-spring.xml                 # 日志配置
│   └── db/
│       ├── migration/                     # Flyway 迁移脚本 (V=版本, R=可重复)
│       │   ├── V1__init_schema.sql              # 完整 DDL（合并原 V1~V6）
│       │   ├── R__seed_categories.sql           # 分类种子数据（含二级）
│       │   ├── R__seed_message_templates.sql    # 消息模板种子数据
│       │   └── R__seed_payment_config.sql       # 支付渠道配置
│       └── dev/                           # 开发环境数据
│           └── R__insert_dev_test_data.sql
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
├── easyorange-ai                       # AI 端点（AiController / CreditScoreController / 搜索增强）
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
| `application.yaml` | 基础配置 | 数据源、Redis、MyBatis-Plus、虚拟线程 |
| `application-dev.yaml` | 开发环境 | 小连接池、详细日志、JWT 开发密钥 |
| `application-prod.yaml` | 生产环境 | 大连接池、SSL、Swagger 关闭、优雅停机 |
| `application-test.yaml` | 测试环境 | 测试环境配置（Testcontainers 已移除） |

### 关键配置项

- `jwt.*` — JWT 配置（RSA PEM 密钥路径 `jwt.private-key-location`/`jwt.public-key-location`、access/refresh 过期时间；开发环境自动生成密钥，见 `JwtProperties`）
- `security` — 安全相关 (白名单路径 `security.product-paths` 等)
- `rate-limit-filter` — 限流+防重 Filter 配置（规则列表、防重间隔、方法匹配）
- ~~`thread-pool.*` — 线程池配置~~（已移除，改用虚拟线程，仅保留 `taskScheduler` 硬编码为 poolSize=5）
- `file.upload.*` — 文件上传路径 (`path`) 和 URL 前缀 (`url-prefix`)
- ~~`easyorange.idgen.*`~~ — ID 生成器配置（已移除，UUID v7 零配置零依赖）
- `easyorange.cache.*` — 多级缓存配置（`image.max-size`、`image.expire-hours`、`l1.max-size`、`l1.expire-minutes`、`l2.expire-minutes`、`l2.negative-expire-seconds`；负缓存默认 30s，L1 TTL 必须 ≤ L2 TTL）
- ~~`http-client.*`~~ — HTTP 客户端超时和协议版本（已删除，Spring Boot 4 自动配置 RestClient）

### 日志配置 (logback-spring.xml)

- `LOG_PATH` — 日志目录，默认 `./logs`，生产环境建议设置为绝对路径
- 输出：CONSOLE + ASYNC_FILE (app.log) + ASYNC_ERROR_FILE (error.log)
- 滚动策略：按天 + 大小（100MB/文件，30 天保留，总量 3GB）
- MDC：`traceId` 由 Micrometer Tracing 自动注入。虚拟线程自动继承 MDC，无需额外配置；`taskScheduler` 通过 `MdcTaskDecorator` 传播（见 framework/AGENTS.md）
- AsyncAppender：生产环境文件日志异步写入，队列满不阻塞业务线程

## Flyway 迁移规范

- 版本号格式: `V{N}__description.sql` (N 为递增整数)
- DDL 放 `db/migration/`，开发数据放 `db/dev/`
- 项目开发阶段的所有 V 迁移已合并为单个 `V1__init_schema.sql`（当前完整 DDL）
- 后续 DDL 变更按递增版本号添加 `V{N+1}__description.sql`
- **禁止修改已执行的迁移脚本**（生产环境原则；开发阶段若需重置，清库重跑即可）
- 新增字段必须可空或有默认值
- 迁移脚本中不写业务逻辑

> **清库重置**：`DROP DATABASE easyorange; CREATE DATABASE easyorange;` 后重跑即可应用新 V1。
> 因为合并后 V1 内容变更，已执行过旧 V1~V6 的数据库需要重置。

## 架构守卫测试

`ArchitectureRulesTest.java` 使用 ArchUnit 验证 DDD 分层规则：

- domain 层不依赖 adapter 层
- domain 层不依赖 Spring 框架
- 包依赖方向合规
- 端口接口必须有适配器实现
- 业务模块不直接导入其他模块的领域类
- 白名单已清零（2026-07-04），所有规则严格合规

## 跨模块适配器

`adapter/outbound/` 目录存放跨模块端口接口的适配器实现：

| 适配器 | 端口接口 | 模块 | 功能 |
|--------|---------|------|------|
| `OrderPaymentGatewayAdapter` | `PaymentGatewayPort` | order | 支付网关调用 |
| `ProductOrderAdapter` | `ProductOrderPort` | order | 订单生命周期产品操作 |
| `OrderProductQueryAdapter` | `ProductQueryPort` | order | 商品查询 |
| `SellerInfoAdapter` | `SellerInfoPort` | product | 资产方信息查询 |
| `MessageUserInfoAdapter` | `UserInfoPort` | message | 用户信息查询 |
| `FavoriteProductInfoAdapter` | `ProductInfoPort` | favorite | 商品信息查询 |
| `ProductNotificationAdapter` | `ProductNotificationPort` | product | 商品事件通知（发布、售出、库存预警） |
| `ProductSearchIndexAdapter` | `ProductSearchIndexPort` | product | MySQL search_text 索引写入 |
| `ElasticsearchProductSearchIndexAdapter` | `ProductSearchIndexPort` | product | ES 搜索索引写入（条件激活） |
| `ElasticsearchProductSearchQueryAdapter` | — | — | ES 商品搜索查询（含分面聚合） |
| `AdminProductQueryAdapter` | `AdminProductQueryPort` | admin | 管理端商品查询 |
| `AdminOrderQueryAdapter` | `AdminOrderQueryPort` | admin | 管理端订单查询 |
| `AdminUserQueryAdapter` | `AdminUserQueryPort` | admin | 管理端用户查询 |
| `AdminRatingQueryAdapter` | `AdminRatingQueryPort` | admin | 管理端评价查询 |

`adapter/outbound/elasticsearch/` 搜索基础设施组件：

| 组件 | 职责 |
|------|------|
| `ElasticsearchIndexManager` | ES 索引创建/映射管理 |
| `ProductDocument` | ES 索引映射 POJO |
| `ReindexService` | MySQL → ES 全量重建索引 |
| `ElasticsearchProductSearchIndexAdapter` | 索引写入适配器（`@ConditionalOnProperty` 激活） |
| `ElasticsearchProductSearchQueryAdapter` | 索引查询适配器（分类/价格/成色分面聚合） |

`adapter/event/` 目录存放跨模块事件消费者（通过 Spring Modulith 的 EVENT_PUBLICATION 表持久化后异步分发到 RabbitMQ）：

| 消费者 | 事件 | 功能 |
|--------|------|------|
| `OrderNotificationEventConsumer` | `OrderCreatedEvent` 等 6 个订单事件 | 订单状态变更→站内消息通知 |
| `ProductAuditEventConsumer` | `ProductAuditedEvent` | 审核结果→站内消息通知 |
| `ReportProcessedEventConsumer` | `ReportProcessedEvent` | 举报处理结果→站内消息通知 |
| `AiProductEventConsumer` | `ProductCreatedEvent` / `ProductUpdatedEvent` / `ProductMarkedSoldEvent` | 商品→AI 智能估值 + 营销文案生成 |
| `AiCreditEventConsumer` | `OrderCompletedEvent` / `ReportProcessedEvent` | 交易/举报→信用分重算 |

所有事件消费者使用 `@RabbitListener` + `EventIdempotencyChecker` 模式，通过 Modulith at-least-once 语义 + 幂等去重实现精确一次处理。

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
3. 不合规的需先修复至合规，禁止新增白名单（白名单已清零，保持零例外）
