# Changelog

> 基于 Git 提交记录自动生成

## [unreleased]

### 2026-08-03 — AI 层全面框架化为 Spring AI 2.0（ADR-0008，Supersedes ADR-0003）

- **refactor(ai)**: 自研 AI 层删除 14 个类——`LlmPort` / `VisionPort` / `DeepSeekLlmAdapter` / `QwenVlVisionAdapter` / `CachingLlmAdapter` / `CachingVisionAdapter` / `PythonLlmAdapter` / `adapter/dto/*`（4 个手写 JSON DTO）/ `AiMetricsService` + 对应 5 个测试。六个业务服务 + 语义搜索 + 搜索增强直接注入 Spring AI `ChatModel` / `EmbeddingModel` bean，删除自研 L1/L2 多级缓存（仅保留 stale Caffeine 24h TTL 供限流降级）
- **feat(ai)**: `AiModelConfig` 定义 3 个 Spring AI 2.0 bean（统一 `OpenAiSetup.setupSyncClient` OpenAI 兼容线协议）——`chatModel`（`@Primary`，DeepSeek）、`visionChatModel`（Qwen-VL，字段级 `@Qualifier`）、`embeddingModel`（DashScope `text-embedding-v3`，dimensions=1024 与 ES `dense_vector` 映射对齐）；新增 `AiModelSupport` 静态调用去重（callText/callJson/embed/analyzeImages）
- **feat(ai)**: **Embedding 真实现**——查询侧 `SemanticSearchService` 用 `embeddingModel.embed(keyword)` 生成查询向量；索引侧 `ElasticsearchProductSearchIndexAdapter` 注入 `ObjectProvider<EmbeddingModel>` best-effort 写 `nameEmbedding`（失败降级 null，不阻塞索引）
- **fix(ai)**: `AiModelSupport.analyzeImages` 补 `Media.Format.IMAGE_JPEG` mimeType——Spring AI 2.0 `Media.Builder.build()` 要求 mimeType 非空，此前多图视觉调用会抛 `IllegalStateException`
- **chore(infra)**: 删除 `easyorange-python/` 侧车（compose.yaml `python-ai` service + `.env.example` AI_PYTHON 块 + `.gitignore` Python 规则）；`easyorange.ai` 配置重写（删 provider/python/cache.enabled/l1-*，新增 embedding 块）；`Resilience4jConfig` 删 aiLlmRetry/aiVisionRetry/aiLlmBulkhead/aiVisionBulkhead（重试 `MAX_RETRIES=2` 由 openai-java 客户端内置承担）；可观测性由 Spring AI 2.0 内置 Observation + Micrometer 提供
- **test**: 重写 4 个 service 测试 + `TokenBudgetAspectTest`（8 测试） + `AiRateLimitInterceptorTest`（5 测试）去 AiMetricsService；新增 `AiModelSupportTest` / `AiCopyGenerationServiceTest` / `AutoListingServiceTest` / `JdbcCreditScoreFetcherTest` / `SemanticSearchServiceTest`。AI 模块 123 → **150** 测试全绿，easyorange-ai 覆盖率回到门禁之上（**行 85.0% / 分支 82.0%**，目标 80%/60%）
- **docs**: 新增 [ADR-0008](doc/adr/0008-ai-migrate-to-spring-ai-framework.md)（Supersedes ADR-0003）；同步根 README / 根 AGENTS.md / easyorange-ai/AGENTS.md / easyorange-backend/AGENTS.md / easyorange-framework/AGENTS.md / CLAUDE.md / PRODUCT_DIRECTION.md / doc/集成/AI-资产管理.md / doc/架构/架构-系统架构.md / doc/工程指标.md / doc/面试-简历使用指南.md / doc/技术债务清单.md / .env.example

### 2026-08-02 — 移除订单创建 Saga 层（拒绝 Saga）

- **refactor(order)**: 移除订单创建 Saga 层，回归**本地单事务 + Redisson 分布式锁 + Outbox 事件**（[ADR-0007](doc/adr/0007-order-saga-single-tx-observability.md) 重写为「拒绝 Saga」，[ADR-0001](doc/adr/0001-use-saga-over-2pc.md) 标记 Superseded）。删除 10 类：`CreateOrderSaga` / `SagaCoordinator` / `SagaTimeoutScheduler` / `SagaException` / `SagaRepository` / `SagaState` / `SagaStatus` / `SagaDO` / `SagaMapper` / `SagaRepositoryImpl` + `OrderCompensationService`
- **refactor(order)**: 新增 `OrderCreationService`（单一 `@Transactional`：锁 → 建单 + 发布事件 → 同步扣库存 → 建支付，失败整体回滚、无补偿路径）；`DistributedLockManager` / `OrderCreationExecutor` / `OrderPreparationService` 迁入 `application/service/`；`OrderCreationException` / `PaymentGatewayAdapterException` 迁入 `domain/exception/`
- **refactor(order)**: `OrderSagaEventConsumer` → `OrderLifecycleEventConsumer`（队列 `QUEUE_ORDER_LIFECYCLE = eo.order.lifecycle`），消费取消/退款恢复库存、完成标记售出；`RabbitMQConfig` / `DlqRetryScheduler` / `DlqAnomalyListener` 队列引用同步
- **chore(db)**: 新增 Flyway `V2__drop_order_saga_table.sql` 删除 `eo_saga_status` 表
- **test**: 重写 `OrderCreationServiceTest`（成功路径 + 失败回滚 + 锁竞争），删除 `SagaTimeoutSchedulerTest`。后端 11 模块 1,362 → **1,356** 测试全绿（注解 1,284 → 1,278，测试文件 154 → 153）

### 2026-08-02 — order 状态机重构：邻接表 → 动作驱动（OrderAction 单一事实来源）

- **refactor(order)**: 订单状态机从"状态邻接表"升级为**动作驱动**——新增 `OrderAction` 枚举作为唯一事实来源，每个动作声明前置状态集合/目标状态/目标支付状态/是否需要原因/错误码/额外支付前置条件；`OrderStatus` 的 `ALLOWED_TRANSITIONS` 邻接表移除，`canTransitionTo()` 改为由动作派生
- **refactor(order)**: `Order` 聚合根 6 组重复的"守卫 + toBuilder"收敛为单一私有 `transitionTo(action, reason)` 守卫（一处校验合法性 + 一处应用副作用：状态/支付状态/关闭原因时间）；公共方法 `pay()/cancel()/forceCancel()/ship()/confirmReceipt()/refund()` 签名与事件类型不变，外部调用方零改动；修掉 `canCancel()` 硬编码不一致、`cancel/forceCancel` 方法体重复
- **test**: 新增 `OrderActionTest`（12 用例：前置状态矩阵/支付守卫/终端态/元数据），更新 `OrderStatusTest` 派生矩阵守护。后端 11 模块 1,362 测试全绿

### 2026-07-31 — message 模块死代码清理 + 多级缓存简化 + neat-freak 文档同步

- **refactor(message)**: 消除无分层的 `service/` 目录——`MessageArchiveService` + `MessageDomainServiceConfig` 迁入 `application/service/` 与 `application/config/`；删除 3 组死代码（`MessageTemplateService`/`MessageSubscriptionService`/`OfflineMessageService` 接口 + 实现 + 测试，共 9 文件）。`service/` 根目录彻底消除，模块演进路线 5 步全部完成（含 `entity/`→`domain/aggregate/`、`domain/port/` 端口），现为完整 DDD 六边形架构
- **refactor(framework)**: `MultiLevelCache` 回源参数从自定义 `CacheLoader` 接口改为 `java.util.function.Supplier`（stdlib 替代），删除 `cache/CacheLoader.java`。同步移除 `MultiLevelCache.evictL2()`，列表类缓存失效统一走 `evict()`
- **chore(product,framework)**: 移除布隆过滤器三件套（`BloomFilter`/`RedisBitmapBloomFilter`/`BloomFilterConfig`），缓存穿透统一由 `MultiLevelCache` 内置负缓存（`NullValue` 哨兵 30s）承担
- **chore(common)**: 移除 `easyorange-common/pom.xml` 冗余 `jackson-databind` 依赖（由各 web 模块经 Spring Boot 传递引入），仅保留 `jackson-annotations`
- **test**: 后端测试 1,377 → **1,350**（@Test 注解 1,306 → 1,277，测试文件 156 → 153）。`mvn clean test` 11 模块全绿（58s）。前端 1,042 不变，总计 2,419 → **2,392**
- **docs(neat-freak)**: 重写 `easyorange-message/AGENTS.md` 目录树——修正 `entity/`（已迁 `domain/aggregate/`）、`dto/`（已迁 `adapter/inbound/web/dto/request/` + `application/query/dto/`）两大过期段，补 `RecallMessageCommand` / `MessageRecalledEvent` / `domain/port/` / `typehandler/` / `ChatWebSocketHandler` 等 12 处遗漏，删幽灵 `UnreadCountQuery.java`；演进路线第 2/5 步标记完成
- **docs(neat-freak)**: 同步测试数字单一来源 `doc/工程指标.md`（v1.6→v1.7，1,377/1,306/156/2,419 → 1,350/1,277/153/2,392）+ 二级引用（根 `AGENTS.md` / `README.md` / `doc/技术债务清单.md` / `doc/面试-简历使用指南.md`）
- **docs(neat-freak)**: 补齐上一轮遗漏的「7 件套→8 件套」同步——`CLAUDE.md` / `README.md` / `PRODUCT_DIRECTION.md` / `doc/集成/AI-资产管理.md` / `doc/技术债务清单.md` / `doc/面试-简历使用指南.md` 全部对齐权威定义（Port/Adapter + 多级缓存 + 令牌桶 + stale 降级 + AiMetrics + Prompt YAML + TokenBudget + Bulkhead）；`doc/工程指标.md` Port 接口数 31→32（实测 `grep` 验证）


### 2026-07-26 — 测试基础设施现代化（PIT 变异测试 + JaCoCo 修复 + 守卫测试）

- **test(infra)**: 集成 **PIT 1.25.8** 变异测试（pitest-maven + pitest-junit5-plugin 1.2.2），放 `-Ppit` profile 按需启用。仅对 domain 层注入变异（聚合根状态机/领域服务/值对象），评估测试对缺陷的真实检测能力——行覆盖率的"金标准"补充。order 模块基线：mutation 70% / test strength 89% / line 81%（81 变异，41 秒）。阈值门禁默认 0 不阻断，CI 用 `-Dpit.mutationThreshold=60 -Dpit.testStrengthThreshold=75 -Dpit.coverageThreshold=70` 启用（对齐 JaCoCo `haltOnFailure` 约定）
- **fix(build)**: 修复 pom.xml 重复 `maven-surefire-plugin` 配置——第一个有完整 WSL2 add-opens 但缺 `@{argLine}`，第二个有 `@{argLine}` 但丢了 add-opens。合并为单一配置：`@{argLine}`（JaCoCo agent）+ WSL2 add-opens + `-XX:+EnableDynamicAgentLoading`
- **fix(test)**: 修复 `RedisConfigTest` 编译错误（`containsSame` → `isSameAs`，`getConnectionFactory()` 返回非 Optional）。P2 新增的 3 个守卫测试文件此前未编译通过，未计入测试数
- **chore(deps)**: JaCoCo 0.8.12 → 0.8.14（Java 25 class file 69 兼容）
- **test**: 后端测试 1,199 → **1,218**（P2 守卫测试修复后编译通过：RedisConfigTest 3 + CodeEnumTypeHandlerTest 6 + FavoriteTest 10）。注解 1,137 → **1,152**，测试文件 134 → **137**。总测试 2,151 → **2,170**
- **docs**: 同步数字单一来源 `doc/工程指标.md`（§1.2 测试规模表 + §2.1 填入实测覆盖率：domain 69.9% line / 53.1% branch，全模块聚合 43.7% / 34.9%）；根 AGENTS.md / easyorange-backend/AGENTS.md / README.md / doc/技术债务清单.md / project_memory.md 全量同步 1,218/2,170/1,152/137 + PIT 说明

### 2026-07-26 — order/payment 模块现代化对齐 + neat-freak 文档同步

- **refactor(order,payment)**: 按product/user模块现代化最佳实践对齐 order 与 payment 模块。9 项落地：①Bean Validation（@NotBlank/@NotNull/@Positive）②sealed Command 接口（`OrderCommand` / `PaymentCommand` permits 各 7/4 个 record）③ListQuery record 收敛查询参数（`OrderListQuery` / `PaymentListQuery`，含默认分页）④查询端口入参类型安全化（`String status` → `OrderStatus` / `PaymentStatus` 枚举）⑤Spec record 收敛聚合根工厂/重建长参数（`OrderCreateSpec` / `OrderReconstructSpec` / `PaymentCreateSpec` / `PaymentReconstructSpec`）⑥`PaymentTransition<E>` record 统一状态转换结果（消除 10 个冗余 Result record）⑦不可变聚合根（字段 final，状态转换返回新实例）⑧枚举 code 语义化（`PaymentStatus` `0`→`PENDING`、`PaymentMethod` `1`→`WECHAT`，DB 列从 TINYINT 改 VARCHAR(20)）⑨Saga 抽到 `application/saga/` 包（对标 order 的 `CreateOrderSaga + support/`）
- **test**: 后端测试 1,220 → 1,199（现代化重构后用例数微调，全绿）。@Test 注解 1,153 → 1,137。测试文件数 134 不变
- **docs(neat-freak)**: 数字单一来源 `doc/工程指标.md` v1.2 → v1.3，最近核对 2026-07-22 → 2026-07-26；同步 README.md / AGENTS.md（根）/ codemap.md / doc/技术债务清单.md 中所有 1,220→1,199 / 2,172→2,151 引用
- **docs(neat-freak)**: 重写 `easyorange-backend/easyorange-payment/AGENTS.md` 目录结构——修复 `PaymentFactory.java`（已删）、`PaymentAmount.java`（不存在）、`PaymentCommandAssembler.java`（实为 `PaymentCommandMapper` + `PaymentViewAssembler`）、`PaymentConverter.java`（实为 `PaymentDataMapper`）、`PaymentMetricsListener.java`（实为 `PaymentMetricsConsumer`）、`PayPreparedResult`/`PayConfirmedResult`（被 `PaymentTransition<E>` 取代）等 6 处过期引用；补 `PaymentCommand` sealed 接口 / `PaymentListQuery` / `PaymentCreateSpec` / `PaymentReconstructSpec` / `PaymentTransition<E>`
- **docs(neat-freak)**: 修复 `easyorange-backend/easyorange-order/AGENTS.md`——`OrderQuery.java` → `OrderListQuery.java`、删除 `Money.java`（在 common 模块）、修正 `mq/subscriber/` 列表（实际只有 `OrderSagaEventConsumer.java`，非 4 个分散 Subscriber）、移除 `adapter/outbound/messaging/` 段（跨模块适配器实际在 `easyorange-application/adapter/outbound/`）、补 `OrderCommand` sealed 接口 / `OrderCreateSpec` / `OrderReconstructSpec`

### 2026-07-15 — 定位差异化重审：从「企业级 Java 架构实战」到「LLM × DDD 工程化实战」

- **refactor(brand)**: 项目定位从「企业级 Java 架构实战项目」演化为「**LLM × DDD 工程化实战项目**」。差异化锚点：DDD/Saga/CQRS/事件驱动是 10 份简历 9 份的标准话术，单纯堆砌清单无记忆点。改为「核心矛盾 + 解法」叙事——DDD 铁律（domain 层零框架依赖）vs LLM 调用昂贵且不稳定，Port/Adapter + 装饰器模式解了这个矛盾
- **refactor(brand)**: 三层定位拓展落地——标题层（`LLM × DDD：Java 架构工程化实战`）/ 副标层（`在 DDD 六边形里装 LLM：可换供应商、可降级、可观测的 AI 工程化落地`）/ 30 秒钩子（核心矛盾 + 解法 + 三指标收尾）
- **refactor(brand)**: 差异化策略三件套——①从「我用了 X」→「我为什么用 X + 我拒绝了 Y」（ADR 驱动）②从「我会 X」→「我守住了 X」（ArchUnit 守卫、编译期隔离）③AI 工程化作唯一故事锚点（不是 7 件套清单，是「核心矛盾 + 解法」故事）
- **refactor(brand)**: 业务叙事口径从「把复杂度留给架构与工程」升级为「把复杂度留给架构与 AI 工程化」，强调 AI 工程化是核心叙事而非附属
- **refactor(docs)**: 全量同步新定位——README.md / CLAUDE.md / AGENTS.md（根）/ PRODUCT_DIRECTION.md / easyorange-frontend/README.md / easyorange-frontend/index.html / codemap.md / easyorange-frontend/codemap.md / doc/adr/0001 / doc/adr/0002 / doc/adr/0003 / doc/工程指标.md / HeroSection.tsx + test / Footer.tsx + test
- **test**: HeroSection 副标与 Footer tagline 同步为 `LLM × DDD · 业务聚焦核心流程，把复杂度留给架构与 AI 工程化`；Footer copyright 同步为 `© 2025-2026 EasyOrange — LLM × DDD 工程化实战项目`
- **refactor(brand)**: README 新增「4 个核心架构模式」inline code 徽章行（`DDD 六边形` · `CQRS` · `Saga` · `事件驱动`），用现代化最简洁形式恢复 4 模式可见性，与 11 模块表 / Saga 时序图 / 三个并列钩子 / 页脚形成 5 处分布、无重复
- **refactor(docs)**: neat-freak 清理——删除 `doc/前端分页重构设计.md`（94 行）+ `doc/前端分页重构实施计划.md`（988 行），共 1,082 行 2025-06-25 brainstorming 输出未执行规划。TypeScript 代码已自然统一为 `PageResult<T>`（grep 20 处 `PageData` 全部是测试文件函数名/变量名，类型已是 `PageResult<>`），规划已自然完成
- **refactor(docs)**: CLAUDE.md L162 旧叙事修正——「AI 仅在两端做辅助 + AI 是项目展示的一部分」→「AI 在两端走生产级工程实践 + 6 决策点全走 7 件套」

### 2026-07-14 — 项目定位重审 + 文档口径全面统一

- **refactor(brand)**: 项目定位从「LLM 时代企业级 Java 应用工程化样板」重审为「**企业级 Java 架构实战项目**」。叙事口径统一为「**业务聚焦核心流程（C2C 资产流转：固定价格 + 直发 + 平台不碰货），把复杂度留给架构与工程**」，替换旧叙事「业务载体刻意简化，承载复杂度才是重点」（自我削弱措辞已废）
- **refactor(brand)**: 目标受众与场景明确化——简历项目 + 面试现场（HR/面试官）+ 多岗位通用（Java 后端 / 高级架构 / AI 应用 / 全栈）。三套并列钩子：①架构落地（DDD/CQRS/Saga/事件驱动）②架构决策记录（4 ADR）③AI 工程化（7 件套）
- **refactor(docs)**: 以 `doc/工程指标.md` 为数字单一权威来源（single source of truth），其他文档（README / PRODUCT_DIRECTION / CLAUDE.md / 前端 README 等）一律引用，禁止独立陈述数字。前端实测 952（951 通过 + 1 失败），总数 1,269 + 952 = 2,221（旧口径 2,214 已废）
- **refactor(docs)**: 文档职责边界重划分——README 项目门面 / PRODUCT_DIRECTION 只管业务场景 / CLAUDE.md 只管 AI Agent 行为准则 / AGENTS.md 是后端编码指南 / doc/工程指标.md 是数字单一来源 / doc/adr/ 不变
- **refactor(docs)**: PRODUCT_DIRECTION.md 整体重写（176 → 113 行），删除越界章节（技术架构 / 讲解框架 / 对外宣传语 / 核心关键词），AI 角色定位从「辅助决策 / 架构展示为主」升级为「生产级工程实践」，5 件套补全为 7 件套
- **refactor(docs)**: 关键词统一替换——`AI 多模态` → `AI 工程化`；`工程化样板` / `全栈样板` / `全栈架构参考` → `企业级 Java 架构实战项目`；`业务载体刻意简化` / `业务不是主角` / `业务不是重点` → `业务聚焦核心流程`；`12 Maven 模块` → `11 Maven 模块`；`架构展示为主` / `辅助决策` → `生产级工程实践`；`限流降级` → `令牌桶限流 + stale 降级`
- **refactor(docs)**: README / CLAUDE.md / AGENTS.md（根 + 后端）/ easyorange-frontend/README.md / doc/工程指标.md / doc/集成/AI-资产管理.md / doc/adr/0003-ai-port-adapter-with-decorator.md 全部同步新口径
- **test**: 数字锚点更新——11 模块 / 7 对 Port/Adapter / 9 RabbitMQ 消费者 + DLQ / 6 AI 决策点 / 30 表 / 2,221 测试 / 4 ADR

### 2026-07-13 — LLM 时代工程化样板定位升级 + AI 工程化 7 件套闭环

- **refactor(brand)**: 项目定位从"架构参考项目"升级为"LLM 时代企业级 Java 应用工程化样板"。叙事口径统一为"业务载体刻意简化，承载复杂度才是重点"，替换旧叙事"业务不是重点，工程才是核心"
- **feat(ai)**: AI 工程化 7 件套闭环 — Port/Adapter 隔离 + L1/L2 多级缓存 + 令牌桶限流 + stale 降级 + AiMetrics 可观测 + Prompt 版本化（YAML）+ Token 预算治理（@TokenBudget AOP）
- **feat(ai)**: 新增 Prompt 版本管理模块（`ai/prompt/`）— `PromptTemplate` record + `PromptRegistry` 接口 + `YamlPromptRegistry`（启动时加载 `classpath:prompts/*.yml`）+ `PromptRenderer`（`{var}` 替换 + `quoteReplacement` 安全）。15 测试通过
- **feat(ai)**: 新增 Token 预算治理模块（`ai/budget/`）— `@TokenBudget` 注解 + `TokenBudgetAspect` AOP 切面（`@Around("@annotation(tokenBudget)")`）+ `InMemoryTokenBudgetStore`（`ConcurrentHashMap` + `AtomicReference`）+ `TokenBudgetExceededException`。11 测试通过
- **docs**: 新增 ADR 模块（`doc/adr/`）— MADR 中文风格，含 0000 模板 + 3 条决策记录（Saga over 2PC / CQRS scope / AI Port/Adapter + 装饰器）
- **docs**: 新增 `doc/工程指标.md`（185 行）— 28 Port 接口 / 9 RabbitListener / 830 测试方法 / 6 AI 决策点 等硬数字 + 待实测采集方案 + 面试话术
- **docs**: 新增 `doc/技术债务清单.md`（191 行）— 13 条债务（8 接受 + 5 待评估 + 0 必须还）
- **docs**: README.md 嵌入 3 张 Mermaid 图（架构总览 + AI 调用流程时序图 + Saga 补偿时序图），L3-L7 升级 30 秒钩子为"LLM 工程化集成进企业级 Java 应用全栈样板"
- **docs**: PRODUCT_DIRECTION.md 6 处升级（顶部引述 / 项目定位 / 演讲故事线 / 对外宣传语 / 核心关键词加 `AI 工程化` `AiMetrics` `可观测` `stale 降级` `令牌桶限流`）
- **docs**: CLAUDE.md / easyorange-ai/AGENTS.md / doc/集成/AI-资产管理.md 同步补 AI 工程化 7 件套描述
- **test**: AI 模块新增 26 测试（Prompt 版本 15 + Token 预算 11），总计 2,214 测试用例（1,269 后端 + 945 前端）

### 2026-07-05 — AI 工程化可观测性现代化

- **refactor(observability)**: 用 Spring Boot 4 内置 `StructuredLogEncoder` 替换 `logstash-logback-encoder` 第三方依赖，prod profile JSON 日志零外部依赖
- **build**: 移除 GraalVM `native-maven-plugin`（MyBatis-Plus 运行时反射不兼容 native-image，AI 项目瓶颈是 LLM 秒级延迟非 JVM 启动时间）
- **feat(observability)**: 暴露 `/actuator/health/rabbit` RabbitMQ 健康检查端点
- **feat(ai)**: 新增 `AiMetricsService` — AI 链路可观测性指标服务，4 类 Micrometer 指标暴露到 `/actuator/prometheus`:
  - `easyorange.ai.cache` (Counter, tags: scope+outcome=hit/miss) — 多级缓存命中率（CachingLlmAdapter + CachingVisionAdapter）
  - `easyorange.ai.llm.duration` (Timer, tags: scope+outcome=success/error) — LLM 调用延迟（DeepSeekLlmAdapter）
  - `easyorange.ai.vision.duration` (Timer, tags: outcome=success/error) — Vision 调用延迟（QwenVlVisionAdapter）
  - `easyorange.ai.ratelimit` (Counter, tags: scope+outcome=rejected/stale_served/fail_open) — 限流拒绝/降级/放行（AiRateLimitInterceptor）
- **test**: 后端 AI 模块新增 9 测试用例（AiMetricsServiceTest），总计 113 AI 模块测试全部通过

### 2026-07-04 — 项目定位升级 + 旧文案残留全面清理

- **refactor(brand)**: 项目定位从"大模型应用工程化全栈实践平台"升级为"让大模型在真实业务中稳定运行的全栈工程实践"。核心理念从拉踩式"别人调 API,我做成生产级服务"改为"调通 API 只是起点。缓存、限流、降级、可观测——让大模型在真实业务约束下稳定运行,才是 AI 工程化的核心命题"
- **refactor(brand)**: "AI 资产管理"品牌名全量替换为"AI 工程化"（前端 UI + 后端 LLM prompt + 文档共 15 处）
- **refactor(naming)**: "智能定价"统一改为"智能估值"（前端 UI + 后端 prompt + 文档共 12 处）
- **refactor(docs)**: `AGENTS.md` / `CLAUDE.md` / `README.md` / `PRODUCT_DIRECTION.md` / `index.html` 顶部定位全部同步更新，新增"工程亮点"行（多级缓存降本 · 令牌桶限流 · 降级兜底 · RabbitMQ 事件路由 + DLQ）
- **refactor(docs)**: 旧标语"业务是容器,架构才是主角"→"业务做减法,工程做加法";"架构才是主角"→"工程深度才是主角"
- **fix(a11y)**: `HeroSection` / `Footer` / `AIFeaturesSection` 装饰性 SVG 添加 `aria-hidden="true"`
- **test**: 前端 26 测试全部通过

### 2026-07-01 — 修复首页分类商品计数为 0 + 显示格式

- **fix(product)**: 分类接口 `GET /api/products/categories` 一级分类 `productCount` 全为 0 的问题。根因：商品挂在二级分类，计数 SQL 只查 `category_id IN (一级分类ID)`。修复：新增 `countProductsByCategoryIdsWithChildren` SQL（LEFT JOIN `eo_category` + `COALESCE` 将子分类商品归到父分类），Service 层一级分类走新方法
- **fix(frontend)**: `CategoriesSection` 空分类显示 "暂无商品 件商品" 语法错误。修复：移除硬编码 "件商品" 后缀，集成到 count 字符串中

### 2026-07-01 — 四阶段现代化路线图（虚拟线程/ProblemDetail/Biome/可观测性/容器化）

- **feat(infra)**: 启用 Virtual Threads（`spring.threads.virtual.enabled=true`），dev profile 关闭防止调试冲突
- **refactor(api)**: GlobalExceptionHandler 返回 RFC 9457 ProblemDetail 标准错误格式，替代自定义 Result 包装
- **chore(build)**: 新增 OpenRewrite Maven 插件，支持自动化 Spring Boot / Java 版本迁移
- **refactor(frontend)**: Biome 统一 lint + format，替代 ESLint + Prettier；删除 `eslint.config.js` + `.prettierrc`；全项目 Biome 格式化
- **feat(observability)**: JSON 结构化日志（prod 用 StructuredLogEncoder），Micrometer Tracing (Brave) 集成 + Actuator 端点增强
- **feat(infra)**: 新增 `compose.yaml` 开发环境 Docker Compose（MySQL 8.4 + Redis 7.4 + RabbitMQ 3.13 + 健康检查），Spring Boot 4 `@ServiceConnection` 自动配置
- **feat(build)**: 新增 GraalVM native-maven-plugin（已于 2026-07-05 移除，MyBatis-Plus 反射不兼容）
- **fix(frontend)**: 修正 Biome 自动将 `Promise<void[]>` 误改为 `Promise<undefined[]>` 的回归

### 2026-07-01 — Nginx 现代化安全响应头整合

- **feat(infra)**: 新增 `security-headers.conf` 统一管理安全响应头 — CSP (`default-src 'self'` + `style-src 'unsafe-inline'` + `img-src data: blob:`)、X-Content-Type-Options、X-Frame-Options、Referrer-Policy、Permissions-Policy；删除已废弃的 X-XSS-Protection；所有 location 块通过 `include` 继承，消除不一致
- **refactor(infra)**: nginx.conf 安全头从重复散落改为 `security-headers.conf` 片段统一 include；HSTS 仅保留于 HTTPS server block；Dockerfile 同步复制新文件

### 2026-06-30 — 移除前端深色模式切换代码

- **refactor(frontend)**: 移除 `ProfilePreferences` 中的主题外观卡片（浅色/深色/自动切换 UI）及关联 CSS
- **cleanup**: 清理 `tokens.css` 中残留的 `--glass-bg-dark` 变量；更新 `profile/codemap.md` 描述

### 2026-06-30 — 前端组件库收尾、可访问性与代码整洁度修复

- **refactor(frontend)**: `shadcn-button.tsx` 重命名为标准 `button.tsx`，全项目导入统一为 `@/components/ui/button`；`Button` 组件支持 `isLoading` / `loadingText`，修复 `asChild` 子元素渲染异常
- **refactor(frontend)**: 完成剩余原生表单控件迁移 — `EditProductPage` / `PublishPage` 类别与成色改为 shadcn `<Select>`；`CategoryManagePage` 排序输入改为 `<Input>`、状态切换改为 `<RadioGroup>`；`FavoritesPage` 复选框改为 `<Checkbox>`
- **fix(frontend)**: Checkbox 边框可见性修复 — 默认边框从 `border-border` 改为 `--border-control` token（`rgba(42,37,32,0.35)`），解决登录/注册/协议勾选框几乎不可见的问题
- **fix(frontend)**: 修复 `ProductDetailDrawer` / `ProductDetailPage` 中重复的 `@/components/ui` 导入；`InputProps` / `TextareaProps` 由空接口改为 type alias，消除 `no-empty-object-type` 报错
- **fix(a11y)**: 为 `CategoryManagePage` 和 `SearchPage` 中可点击的自定义选项/标签添加 `role="button"`、`tabIndex` 与 `onKeyDown` 键盘支持，满足 jsx-a11y 规范
- **chore(frontend)**: 全量运行 `eslint --fix`，自动修复 39 个 curly 风格错误；手动清理 3 处 `console.error` 调试残留，生产代码不再输出 console
- **test**: 前端 `typecheck` + `lint:check`（0 errors，75 warnings）+ `test`（100 测试文件 / 953 用例）+ `build` 全部通过

### 2026-06-29 — 前端 shadcn/ui 组件化迁移与 CSS 清理

- **refactor(frontend)**: 全面迁移到 shadcn/ui 组件库 — 原生 `<button>` → `<Button>`、`<input>` → `<Input>`、`<textarea>` → `<Textarea>`、`<select>` → 保留原生（测试兼容性）、checkbox → `<Checkbox>`、switch → `<Switch>`、label → `<Label>`
- **refactor(frontend)**: 清理大量废弃 CSS — 从 `src/styles/main.css` 移除约 200 行未使用规则（`.glass-input`、`.auth-btn`、`.form-input`、`.checkbox-custom`、`.switch-slider` 等）；从 `src/admin/styles/admin.css` 移除约 220 行（`.admin-btn`、`.admin-card`、`.admin-table`、`.admin-input`、`.admin-select`、`.admin-badge` 等）；删除未引用的 `src/admin/styles/admin-pages.css`
- **fix(css)**: 修复 `global.css` 中未分层的基础 reset 规则（`* { padding: 0 }`、`button { border: none }` 等）覆盖 Tailwind utilities 的问题 — 将所有 reset 规则移入 `@layer base`，解决登录/注册界面输入框 padding 失效和 checkbox 边框不可见问题
- **fix(frontend)**: `FilterSidebar` 分类/条件筛选原生 checkbox 替换为 shadcn `<Checkbox>`；`ProfilePreferences` 开关替换为 shadcn `<Switch>`；`ChatInputBar` / `PublishPage` textarea 替换为 shadcn `<Textarea>`；管理后台表单控件全部统一为 shadcn 组件
- **test**: 前端 typecheck + 100 测试文件 / 953 用例全部通过 + build 通过

### 2026-06-25 — 副标敲定:"砍业务,撑架构"

- **refactor(brand)**: 副标从"业务是容器,架构才是主角"精炼为 6 字对仗版 "**砍业务,撑架构**"。长版副标"业务做减法,架构做加法"作为展开说明保留
- **refactor(docs)**: `README.md` / `CLAUDE.md` / `AGENTS.md` / `PRODUCT_DIRECTION.md` / `index.html` 顶层副标全部统一为"砍业务,撑架构",主标题统一改为"EasyOrange — 砍业务,撑架构"
- **docs(presentation)**: 比赛答辩 / 面试推荐开场 hook —— "别人堆功能,我压架构",引出"为什么砍 + 砍了什么 + 换来什么"5 分钟故事线

### 2026-06-25 — 项目定位重塑:技术 demo 项目

- **refactor(brand)**: 项目定位从"AI 资产管理产品"重塑为"DDD + CQRS + Saga + 事件驱动 + AI 多模态 全栈架构 demo"。业务场景（C2C 资产流转）从主角调整为"业务容器"，架构与工程成为主角
- **refactor(docs)**: `README.md` 重写为技术 demo 叙事（技术亮点 + 11 Maven 模块 + 业务简化原则 + 业务是容器、架构才是主角）
- **refactor(docs)**: `PRODUCT_DIRECTION.md` 改名为《业务场景说明》，明确说明业务简化原则 + 业务不是商业计划书
- **refactor(docs)**: `doc/集成/AI-资产管理.md` 改名为《AI 能力清单 — 详细机制》，聚焦 AI 架构侧关注点（端口抽象、缓存装饰、限流降级）
- **refactor(docs)**: `CLAUDE.md` / `AGENTS.md` 顶部项目说明同步更新；AGENTS.md 中"AI 资产管理 工作流"小节改名为"AI 能力清单"并补充 AI 架构侧关注点
- **refactor(frontend)**: `easyorange-frontend/index.html` meta description / keywords / title 更新为"DDD + CQRS + Saga + 事件驱动 + AI 多模态 全栈架构 demo"

### 2026-06-25 — 议价 / 阶梯降价功能下线

- **refactor(product)**: 移除 `OfferRuleEngine` / `OfferAppService` / `OfferResult` / `OfferDecision` / `ConsignmentMode` 枚举 / 议价领域事件 (`PriceAdjustedEvent` / `OfferAcceptedEvent` / `OfferRejectedEvent` / `CounterOfferMadeEvent`)；`Product` 聚合根移除 `floorPrice` / `consignmentMode` / `listedAt` / `currentPriceLevel` 字段及对应调价方法
- **refactor(product)**: 移除 `NegotiationMessagePort` / `OrderCreationPort`（AI 自动成单 port）跨模块接口
- **refactor(product)**: 移除 `ProductPriceAdjustTask` 阶梯降价定时任务与对应 Bean 注册
- **refactor(message)**: 移除 `OfferMessageType` 枚举 / `OfferProcessingPort` / `@MessageMapping("/offer.make")` WebSocket 端点
- **refactor(application)**: 移除 `AiOrderCreationAdapter` / `OfferProcessingAdapter` 跨模块编排适配器
- **refactor(ai)**: 移除 `DeepSeekNegotiationMessageAdapter` 议价话术 LLM 实现
- **refactor(frontend)**: 移除出价弹窗 / 还价弹窗 / 议价 UI / `useOfferSocket` 实时推送 hook
- **refactor(sql)**: Flyway 迁移 `V4__add_ai_consignment_fields.sql` 改写为 DROP COLUMN 移除 `floor_price` / `consignment_mode` / `listed_at` / `current_price_level` 字段及 `idx_product_ai_managed` 索引
- **refactor(docs)**: `AGENTS.md` / `CLAUDE.md` / `README.md` / `DATABASE.md` / `PRODUCT_DIRECTION.md` / `doc/集成/AI-资产管理.md` / `doc/集成/API-速查.md` 同步移除议价 / 阶梯降价 / 寄售模式相关描述
- **fix(index)**: `easyorange-frontend/index.html` meta / title 移除"AI 议价"关键词
- **fix(sql)**: `R__seed_message_templates.sql` / `R__insert_dev_test_data.sql` 欢迎语移除"议价"相关文案
- **test**: 后端 11 模块全量回归 + 前端 tsc + vitest

### 2026-06-25 — AI 资产管理定位回滚（好物→资产）

- **refactor(brand)**: 品牌定位回滚 —— "AI 好物管家" → "**AI 资产管理**"(EasyOrange — AI 资产管理 · 让每一份资产,运转不息)
- **refactor(naming)**: 文案回滚 —— 好物 → 资产 / 管家 → AI 资产管家 / 买家 → 认领方 / 卖家 → 资产方
- **refactor(frontend)**: `HeroSection` 主标题 "让每一份价值,物尽其用" → "让每一份价值,运转不息";副标改回 "AI 资产管理 · 把资产交给我们,管家替你估值、写描述、自动调价"
- **refactor(frontend)**: `Footer` / `LoginPage` / `HomePage` / `AIFeaturesSection` / `ProductDetailPage` 同步资产方/认领方术语
- **refactor(backend)**: 后端 Java 实体 / 领域事件 / Port 接口 / Service / 测试 —— 卖家→资产方、买家→认领方、好物→资产
- **refactor(sql)**: Flyway 迁移脚本 `R__seed_categories.sql` / `R__seed_message_templates.sql` / `V1__init_schema.sql` / `R__insert_dev_test_data.sql` 全部回滚到资产方/认领方术语
- **refactor(docs)**: `doc/集成/AI-替卖家运营.md` → `AI-资产管理.md`;`PRODUCT_DIRECTION.md` / `CLAUDE.md` / `DATABASE.md` / `AGENTS.md` 同步
- **refactor(admin)**: 管理后台订单/商品 `AdminOrder` 字段文案 —— 买家/卖家 → 认领方/资产方(全平台术语统一)
- **test**: 前端测试 + 后端测试 全部同步更新

### 2026-06-25 — AI 好物管家定位重塑

- **refactor(brand)**: 全平台品牌重塑 — "AI 替卖家运营" → "**AI 好物管家**"(EasyOrange — AI 好物管家 · 让每一份价值,物尽其用)
- **refactor(naming)**: 文案统一替换表 —— 资产 → 好物 / 议价 → 管家 / 智能助手 → AI 好物管家
- **refactor(frontend)**: `AIFeaturesSection` 完全重写 —— 从"4 张静态卡片"改为"双列管道 + 管家日报"(`StewardDailyReport` + `PipelineStepRow`)
- **refactor(frontend)**: `HeroSection` 主标题 —— "让闲置流转,让价值延续" → "让每一份价值,物尽其用";副标 "设一个底价,AI 替你议价、改价、撮合" → "AI 好物管家 · 设一个底价,管家替你估值、写描述、自动调价"
- **refactor(frontend)**: `Footer` / `LoginPage` / `HomePage` 同步新定位
- **refactor(docs)**: `PRODUCT_DIRECTION.md` v1.0 → v2.0 —— 4 决策点扩为 6 决策点(双端对称),新增"AI 信用画像" + "AI 智能找货" + "AI 物品评估"
- **refactor(docs)**: `README.md` 顶部"AI 替卖家运营的 C2C 平台" → "AI 好物管家 · 让每一份价值,物尽其用"
- **test**: `HeroSection.test.tsx` + `Footer.test.tsx` 文案同步

### 2026-06-24 — 知识库整理（neat-freak）

- **docs**: 新建 `doc/集成/` 目录，含 `AI-替卖家运营.md`（4 决策点/议价规则/阶梯降价/WebSocket 协议）和 `API-速查.md`（全模块 REST+WebSocket 端点）
- **docs**: AGENTS.md（315→261 行）— 详细机制迁出，改为"核心约定 + 深入文档指针"
- **docs**: AGENTS.md 新增"集成文档"指针表
- **docs**: README.md — 删除过期引用（`easyorange-shared/` `easyorange-miniprogram/` 这两个不存在的目录），更新项目结构图（含 `doc/集成/` + `PRODUCT_DIRECTION.md` + `codemap.md`）
- **docs**: CLAUDE.md — 项目结构区加上"详细目录见 codemap/AGENTS"指针

### 2026-06-24 — AI 替卖家运营 + 品牌重塑

- **feat(product)**: AI 替卖家运营功能全量上线 — 规则引擎议价 `OfferRuleEngine` + LLM 话术 `DeepSeekNegotiationMessageAdapter` + 阶梯降价 `ProductPriceAdjustTask` + 议价事件消费者 `OfferEventConsumer`
- **feat(websocket)**: 议价 WebSocket 协议扩展（MessageType 6-9: OFFER/OFFER_ACCEPTED/OFFER_REJECTED/COUNTER_OFFER）
- **feat(frontend)**: 前端完整议价交互（AI 托管开关 + 出价弹窗 + 还价弹窗 + 阶梯降价指示条 + useOfferSocket 实时推送）
- **feat(application)**: 跨模块编排适配器（AiOrderCreationAdapter + OfferProcessingAdapter）
- **refactor(product)**: NegotiationMessagePort 从 ai 模块移至 product 模块解决循环依赖
- **refactor(brand)**: 全平台品牌重塑 — "资产流转平台"→"EasyOrange — AI 替卖家运营的 C2C 平台"（32 文件）
- **test**: 后端新增 ~146 测试用例（总计 1,269），前端 945 测试用例全部通过

### 2026-06-23 — 错误码体系精简与一致性优化

- **refactor(common)**: `IResultCode.mapToHttpStatus()` 静态方法移至 `GlobalExceptionHandler`（framework），`BaseBusinessException.httpStatus()` 方法删除，common 模块不再依赖 `org.springframework.http.HttpStatus`
- **refactor(common)**: `FileException` 构造器从 `public` 改为 `protected`，统一使用 `FileException.of(...)` 工厂方法（与 `BusinessException` 模式对齐）
- **refactor(common)**: `BizRequire` 新增 `notBlank`/`notEmpty` 的 `IResultCode` 重载（此前仅 `notNull`/`requireTrue` 支持）
- **refactor(framework)**: `GlobalExceptionHandler` 所有异常处理器统一返回 `ResponseEntity` + 正确 HTTP 状态码（校验类错误此前返回 HTTP 200，现统一返回 400）
- **refactor(framework)**: `extractDuplicateFieldMessage` 移除硬编码 DB 约束名（`uk_eo_user_email` 等），改为通用兜底消息
- **refactor(product)**: 6 个异常类改用 `ProductResultCode` 专属错误码（此前回退到全局 `B0002`），新增 `REPORT_NOT_FOUND(B2007)`/`REPORT_ERROR(B2008)`/`PRODUCT_STATUS_INVALID(B2009)`
- **refactor(message)**: `MessageDomainException.defaultCode()` 改用 `MessageResultCode.MESSAGE_DOMAIN_ERROR(B7008)`（此前回退到全局 `B0002`）

### 2026-06-22 — 通知系统精简与实时推送

- **fix(message)**: `MessageCommandHandler` 在线用户系统消息现在通过 `WebSocketNotifier.sendNotification()` 实时推送（此前仅存储不推送，前端依赖 30s 轮询）
- **fix(product)**: 修复 `notifyLowStock` 发送给 `receiverId=null` 的 bug，现在正确传递 `sellerId`
- **refactor(notification)**: 删除未使用的 `NotificationService` 接口 + `DefaultNotificationServiceImpl` 日志桩（common/framework 模块）
- **refactor(order)**: `OrderNotificationEventConsumer` 从 order 模块迁移到 application 模块，改用 `MessageCommandHandler` 发送站内消息（与其他 3 个通知消费者统一路径）
- **refactor(message)**: 删除死代码 `WebSocketMessageHandler`（`/chat.sendMessage`、`/chat.addUser` 无调用方，实际路径走 `ChatWebSocketHandler`）
- **feat(frontend)**: 新增 `useNotificationSocket` STOMP hook 订阅 `/user/queue/notification`，`NotificationBell` 轮询降为 60s 兜底

### 2026-06-04 — 全局权限注解清理

- **refactor**: 移除 46 个冗余 `@PreAuthorize("isAuthenticated()")` 注解（9 个 Controller）
- **chore**: 清理 8 个未使用的 `PreAuthorize` 导入
- **docs**: 补充全局认证约定到 AGENTS.md / CLAUDE.md

### 2026-06-02 — 启动日志清理与配置修复

- **fix**: JWT 开发密钥不再触发弱密钥警告（`dev-secret` → `dev-key`）
- **fix**: Flyway 升级至 11.15.0，消除 MySQL 8.4 版本警告
- **fix**: 关闭 dev 环境的 out-of-order 模式（个人开发无需多分支合并）
- **chore**: 关闭 MyBatis-Plus ASCII banner
- **chore**: 降低 Spring Boot devtools/actuator/web-context 等内部日志级别，精简启动输出

### 2026-06-01 — 文档与配置整理

- **docs**: 精简 README (604→121 行)，创建独立 CHANGELOG
- **docs**: 同步项目启动时间 2025-11 到所有文档
- **chore**: 更新 .gitignore 最佳实践（78 行精简版）
- **chore**: 补充 AI 文件忽略规则（codemap, .slim, .superpowers）

### 2026-06-01 — DDD 架构整合

- **feat(event)**: 实现 8 个领域事件消费者（PaymentInitiation, ProductAudit, ReportProcessed, StockReservation, OrderCreated, PaymentEvent, ProductEvent, WebSocket）
- **feat(user)**: 增强认证模块（PasswordManagementService, 密码重置, 个人资料）
- **feat(payment)**: 支付视图组装器 + PaymentMethodInfo 值对象
- **feat(query)**: Assembler 模式 + CQRS 查询 DTO（FavoriteAssembler, CategoryAssembler）
- **feat(framework)**: RabbitMQ 消息基础设施（DomainEventPublisher, 序列化工具）
- **refactor**: 全模块迁移到 Assembler 模式，清理废弃 DTO
- **docs**: 架构文档同步更新

### 2026-05-30 ~ 2026-05-29 — 架构重构冲刺

- **refactor**: 全面 DDD 架构重构，Auth 服务整合，Assembler 模式迁移
- **fix**: Flyway placeholder 解析失败修复，domain service Bean 注册问题

### 2026-05-25 ~ 2026-05-28 — 功能增强

- **feat(order)**: 订单行项支持 + Saga 重构
- **feat(framework)**: 基础设施组件 + POM 注解处理器配置
- **feat(message)**: 模板缓存加载/清理/重置方法
- **test(ai)**: AI 组件测试套件（定价、问答、信用评分）
- **refactor(admin)**: 控制器迁移到 DDD adapter 模式，DTO 转为 record

### 2026-05-17 ~ 2026-05-24 — ES 搜索 & 文件存储

- **feat(product)**: Elasticsearch 集成（分面搜索、ProductSearchQueryPort、ES fallback）
- **feat(application)**: ES 索引管理器、索引适配器、ProductDocument、reindex 服务
- **feat(docker)**: ES 容器 + IK 中文分词插件
- **feat(frontend)**: FacetFilter 分面筛选组件、搜索类型和 Hook
- **feat(upload)**: FileStorage 接口 + LocalFileStorage 实现
- **feat(image)**: 图片处理服务（质量注入、渐进式 JPEG、智能裁剪）

### 2026-05-15 ~ 2026-05-16 — 测试体系 & 管理后台图表

- **test(frontend)**: 完整测试套件（98 文件 / 947 用例）— Vitest + Testing Library + MSW + Playwright E2E
- **feat(admin)**: Dashboard 图表升级（Recharts 趋势图、活动热力图、TopProductsChart）
- **feat(chat)**: STOMP 聊天 Hooks（消息撤回、离线队列）
- **feat(notification)**: 通知中心功能
- **feat(review)**: 管理端审核页面

### 2026-05-09 ~ 2026-05-12 — 管理后台 & 审核系统

- **feat(admin)**: 完整管理面板（暖橙指挥中心设计系统）
- **feat(product)**: 商品审核日志系统（事件驱动持久化）
- **refactor**: 消息/收藏模块迁移到 adapter/application 架构
- **style(frontend)**: UI 组件和动画增强
- **feat(db)**: 索引优化迁移 V4

### 2026-04-27 ~ 2026-05-08 — 核心功能开发期

- **feat(backend)**: Saga 模式、缓存服务、事件基础设施
- **feat(frontend)**: React + Vite + Tailwind 迁移完成
- **feat(frontend)**: 首页内容丰富化、UI 全面升级
- **refactor**: 登录策略模式重构、目录结构重组（Phase 1-4）
- **refactor(backend)**: 核心架构重构、用户认证优化、常量统一

### 2026-04-20 ~ 2026-04-26 — 项目初始化

- **chore**: 初始提交
- **refactor**: 前端目录结构重构
- **feat(backend)**: 后端架构重构和功能优化
- **feat(frontend)**: 前端页面优化和功能完善

---

## 版本说明

| 版本 | 日期 | 说明 |
|------|------|------|
| v0.1.0 | 2025-11 | 项目启动，DDD 模块化架构，React 19 SPA 重构 |

---

> 此文件由 Git 提交记录生成。查看完整历史：`git log --oneline`
