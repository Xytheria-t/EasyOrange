# EasyOrange — LLM × DDD：Java 架构工程化实战

> **定位**：LLM × DDD 工程化实战项目 — 在 DDD 六边形架构里集成 LLM，让 AI 链路可换供应商、可降级、可观测。**两条技术主线**：AI 应用工程化（6 决策点 + 轻量 Agent 编排 + 8 件套工程化）+ 架构落地（DDD + 分布式可靠性 + ADR/ArchUnit/PIT 治理三板斧）。**业务**：C2C 资产流转（固定价格 + 直发 + 平台不碰货），把复杂度留给架构与 AI 工程化。**工程亮点**：DDD 六边形 + CQRS · 事件驱动 + Outbox + DLQ 三级重试 + traceId 全链路 · 分布式锁防超卖 · AI 8 件套（Spring AI 2.0 框架化 + Redisson 令牌桶 + stale 降级 + Prompt YAML + TokenBudget + Embedding 真实现）· ES 搜索 + IK 分词 · ArchUnit 6 条规则 · 7 ADR · 2,412 测试（后端 1,356 + 前端 1,056）/ Domain 层 84.1% 行覆盖（JaCoCo + PIT 变异测试双重门禁）· SpringDoc OpenAPI 3 · Biome 0 errors。**2025 年 11 月启动**。

## 技术栈

| 层 | 技术 |
|---|------|
| **后端** | Java 25, Spring Boot 4.0.7, MyBatis-Plus 3.5.16 |
| **前端** | TypeScript, React |
| **数据库** | MySQL 8.4, Redis 7.4 |
| **消息队列** | RabbitMQ 3.13 (Spring AMQP 4.0.x) |
| **搜索引擎** | Elasticsearch 8.17.3 (IK 中文分词器) |
| **认证** | JWT Access (RSA) + Opaque Refresh (Redis, HttpOnly Cookie) |
| **迁移** | Flyway 11.15.0 |
| **部署** | Docker, docker-compose, compose.yaml (@ServiceConnection) |

## 数据库表清单

| 表名 | 说明 | 备注 |
|------|------|------|
| `eo_user` | 用户信息表 | user_type 枚举: 00(ADMIN)/01(NORMAL)/02(MANAGER) |
| `eo_product` | 商品信息表 | 6状态(VARCHAR code, 按生命周期): DRAFT/PENDING_REVIEW/REJECTED/ONLINE/OFFLINE/SOLD |
| `eo_product_audit_log` | 审核记录表 | action: 1通过/2拒绝/3重提交; 含维度JSON+前后状态快照 |
| `eo_product_detail` | 商品详情表 | JSON 格式 |
| `eo_product_image` | 商品图片表 | 1:N, 含 is_main 主图标记 + sort_order 排序; 图片不在 eo_product 表上 |
| `eo_product_report` | 举报记录表 | 4状态: PENDING(0)/PROCESSING(1)/RESOLVED(2)/DISMISSED(3); 含reason_type分类+24h重复检测 |
| `eo_report_handle_history` | 举报处理历史表 | 记录每次管理员操作(action/remark/operator_id) |
| `eo_upload_file` | 文件上传记录表 | 含 storage_type/storage_key 支持多后端存储 |
| `eo_user_credit` | 用户信用分表 | score(0-200)+total_trades+completed_trades+cancelled_trades+total_reports+confirmed_reports+avg_rating |
| `eo_order` | 订单主表 | total_amount(行项总和); 状态机: PENDING_PAYMENT/PAID/SHIPPED/COMPLETED/CANCELLED/REFUNDED |
| `eo_order_item` | 订单行项表 | 含 product_snapshot JSON 快照, unit_price, quantity, subtotal |

## 商品状态机

```
                    ┌──────────────────┐
                    │    DRAFT         │
                    └────┬────┬────────┘
                         │    │
                    submit    putOnline (ADMIN bypass)
                         │    │
                    ┌────▼────▼─────────┐
                    │  PENDING_REVIEW   │
                    └────┬────────┬─────┘
                    approve    reject
                         │    ┌────▼─────┐
                    ┌────▼────▼─┐  REJECTED  │
                    │  ONLINE   │  └────┬─────┘
                    └──┬─────┬──┘  submit (循环)
                  takeOffline  markAsSold
                       │      │
                  ┌────▼──┐   │
                  │OFFLINE│   │
                  └───┬───┘   │
                 putOnline    │
                  (relist)    │
                       ┌──────▼──────┐
                       │    SOLD     │ (终端状态)
                       └─────────────┘
```

- **资产方角度**：`DRAFT → submitForReview → PENDING_REVIEW → (approve → ONLINE)` / `REJECTED → submitForReview`（循环）。前端 C 端仅展示 submitForReview 按钮
- **管理员角度**：`DRAFT` / `OFFLINE → putOnline → ONLINE`（绕过审核直接上架）。`putOnline` 端点受 `@PreAuthorize("hasRole('ADMIN')")` 保护
- **系统角度**：`ONLINE → markAsSold → SOLD`（订单完成时由订单生命周期事件消费者触发）
- **卖家**：`ONLINE → takeOffline → OFFLINE`
- 审核结果触发站内消息通知（AUDIT_SUCCESS / AUDIT_REJECTED）
- 审核记录持久化至 `eo_product_audit_log` 表（action: 1通过/2拒绝/3重提交; 含维度JSON+前后状态快照）

状态机定义在 `ProductStatus.java`：枚举按生命周期声明（DRAFT → PENDING_REVIEW → REJECTED → ONLINE → OFFLINE → SOLD），所有合法转换集中在一张 `ALLOWED_TRANSITIONS` 表（单一事实来源，行内注释标注各转换的触发动作），聚合根通过 `transitionTo(target, action)` 统一守卫。终端状态用 `isTerminal()`（仅 SOLD）判断。delete 不改变 status，单独由 `canDelete()` 守卫（SOLD 状态下的商品不可删除以保留订单追溯记录）。库存恢复不变量 `canRestoreStock()`（SOLD/OFFLINE 不可恢复库存）由 `Product.restoreStock()` 调用。进入 ONLINE 只有 `approve`（审核通过）和 `putOnline`（管理员直上架）两条路径，统一经 `Product.validateOnline()`（isComplete + hasValidPrice + hasStock）守卫，禁止新增第三条绕过校验的上架路径。

## AI 能力清单

> **业务定位**：项目选 C2C 资产流转作为业务载体，AI 能力是**核心叙事**——6 个 AI 决策点全面框架化为 Spring AI 2.0（ADR-0008，Supersedes ADR-0003）：直接注入 Spring AI `ChatModel` / `EmbeddingModel` bean（DeepSeek / Qwen-VL / DashScope），叠加令牌桶限流 + stale 降级 + Prompt 版本化 + Token 预算治理，让 AI 从"demo 调用"走到"生产级工程"。议价 / 阶梯降价 / AI 自动成单等"AI 替资产方运营"的营销叙事已在 2026-06-25 下线。
>
> **平台边界**：平台不碰货、不囤货、不经手资金，物流走资产方→认领方 C2C 直发。
> 资产方只需发布资产、设固定价格，平台 AI 在两端做生产级工程实践；认领方获得 AI 找货 / 评估 / 信用画像等能力。
> **详细机制**（智能估值 / AI 营销文案 / WebSocket 实时沟通协议）见 [doc/集成/AI-资产管理.md](doc/集成/AI-资产管理.md)。

**核心约定**：
- **资产方侧 3 个决策点**：**智能估值** / **AI 营销文案** / **AI 信用画像**
- **认领方侧 3 个决策点**：**AI 智能找货** / **AI 物品评估** / **AI 信用画像**
- 资产方按固定价格上架资产，平台不参与议价 / 不自动调价
- 沟通走 `WebSocket /ws/chat` STOMP 通道（认领方与资产方直聊）
- **模型 Bean（Spring AI 2.0）**：`AiModelConfig` 定义 3 个 bean（统一走 `OpenAiSetup.setupSyncClient` OpenAI 兼容线协议）——`chatModel`（`@Primary`，DeepSeek `deepseek-chat`）、`visionChatModel`（Qwen-VL `qwen-vl-max`）、`embeddingModel`（DashScope `text-embedding-v3`，dimensions=1024 与 ES `dense_vector` 映射对齐）。六个业务服务 + 语义搜索 + 搜索增强直接注入，调用模式去重见 `AiModelSupport`（callText/callJson/embed/analyzeImages）
- **供应商可换（options 切换）**：改 `AiModelConfig` 的 baseUrl/apiKey/model（或 `application.yaml` 的 `easyorange.ai.*`），无需改业务代码；`easyorange.ai.provider` / Python 侧车已随框架化删除（2026-08-03）
- 限流：`AiRateLimitInterceptor` 基于 Redis 令牌桶，Redis 不可用时 fail-open
- **Embedding 真实现**：查询侧 `SemanticSearchService` 用 `embeddingModel` 生成查询向量；索引侧 `ElasticsearchProductSearchIndexAdapter.buildDocument` 注入 `ObjectProvider<EmbeddingModel>` best-effort 写入 `nameEmbedding`（失败降级 null，不阻塞索引）
- **可观测性**：由 Spring AI 2.0 内置 Observation（`spring-ai-autoconfigure-model-chat-observation`）+ Micrometer 提供，暴露到 `/actuator/prometheus`（原 `AiMetricsService` 已删除）

## 举报处理工作流

状态机: `PENDING(0) → PROCESSING(1) → RESOLVED(2)` / `DISMISSED(3)`

- 用户提交举报（选择类型+填写描述），24h内同一用户对同一商品不可重复举报
- 管理员可单条/批量处理（**resolve** 通过/**dismiss** 驳回/IGNORE/PRODUCT_OFFLINE/WARN_SENDER/BAN_PRODUCT）
- 每次处理操作记录至 `eo_report_handle_history` 表
- 处理完成后通过 `ReportProcessedEvent` → `ReportProcessedEventListener` 异步发送站内信通知举报人
- 用户可查看自己的举报列表（分页）和详情（含处理结果）

> **完整 API 速查表**（用户侧 + 管理端 + 所有模块 + AI 功能）见 [doc/集成/API-速查.md](doc/集成/API-速查.md)。

## 项目结构

```
easy-orange/
├── easyorange-backend/          # Spring Boot 后端 (11 Maven 模块)
├── easyorange-frontend/         # React 前端 (Vite + TypeScript + TanStack Query)
├── doc/                         # 项目文档
└── .claude/rules/ecc/           # AI 编码规则 (ECC: common/java/typescript/react/web)
```

## 后端架构核心原则

1. **DDD 分层**: domain → application → adapter，依赖方向单向向内
2. **CQRS**: 命令与查询分离 (product, order, payment 模块)
3. **六边形架构**: domain 层通过 port 接口与外部解耦
4. **不可变性**: 聚合根用 `@Builder(toBuilder = true)`，值对象用 `record`
5. **领域事件**: `DomainEventPublisher` 发布事件 → `ModulithDomainEventPublisher`（`@Primary`）代理到 `ApplicationEventPublisher` → Spring Modulith 在数据库 `EVENT_PUBLICATION` 表中持久化事件（与应用事务同原子） → 异步从 `EVENT_PUBLICATION` 读取并发布到 **RabbitMQ Topic Exchange** (`eo.domain.events`)。路由键由事件类名自动派生（`ProductCreatedEvent` → `product.created`），无需手动注册。每个消费者独占队列（`eo.{name}`），失败消息路由到 DLQ（`eo.{name}.dlq`）→ `DlqRetryScheduler` 每 5 分钟扫描 DLQ，重试次数 < 3 重投主队列，超过 3 次的毒消息转储 `eo.dlq.terminal` 等待人工介入。Modulith 的 at-least-once 语义 + 消费者 `EventIdempotencyChecker` 确保精确一次处理。审计日志同样走 Outbox 模式（`AuditLogAspect` 发布 `AuditLogEvent` → `AuditLogEventConsumer` 异步入库）。采用 RabbitMQ-only 模式（`@ConditionalOnProperty(matchIfMissing=true)` 保留以防无 RabbitMQ 环境）。**traceId 全链路传递**：Brave `TracingFilter` 从 HTTP 头提取 traceId → 写入 MDC → `MdcTaskDecorator` 传递到 @Async 虚拟线程 → `EventMetadataMessagePostProcessor` 发布前把 traceId 注入 RabbitMQ message header → 消费者端 `EventMetadata.from(message, event)` decode 回 MDC → 日志 + Micrometer metrics 统一关联。**已实现 10 个事件消费者**: ProductEventConsumer (内部 CQRS 投影), AiProductEventConsumer (AI 估值/文案), OrderNotificationEventConsumer (站内信), OrderLifecycleEventConsumer (订单生命周期: 取消/退款恢复库存, 完成标记售出), AiCreditEventConsumer (信用分), ProductAuditEventConsumer (审核通知), ReportProcessedEventConsumer (举报通知), WebSocketEventConsumer (消息撤回广播), PaymentMetricsConsumer (支付指标), **AuditLogEventConsumer (审计日志 Outbox 入库)**
6. **Assembler 模式**: DTO 转换统一在 `adapter/inbound/web/assembler/` 目录下实现（FavoriteAssembler, CategoryAssembler, PaymentViewAssembler, UserAssembler）。**禁止**在 Controller/Service 中直接构造 Response DTO。已废弃旧 DTO（AddFavoriteDTO, FavoriteVO, PaymentQuery, PaymentView, PaymentMethodVO 等；`QueryOrderRequest` 仍为现役请求 DTO，Controller 负责转为 `OrderListQuery`）
7. **ACL 隔离**: 跨模块通过 ACL/Port 适配，禁止直接依赖领域模型
8. **异常继承**: 领域异常必须继承 `BaseBusinessException`（common 模块），`GlobalExceptionHandler` 使用 Java 21 模式匹配 switch 在单个 `handle()` 方法内按类型分发，返回动态 HTTP 状态码（按错误码前缀自动映射：A0401→401/A0403→403/B→400/C→500/D→502）+ 业务错误码；校验类错误统一返回 400。**禁止直接抛出非 `BaseBusinessException` 子类的 RuntimeException**，否则会落入 500 兜底。`BusinessException` 和 `FileException` 构造器均设为 `protected`，抛业务异常时统一使用 `BusinessException.of(...)` / `FileException.of(...)` 工厂方法；子类可正常调用 `super(...)`。各模块领域异常必须使用模块专属 `ResultCode`（如 `ProductResultCode.PRODUCT_NOT_FOUND`），**禁止回退到全局 `B0002`**

## 模块依赖关系

```
application → framework, common, user, product, order, payment, message, favorite
framework → common
user → framework, common
product → framework, common, user (通过 SellerInfoPort 隔离，optional)
order → framework, common, product, user, payment (通过 Port 接口隔离，optional)
payment → framework, common
message → framework, common, user (通过 UserInfoPort 隔离，optional)
favorite → framework, common, product (通过 ProductInfoPort 隔离，optional)
ai → framework, common, product (通过 ProductSearchQueryPort 隔离)
admin → framework, common, user (optional), product (optional), order (optional), payment (optional), ai (optional)
```

> **状态**：所有跨模块依赖已通过端口接口 + 适配器模式隔离，Maven 依赖标记为 `<optional>true</optional>`。写操作通过事件驱动解耦，查询操作保留同步端口调用。
>
> **演进说明**：`BaseDO`、`BaseRepository`、`IdGenerator`、`ConcurrentUpdateException` 已从 `framework` 下沉至 `common` —— 业务模块依赖 `framework` 是基础设施层面的模块聚合（每个模块的 `domain/` 包不导入 framework 代码），实际 DDD 层约束由 ArchUnit 在包级别守卫。

## 已知问题

- **OWASP 硬闸门已恢复绿色（2026-08-04）**: 通过依赖升级 + 定向豁免清除全部 CVSS ≥ 8 告警：
  - Spring Boot 4.0.3 → **4.0.7**（spring-framework 7.0.8，修复 **CVE-2026-41855** 9.8）；netty → **4.2.16**、tomcat → **11.0.24**、jackson → **3.1.5**（见 pom `<netty.version>`/`<tomcat.version>`/`<jackson3.version>` 覆盖）
  - **豁免项**（`easyorange-backend/dependency-suppression.xml`，注明理由、待条件满足后解除）：kotlin-stdlib **CVE-2026-53914**（修复版 Kotlin 2.4.20 未发布稳定版；纯 Java 项目不编译 Kotlin，攻击面≈0）；mysql-connector-j **CVE-2026-60193/60192**（受影响组件为 Connector/Net(.NET)，非 Connector/J(Java JDBC)，且 9.7.0 已是最新版）
  - **环境提示**：OWASP 依赖检查已从 `-Pci` 主门禁拆出，改为独立 `-Powasp` profile + CI 非阻断 security job（NVD 在境外 runner 上首次扫描实测 39 分钟，不适合阻塞每次 push/PR；`failOnError=false` 使 NVD 抖动只告警不红，真实 CVSS ≥ 8 仍会标红）。本地默认构建完全跳过 OWASP（提速显著）；按需扫描用 `./mvnw -Powasp verify -DskipTests`。检查需联网拉取数据源（OSS Index API 需 token、RetireJS 从 raw.githubusercontent 下载），本机若遇 401/超时可用 `-Danalyzer.ossindex.enabled=false` + `-Danalyzer.retirejs.enabled=false`，或临时 `-Ddependency-check.skip=true`
- **Redis 连接**: `application.yaml` 的 base 配置和 `.env.example` 模板已统一默认 `REDIS_PASSWORD=easyorange123`，与 Docker Compose 一致。若仍遇到 `Unable to connect to Redis` 错误，检查：① 是否已执行 `docker compose up -d` 启动 Redis；② 环境变量 `REDIS_PASSWORD` 是否被设置为空值覆盖了默认值；③ **YAML 占位符必须用 `${VAR:default}`（单冒号），不要用 bash 风格的 `${VAR:-default}`**（多一个 `-`）—— Spring 会把 `-default` 当字面量默认值，导致 Lettuce 实际发出去的密码比预期多一个前导连字符，触发 `WRONGPASS invalid username-password pair`。**注意**：`docker-compose.yml` 和 `mvnw` 用 `:-` 是正确的（bash/Docker Compose 语法），但所有 `application*.yaml` 必须用单冒号。新增/修改 Spring Boot 配置占位符时，复制粘贴前先确认语法

## 错误码规范

错误码采用 **A/B/C/D 前缀格式**：

| 前缀 | 类型 | HTTP 状态 | 示例 |
|------|------|----------|------|
| A | 成功/客户端语义 | 200/4xx | A0000=成功, A0401=未登录, A0403=禁止访问 |
| B | 业务错误 | 400 | B0001=操作失败, B0002=业务异常 |
| C | 系统错误 | 500 | C0500=服务器内部错误 |
| D | 第三方错误 | 502 | D0502=上游服务不可用 |

判断成功：`"A0000".equals(code)`

### 错误码模块分配表

B 前缀（业务错误码）按模块分段，新增模块时在预留段内分配：

| 范围 | 模块 | 实现类 |
|------|------|--------|
| B000x | 全局通用 | `ResultCode` |
| B1xxx | user | `UserResultCode` |
| B2xxx | product | `ProductResultCode` |
| B3xxx | order | `OrderResultCode` |
| B4xxx | payment | `PaymentResultCode` |
| B5xxx | file | `FileResultCode` |
| B6xxx | admin | `AdminResultCode` |
| B7xxx | message | `MessageResultCode` |

> 新增模块时优先使用 B8xxx-B9xxx 预留范围，避免与现有范围冲突。各枚举统一使用 `@Getter @AllArgsConstructor` 模式实现 `IResultCode`。

## 架构文档

详细架构规范见 `doc/架构/` 目录：

| 文档 | 内容 |
|------|------|
| [架构.md](doc/架构/架构.md) | 主索引文档 |
| [架构-技术栈.md](doc/架构/架构-技术栈.md) | 技术选型 |
| [架构-系统架构.md](doc/架构/架构-系统架构.md) | 整体架构、模块划分 |
| [架构-模块结构.md](doc/架构/架构-模块结构.md) | 包结构规范 |
| [架构-DDD规范.md](doc/架构/架构-DDD规范.md) | DDD 设计规范 |
| [架构-安全认证.md](doc/架构/架构-安全认证.md) | JWT 认证 |
| [架构-数据库迁移.md](doc/架构/架构-数据库迁移.md) | Flyway 规范 |
| [架构-部署演进.md](doc/架构/架构-部署演进.md) | 部署与演进 |

## 集成文档

业务专题与 API 速查见 `doc/集成/` 目录：

| 文档 | 内容 |
|------|------|
| [AI-资产管理.md](doc/集成/AI-资产管理.md) | 资产方 / 认领方 6 个 AI 决策点 / 营销文案 / WebSocket 协议 / 资产方直发边界 |
| [API-速查.md](doc/集成/API-速查.md) | 后端所有 REST + WebSocket 端点速查 |

## 环境变量

| 变量 | 值 | 说明 |
|------|------|------|
| `DEEPSEEK_API_KEY` | - | DeepSeek API 密钥（智能估值/审核/问答/Embedding） |
| `DEEPSEEK_BASE_URL` | `https://api.deepseek.com/v1` | DeepSeek API 地址 |
| `DEEPSEEK_MODEL` | `deepseek-chat` | DeepSeek 文本模型 |
| `QWENVL_API_KEY` | - | 通义千问 VL API 密钥（拍照上架图片识别） |
| `QWENVL_BASE_URL` | `https://dashscope.aliyuncs.com/compatible-mode/v1` | 通义千问 API 地址 |
| `QWENVL_MODEL` | `qwen-vl-max` | 通义千问视觉模型 |
| `VITE_WS_URL` | 自动推导 | WebSocket 地址（前端环境变量，默认从 `location.host` 推导） |

## 开发规范

- 编码规则见 `.claude/rules/ecc/` 目录（ECC 分层规则集：common 通用 + java/typescript/react/web 语言专用，按文件路径自动激活，语言级规则优先级高于 common）
- **开发中指标收口延后**：开发阶段（未整体完成前）每次改动只做**增量验证**——跑改动涉及模块/文件的单测与集成测试即可，不做全量测试统计、不核查 JaCoCo/PIT 覆盖率、不刷新 `doc/工程指标.md` 的测试数数字（这些只在开发整体收口时统一跑一次并落档）。用户明确要求核查时除外
- 架构守卫测试: `ArchitectureRulesTest.java` (ArchUnit)
- 数据库变更必须通过 Flyway 迁移脚本
- 所有 API 统一返回 `Result<T>`，分页返回 `PageResult<T>`（搜索返回 `SearchPageResponse<T>`，包含 `records/total/current/size/pages` + `facets` 分面桶 + `aiEnhancement` 增强）
- 覆盖率报告由 **JaCoCo 0.8.14** 在 `prepare-package` 阶段生成（`jacoco:report`），门禁（行≥80%/分支≥60%）在 `-Pci` profile：本地默认构建跳过门禁（`prepare-agent`/`report` 仍在默认构建，`./mvnw test` 照常收集覆盖率），CI 用 `./mvnw -Pci verify` 启用，`-Djacoco.haltOnFailure=true` 可强制覆盖率阻断。**OWASP Dependency Check 12.1.0**（CVSS ≥ 8 标红）已拆出为 `-Powasp` profile，由 CI 非阻断 security job 单独跑（NVD 境外首次扫描 ~39 分钟，不阻塞主门禁）
- **变异测试（PIT 1.25.8）**: 行/分支覆盖率只测"代码被执行过"，不测"测试能否发现缺陷"。PIT 向 domain 层注入变异（聚合根状态机/领域服务/值对象），用现有测试杀灭变异来评估测试真实质量 — 行覆盖率的"金标准"补充。默认不启用（较慢），按需 `./mvnw -Ppit test-compile pitest:mutationCoverage`，HTML 报告 `target/pit-reports/index.html`；阈值门禁默认 0 不阻断，CI 用 `-Dpit.mutationThreshold=60 -Dpit.testStrengthThreshold=75 -Dpit.coverageThreshold=70` 启用（order 模块基线 70%/89%/81%）
- **标准 API 优先（STP）**: 优先使用框架/标准库内置功能，不重复造轮子。Spring Security 有 JWT 认证就通过 `oauth2ResourceServer()` 配置，不要手写 Filter；有标准 `JwtDecoder`/`JwtEncoder` 就注入使用，不要手写 JWT 工具类。"零新增自定义代码"是最优方案——删掉手写代码，换成框架配置即可
- **测试统计**：后端 11 模块合计 1,356 测试用例 / 1,278 注解 / 153 文件（2026-08-02 实测全绿，Domain 层行覆盖率 84.1%，`mock-maker-subclass` 模式）；前端 112 测试文件 / 1,056 测试用例。数字单一来源见 [doc/工程指标.md](doc/工程指标.md) §1.2
- **前端组件规范**：表单/按钮统一使用 shadcn/ui（Button、Input、Label、Checkbox、Switch、Select、Textarea、RadioGroup），禁止保留原生 `<button>` / `<input>` / `<textarea>` / `<select>`；导入优先走 `@/components/ui`，颜色/边框硬编码应提取到 `src/styles/tokens.css`；CSS reset 必须置于 `@layer base` 避免覆盖 Tailwind utilities；生产代码禁止 `console.log` / `console.warn` / `console.error`
- **前端表单校验**：所有包含显式校验逻辑的表单（登录/注册/发布/密码修改等），必须使用 `react-hook-form` + `zod` + `@hookform/resolvers` 方案，禁止手写 `useState` + 自定义 `validate` 函数。Zod schema 统一放在 `src/schemas/` 目录下，`.default()` 禁止使用（默认值通过 `useForm` 的 `defaultValues` 设置以保持类型推导正确）。`reValidateMode` 统一设为 `'onChange'` 使得首次提交后输入即时清除错误。管理端搜索/筛选类简单表单（如 CategoryManagePage）无需迁移
- **TestSecurityUtil**: 测试中禁止使用 `mockStatic(SecurityContextUtil.class)`（不支持静态 mock）。改用 `TestSecurityUtil.setSecurityContext(userId) + finally { clearSecurityContext() }` 模式，位于 `easyorange-framework/src/main/java/.../framework/util/TestSecurityUtil.java`
- **全局认证拦截**: SecurityConfig 的 `.anyRequest().authenticated()` 已在过滤器层拦截所有未认证请求，Controller 方法上**无需**重复添加 `@PreAuthorize("isAuthenticated()")`。仅在需要角色/权限校验时使用 `@PreAuthorize`（如 `hasRole('ADMIN')`）
- **UUID v7 ID**: 全库 ID 使用 UUID v7 (RFC 9562, String)，已彻底移除 Snowflake 备选代码。后端通过 `IdGenerator` 接口（`UuidV7IdGenerator` 为 `@Primary` 实现）生成 36 位 UUID 字符串。`BaseDO.id` 字段类型为 `String`，`@TableId(type = IdType.INPUT)`。前端实体 ID 字段类型保持 `string`（无需更改，JS 始终兼容字符串）。`V1__init_schema.sql` 直接使用 `VARCHAR(36)`。
- **全量 Long→String 迁移**: 涉及所有模块——领域事件、值对象、DO、DTO、Port 接口、Adapter、Controller、测试文件。`SecurityContextUtil.getCurrentUserIdOrThrow()` 返回 `String`（原 Long）。`SnowflakeConfig`/`IdGenProperties` 等 Snowflake 配置已彻底移除，仅保留 `UuidV7IdGenerator`。`TestSecurityUtil.setSecurityContext()` 同时保留 `Long` 和 `String` 重载。全项目 2,412 测试通过（1,356 后端 + 1,056 前端）。
- **DO 枚举字段使用 TypeHandler**: DO 中 `status`、`condition_level` 等枚举字段直接使用领域枚举类型（`ProductStatus`、`ConditionLevel`），通过自定义 TypeHandler 持久化。框架提供 `IntegerCodeEnumTypeHandler`（TINYINT/INT 列）和 `CodeEnumTypeHandler`（VARCHAR 列）两个基类。新增枚举字段时：① 创建 TypeHandler 继承对应基类，标注 `@MappedTypes`；② 将 TypeHandler 所在包加入 `application.yaml` 的 `mybatis-plus.type-handlers-package`；③ DO 字段类型改为枚举。各模块测试通过后即为就绪。
- **React Query 缓存**: mutation 后 `invalidateQueries` 必须使用 `ORDER_KEYS.all` 前缀匹配，确保 myOrders/soldOrders/detail 等所有查询都能被正确失效
- **零配置启动**: 项目支持零配置开发环境启动（MySQL localhost:3306, Redis localhost:6379）。新开发者只需 `./mvnw install -DskipTests && ./mvnw spring-boot:run -pl easyorange-application` 即可运行。敏感配置通过根目录 `.env.example` 模板管理，复制为 `.env` 即可（IDEA、Docker Compose 通用）
- **.gitignore 规范**: 使用精简版 .gitignore (98行)，已忽略 AI 生成文件 (**/codemap.md, 298个)、AI 工具目录 (.slim/, .superpowers/)、前端 .env.production/.env.development、测试产物 (test-results/)
- **Java `var` 使用规范**: 局部变量推荐使用 `var` 的场景：同一类型构造器（`Foo x = new Foo()` → `var x = new Foo()`）、显式 cast（`Type x = (Type) expr` → `var x = (Type) expr`）、StringBuilder/ByteArrayOutputStream 等无泛型构造器。**不推荐**的场景：接口类型到实现类型的赋值（`List<X> x = new ArrayList<>()` → 保持 `List<X>`，使用 `var` 会丢失接口抽象）
- **可靠性工程模式**（2026-07-27 企业级差距优化后落地）：① **审计日志 Outbox** — `AuditLogAspect` 不再直接入库，改为发布 `AuditLogEvent` 走 Spring Modulith Outbox（事务一致 + 崩溃恢复），`AuditLogEventConsumer` 异步消费写库；② **DLQ 三级重试** — `DlqRetryScheduler` 每 5 分钟扫描 DLQ，重试次数 < 3 重投主队列，超限转储 `eo.dlq.terminal`；③ **Redisson 分布式令牌桶** — `DistributedRateLimiter` 基于 `RRateLimiter` 替代 `increment+expire` 固定窗口，解决原子性缺口 + 边界突刺；④ **L1 缓存广播失效** — `MultiLevelCache` evict 时 Redis Pub/Sub 广播，`CacheInvalidationListener` 订阅清除本地 Caffeine；⑤ **订单创建一致性** — 下单链路 = 本地单 `@Transactional`（订单/库存/支付/Outbox 原子提交）+ Redisson 分布式锁（按 productId 排序防死锁防超卖）+ Outbox 事件副作用；**拒绝 Saga**：单库场景下反向补偿与回滚重复、失败状态随事务回滚丢失，决策记录见 [ADR-0007](doc/adr/0007-order-saga-single-tx-observability.md)；⑥ **Resilience4j Bulkhead** — aiLlm/aiVision/dbHeavy 三隔离舱限制并发；⑦ **SpringDoc OpenAPI 3** — `/v3/api-docs` + `/swagger-ui.html`

## 常用命令

```bash
# Git 操作（远程 origin = GitHub，push/PR 触发 GitHub Actions）
git push origin develop
git pull origin develop

# 后端构建
cd easyorange-backend && ./mvnw clean package -DskipTests

# 单测（surefire，*Test，快速）
./mvnw test

# 集成测试（failsafe 绑定 verify，*IT）：复用 docker compose dev 栈（MySQL/Redis/RabbitMQ），
# 需先 `docker compose -f compose.yaml up -d`。相比 Testcontainers 不依赖 ryuk sidecar（无代理 Docker 下不会被静默跳过）
./mvnw verify

# 运行特定模块测试
./mvnw test -pl easyorange-framework
./mvnw test -pl easyorange-admin
./mvnw test -pl easyorange-order
./mvnw test -pl easyorange-message -am
./mvnw test -pl easyorange-product -am

# 生成 JaCoCo 覆盖率报告
./mvnw clean test jacoco:report

# PIT 变异测试（仅 domain 层，HTML 报告 target/pit-reports/index.html）
./mvnw -Ppit test-compile pitest:mutationCoverage
./mvnw -pl easyorange-order -Ppit test-compile pitest:mutationCoverage  # 单模块

# JaCoCo 覆盖率门禁（行≥80%/分支≥60%，CI 硬闸门）
# OWASP 依赖安全（非阻断 security job，CVSS ≥ 8 标红）：./mvnw -Powasp verify -DskipTests
# 豁免项见 easyorange-backend/dependency-suppression.xml
./mvnw -Pci verify

# 启动开发环境 (MySQL 8.4 + Redis 7.4 + RabbitMQ 3.13)
docker compose -f compose.yaml up -d

# 可选: ES 搜索 (需先构建镜像: docker compose build elasticsearch)
docker compose up -d elasticsearch

# 启动后端（⚠️ 不要单独用 spring-boot:run -pl，会从本地仓库加载依赖模块旧 JAR）
# 正确方式：先打包所有依赖模块，再 java -jar
cd easyorange-backend && ./mvnw clean package -DskipTests -pl easyorange-application -am && java --sun-misc-unsafe-memory-access=allow -jar easyorange-application/target/easyorange-application-*.jar

# 或先 install 所有模块到本地仓库，再用 spring-boot:run（spring-boot-maven-plugin 已配置 jvmArguments）
cd easyorange-backend && ./mvnw install -DskipTests && ./mvnw spring-boot:run -pl easyorange-application

# 前端测试
cd easyorange-frontend

# 运行所有单元/组件/Hook 测试
npm test

# 监听模式
npm run test:watch

# 覆盖率报告
npm run test:coverage

# E2E 测试 —— 跑在 vite preview（生产构建产物），自动 npm run build 后再跑；
# 纯 page.route mock 双 Token 会话恢复流，不依赖真实后端。WSL2 慢机抗性见
# playwright.config.ts / tests/e2e/global-setup.ts 注释（retries 2、预热、禁 networkidle）
npm run test:e2e

# 生产构建 + Bundle 分析（rollup-plugin-visualizer 输出 dist/stats.html，treemap 可视化）
npm run build:analyze
```

CI/CD: `.github/workflows/maven-test.yml` — push/PR 到 develop/main 触发（后端 11 模块测试 + OWASP/JaCoCo 门禁 → 前端 lint/test/typecheck/build → Playwright E2E），远端为 GitHub Actions

## Repository Map

`codemap.md` 为 AI 生成的可选索引（已被 `.gitignore` 忽略），若存在则工作前先读对应的 `codemap.md` 了解入口、模式和数据流；不存在时以本文档 + `doc/` 为准。
