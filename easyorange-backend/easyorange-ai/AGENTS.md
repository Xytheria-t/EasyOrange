# easyorange-ai 模块指南

AI 能力模块，全面框架化为 **Spring AI 2.0**（ADR-0008，Supersedes ADR-0003）。所有 LLM/Embedding 调用直接注入 Spring AI `ChatModel` / `EmbeddingModel` bean，不再有自研 LlmPort/VisionPort/装饰器层；对外协作者（持久化、缓存、日志、评测）一律经 `domain/port` 端口。

## 目录结构

分层与其余业务模块一致：`domain` → `application` → `adapter`，依赖方向单向向内（ArchUnit `domain_only_depends_on_allowlist` / `domain_and_application_should_not_depend_on_adapter` / `persistence_exceptions_stay_in_adapters` 三条规则覆盖本模块）。

```
ai/
├── domain/                     # 无框架依赖（白名单准入）
│   ├── model/                  # 值类型：KnowledgeChunk/KnowledgeMatch/KnowledgeDocEntity/KnowledgeHit、
│   │                           #   RrfFusion/VectorUtils、
│   │                           #   ChatTurn/ToolDecision/UserPreference、PromptTemplate、
│   │                           #   GoldenSet/GoldenSetCase/GenerationReport/RetrievalReport
│   ├── port/                   # 端口（12 个，见下「分层与端口」）
│   ├── constant/               # AiCallScope（6 场景）/ AiResultCode / KnowledgeDocStatus
│   ├── annotation/             # @TokenBudget（编译期兜底契约，配置可热更新覆盖）
│   └── exception/              # TokenBudgetExceededException（继承 BaseBusinessException）
├── application/
│   ├── service/                # 16 个业务服务（见下）
│   ├── dto/                    # 业务 DTO（14 个）
│   └── eval/                   # EvalGate（门禁判定）/ GoldenSetEvaluator / GoldenSetLoader
├── adapter/
│   ├── inbound/
│   │   ├── job/                # AiEvalScheduler / RetrievalEvalScheduler / KnowledgeBootstrapIndexer
│   │   └── web/                # AiRateLimitInterceptor（Redis 令牌桶，超限 429）
│   └── outbound/
│       ├── AiSearchEnhancerAdapter.java   # 搜索增强管道（4 路并行）
│       ├── budget/             # TokenBudgetAspect（AOP）/ InMemoryTokenBudgetStore
│       ├── cache/              # ChatSessionStore（Redis 会话窗口）/ SemanticCacheService（语义缓存）
│       ├── persistence/        # AiCallLogRecorder / GoldenSetExportService / JdbcCreditScoreFetcher /
│       │                       #   RetrievalMetricRecorder / knowledge/ / preference/
│       ├── prompt/             # YamlPromptRegistry（classpath:prompts/*.yml）
│       └── tool/               # 搜索增强工具集（SearchToolRegistry / IntentDetectionTool 等）
└── config/                     # AiProperties / AiModelConfig / AiConfig / AiCacheConfig /
                                #   AiStaleCacheConfig / UnconfiguredChatModel / UnconfiguredEmbeddingModel
```

Controller 全在 `easyorange-application`（`AiChatController` / `AiCopyController` / `AiQaController` / `AiListingController` / `AiKnowledgeController` / `AiFeedbackController` / `AdminFeedbackExportController` / `AdminKnowledgeController` / `CreditScoreController`），本模块不持有 web 入站适配器。

## 分层与端口

应用层只依赖端口，实现在 `adapter/outbound`（或由别的模块实现）：

| 端口（`domain/port`） | 实现（`adapter/outbound` 或其它模块） |
|---|---|
| `KnowledgeRepository` | `persistence/knowledge/KnowledgeRepositoryImpl` |
| `KnowledgeIndexPort` | **easyorange-application**：`elasticsearch/KnowledgeElasticsearchAdapter`（主）+ `KnowledgeFallbackAdapter`（ES 关闭时降级） |
| `UserPreferenceRepository` | `persistence/preference/UserPreferenceRepositoryImpl` |
| `CreditScoreFetcher` | `persistence/JdbcCreditScoreFetcher` |
| `PromptRegistry` | `prompt/YamlPromptRegistry` |
| `TokenBudgetStore` | `budget/InMemoryTokenBudgetStore` |
| `AiCallLogPort` | `persistence/AiCallLogRecorder`（`eo_ai_call_log`） |
| `ChatSessionPort` | `cache/ChatSessionStore`（Redis List 会话窗口） |
| `SemanticCachePort` | `cache/SemanticCacheService` |
| `GoldenSetExportPort` | `persistence/GoldenSetExportService`（跨模块消费：AdminFeedbackExportController） |
| `RetrievalMetricPort` | `persistence/RetrievalMetricRecorder`（`eo_retrieval_metric`） |
| `ChatStreamHandler` | 由入站方实现（`AiChatController` 的 SSE 匿名类），服务只向回调推事件 |

> 新增持久化/缓存/外部写入时，先在 `domain/port` 定义端口再在 `adapter/outbound` 实现，禁止 application 直接注入 adapter 类（ArchUnit 会拦）。

## 架构决策

- **Spring AI 2.0 全面框架化（ADR-0008）**：自研 `LlmPort` / `VisionPort` / `DeepSeekLlmAdapter` / `PythonLlmAdapter` / `QwenVlVisionAdapter` / `CachingLlmAdapter` / `CachingVisionAdapter` / `AiMetricsService` / `adapter/dto/` 全部删除。六个业务服务 + 语义搜索 + 搜索增强直接注入 `ChatModel` / `EmbeddingModel` bean。决策翻转记录：ADR-0003 曾在 2025-11 拒绝 Spring AI 1.0（不稳定），Spring AI 2.0.0 GA 后迁移
- **模型 Bean（`AiModelConfig`）**：三个 bean 统一走 `OpenAiSetup.setupSyncClient`（OpenAI 兼容线协议）——`chatModel`（`@Primary`，DeepSeek `deepseek-chat`）、`visionChatModel`（Qwen-VL `qwen-vl-max`，注入处用 `@Qualifier("visionChatModel")`）、`embeddingModel`（DashScope `text-embedding-v3`，dimensions=1024 与 ES `dense_vector` 映射对齐）
- **模型路由（`AiModelRouter`）**：场景→bean 名映射在 `easyorange.ai.routing.scenarios`（yaml 可热更新），未配置回退 `routing.default-model`。已接入 `chat_tool` → chatModel、`vision` → visionChatModel、`judge` → chatModel（评审模型独立可换，用于消除自评偏差）；接入新模型只改配置
- **调用收敛（`AiModelSupport`）**：`callText`（system+user 双消息 / 多角色 `List<Message>` 两个重载）、`callJson`（`response_format=json_object`）、`callJsonAs`（调用+反序列化+降级一步到位，返回 `Optional<T>`）、`callTextStream`（逐 token 回调）、`embed`（float[]→List<Float>）、`analyzeImages`（多图 Media），不构成端口/适配器抽象。带 `AiCallScope` 的重载做两类横切记账：`AiCallLogPort` 落 eo_ai_call_log、`TokenBudgetStore` 落**真实 token 用量**（供应商未回报用量时退化为场景上限估算）
- **供应商可换（options 切换）**：改 `AiModelConfig` 的 baseUrl/apiKey/model（或 `application.yaml` 的 `easyorange.ai.*`），无需改业务代码；`easyorange.ai.provider` 字段与 `easyorange-python/` 侧车已删除（2026-08-03）
- **跨模块 Port**：`SemanticSearchService` / `AiSearchEnhancerAdapter` 通过 consumer 模块定义的 port 接口查询（`ProductSearchQueryPort` / `AiSearchEnhancerPort`），本模块作为实现方
- **纯规则零 LLM**：`NaturalLanguageDetector` 和 `ProductTagger` 不调任何 LLM，通过规则引擎 + 数据库查询完成，确保亚毫秒级响应
- **并行容错**：`AiSearchEnhancerAdapter` 内 4 个子步骤使用 `CompletableFuture` 并行执行，任一步骤失败不阻塞其余步骤。整体 5s 总超时（`allOf(...).get(5, SECONDS)`，无单步超时），超时后经 `getNow` 保留已完成步骤的部分结果（规则标签工具刻意不取消）。`supplyAsync` 显式传 `SearchTool.VIRTUAL` 虚拟线程执行器（每任务一个虚拟线程；`spring.threads.virtual.enabled` 管不到 `ForkJoinPool.commonPool()`，秒级 LLM 阻塞不占平台线程），无需自定义线程池。取消操作使用 `cancel(false)`（对 `CompletableFuture` 该参数无效，在飞调用会跑到客户端超时；只避免未开始的任务继续调度）
- **搜索增强的两条硬约束**：①**永不抛异常**（挂在商品检索主链路，调用方无兜底，`tryEnhance` 收敛为 `Optional.empty()`）；②**降级结果不写缓存**（超时/部分失败只服务本次请求），因此工具不得吞异常——吞掉异常返回空值会让管道分不清「正常空结果」与「本次降级」，把抖动固化成 5 分钟缓存
- **Embedding 真实现**：查询侧 `SemanticSearchService` 用 `embeddingModel.embed(keyword)` 生成查询向量经 `ProductSearchQueryPort` 传入 ES kNN；索引侧 `ElasticsearchProductSearchIndexAdapter`（easyorange-application 模块）注入 `ObjectProvider<EmbeddingModel>` best-effort 写入 `nameEmbedding`（失败降级 null，不阻塞索引）
- **RAG 检索（2026-09 调整）**：`KnowledgeElasticsearchAdapter` 做两路独立召回（kNN + BM25）后在实现侧用 `RrfFusion`（RRF，k=60）融合排名，返回 `KnowledgeMatch`（**不回传分块向量**，`_source` 排除 embedding）。此前是「ES 同请求合并 kNN+BM25 + Java Cosine 重排」：余弦对稠密那一路是单调变换（等于没排），却会丢掉 BM25 的排序信号
- **LLM-as-Judge 离线评估**（2026-08-08 新增）：`AiCallLogRecorder` 记录每次 LLM/Embedding 调用到 `eo_ai_call_log`，`AiEvalScheduler` 定时对未评审成功调用打分（1-5 + 评语）；默认关闭（`easyorange.ai.eval.enabled=false`），把 AI 输出质量从「感觉还行」变成「可量化、可回归」
- **限流拦截器**：`AiRateLimitInterceptor` 拦截 `/api/ai/**`，按端点独立令牌桶 (5-30次/分)，超限返回 429（`ResultCode.TOO_MANY_REQUESTS`，Redis 故障 fail-open 放行）；stale 兜底属 LLM 供应商故障（`AiChatService` 服务层 stale-while-error），拦截器不承担缓存职责

## 限流与预算

**限流**（`AiRateLimitInterceptor`，按端点独立令牌桶）：

| 端点 | 限流 (次/分) |
|------|-------------|
| pricing | 10 |
| review | 10 |
| generate-copy | 20 |
| auto-listing | 5 |
| semantic-search | 30 |
| qa | 20 |

**Token 预算**（`@TokenBudget` AOP + `easyorange.ai.budget.scenarios` 配置覆盖）：
- 6 个 service 公开方法标注 `@TokenBudget(scenario, maxTokensPerCall, dailyTokenLimit)`，注解为编译期兜底契约，`application.yaml` 配置可热更新覆盖
- 切面**只做前置检查**（`累计用量 + maxTokensPerCall > dailyTokenLimit` 抛 `TokenBudgetExceededException`）；**记账在 `AiModelSupport`**——那里拿得到 `ChatResponse` 里供应商回报的真实 prompt/completion tokens，切面只有业务 DTO、只能按上限估算（差一个量级）。流式链路不带 `@TokenBudget` 注解（AOP 拦不住流式返回），由 `AiChatService.checkBudget()` 做同一套前置检查，且不得重复记账

**配置**：`application.yaml` → `easyorange.ai.*`

## 新增 AI 能力

1. 在 `application/service/` 实现业务逻辑，直接注入 `ChatModel` / `EmbeddingModel`（选模型走 `AiModelRouter`）
2. 多消息 / JSON / 流式 / 多图 / embedding 调用用 `AiModelSupport` 去重
3. 若为 LLM 生成型场景，标注 `@TokenBudget(scenario, ...)`（值必须与 `AiCallScope.budgetScenario()` 一致，否则记账与检查落在两个场景、预算静默失效）
4. 若为新端点，在 `AiCallScope` 枚举中新增条目并配置 `AiRateLimitInterceptor` 限流值
5. 若需落库 / 缓存 / 外部写入，先在 `domain/port` 定义端口，实现放 `adapter/outbound/`
6. 若为语义搜索相关，把向量写入 ES（`ElasticsearchProductSearchIndexAdapter`）或查询侧生成查询向量

## 搜索增强管道 (AiSearchEnhancer)

```
用户输入自然语言查询 (如 "5000以内适合编程的笔记本")
    ↓
NaturalLanguageDetector.isNaturalLanguage()  → false → 降级为普通搜索
    ↓ (true 且 aiEnhanced=true)
AiSearchEnhancerAdapter
    ├─ Future 1: ChatModel → 需求理解 (intentExplanation)
    ├─ Future 2: ProductTagger → 商品标签 (productTags)
    ├─ Future 3: ChatModel → 市场分析 (marketAnalysis)
    └─ Future 4: ChatModel → 猜你想问 (suggestedQuestions)
    ↓
RedisTemplate (5min TTL, 注入时检查 ObjectProvider: 无 Redis 时不缓存)
    ↓
AiEnhancement DTO → SearchPageResponse.aiEnhancement
```

## 单元测试

32 个测试类与主代码同包镜像（受测类在哪层，测试就在哪层）；共享测试夹具放 `testsupport/`（`PropertyBindings` 配置绑定、`TestPromptRegistry` Prompt 桩、`TestAiModelSupport` 调用工具装配）。

| 测试类 | 覆盖场景 |
|--------|---------|
| `domain/constant/AiCallScopeTest` | URI 映射/TTL/限流配置 |
| `adapter/inbound/web/AiRateLimitInterceptorTest` | 非 AI 路径/限流/fail-open/429/X-Forwarded-For |
| `adapter/inbound/job/AiEvalSchedulerTest` | LLM-as-Judge 巡检/跳过已评审/关闭开关 |
| `adapter/outbound/AiSearchEnhancerTest` | 前置条件/缓存命中/正常流程/容错降级（含降级不写缓存、异常不逃逸） |
| `adapter/outbound/tool/SearchToolRegistryTest` | 工具注册/按名取用 |
| `adapter/outbound/cache/ChatSessionStoreTest` | Redis 不可用降级/会话窗口截断 |
| `adapter/outbound/cache/SemanticCacheServiceTest` | 命中/未命中/相似度阈值/脏条目隔离/base64 向量编解码 |
| `adapter/outbound/persistence/AiCallLogRecorderTest` | 落库字段/异常只告警 |
| `adapter/outbound/persistence/JdbcCreditScoreFetcherTest` | 批量查询/空输入/降级逐个查询 |
| `adapter/outbound/prompt/YamlPromptRegistryTest` | YAML 加载 / 版本路由 / 缺失异常 / 资源解析 |
| `adapter/outbound/prompt/PromptContentTest` | 生产 YAML 内容回归（8 个模板的关键短语与版本号防漂移 + 注入防护声明全覆盖） |
| `adapter/outbound/budget/TokenBudgetAspectTest` | 预算未超通过 / 超限抛 TokenBudgetExceededException / maxPerCall=0 跳过 / dailyLimit=0 不限 |
| `adapter/outbound/budget/InMemoryTokenBudgetStoreTest` | recordUsage 累加 / getTodayUsage 跨日重置 / 并发安全 |
| `application/service/AiChatServiceTest` | Agent 编排（记忆/工具决策/检索/生成）/ 流式 / 预算 |
| `application/service/AiJudgeTest` | Judge 打分边界 / 解析降级 |
| `application/service/AiModelSupportTest` | callText/callJson/embed/analyzeImages 调用收敛 + 真实用量记账 |
| `application/service/AiModelSupportStreamTest` | 流式 token 回调 / 完整文本拼接 |
| `application/service/AiPricingServiceTest` | 正常/降级/JSON 解析失败 |
| `application/service/AiQaServiceTest` | 问答正常/降级 |
| `application/service/AiReviewServiceTest` | 审核正常/降级 |
| `application/service/AiCopyGenerationServiceTest` | 文案生成正常/风格分支/降级/模板缺失 |
| `application/service/AutoListingServiceTest` | 拍照上架正常/视觉降级/文本降级/模板缺失 |
| `application/service/CreditScoringServiceTest` | 等级判定边界值 / 重算 |
| `application/service/KnowledgeServiceTest` | 知识库摄入/检索（融合顺序透传、降级 LIKE） |
| `application/service/NaturalLanguageDetectorTest` | null/空白/长度边界/意图词组合 |
| `application/service/ProductTaggerTest` | 折扣/图片/信用分/综合场景 |
| `application/service/SemanticSearchServiceTest` | 空白/null/端口缺失/空向量/正常 kNN 查询 |
| `application/service/FeedbackLoopTest` | 反馈入库 → 导出金标准用例 |
| `application/eval/GoldenSetEvaluatorTest` | 金标准回归（生成评分/检索指标） |
| `application/eval/GoldenSetLoaderTest` | 金标准集与基线加载 + scope/字段自洽性校验 |
| `domain/model/RrfFusionTest` | 排名融合（双路命中优先、稳定排序、退化输入） |
| `config/UnconfiguredAiModelTest` | AI 未配置时的占位模型行为 |

> 测试统一用 Mockito mock `ChatModel` / `EmbeddingModel`，`textResponse(text)` 构造 `ChatResponse(List.of(new Generation(new AssistantMessage(text))))`；端口协作者 mock 端口接口而非适配器实现。Prompt 匹配用 `argThat`（注意 null-safe，避免 Mockito 对 stubbing 期 null 参数触发 NPE）。
