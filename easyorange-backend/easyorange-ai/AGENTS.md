# easyorange-ai 模块指南

AI 能力模块，全面框架化为 **Spring AI 2.0**（ADR-0008，Supersedes ADR-0003）。所有 LLM/Embedding 调用直接注入 Spring AI `ChatModel` / `EmbeddingModel` bean，不再有自研 Port/Adapter/装饰器层。

## 目录结构

```
ai/
├── config/
│   ├── AiProperties.java           # AI 配置属性 (deepseek/qwenVl/embedding/cache/rateLimit/budget)
│   ├── AiModelConfig.java          # 3 个 Spring AI 模型 bean（chatModel/visionChatModel/embeddingModel）
│   ├── AiConfig.java               # tokenBudgetStore bean
│   ├── AiCacheConfig.java          # AiRateLimitInterceptor 注册（/api/ai/**）
│   └── AiStaleCacheConfig.java     # Stale 缓存 (Caffeine, 24h TTL, 限流降级用)
├── interceptor/
│   └── AiRateLimitInterceptor.java # AI 限流拦截器，Redis 令牌桶 + stale 降级
├── prompt/                         # Prompt 版本管理 (YAML 加载，模板即 system prompt)
│   ├── PromptTemplate.java         # record 值类型 (name/version/template/description)
│   ├── PromptRegistry.java         # 接口 getLatest(name)
│   └── YamlPromptRegistry.java     # @Component，启动时加载 classpath:prompts/*.yml；业务变量由服务内联 String.format 填充（原 PromptRenderer {var} 渲染已移除，2026-08-12）
├── budget/                         # Token 预算治理 (@TokenBudget AOP)
│   ├── TokenBudget.java            # @注解 (scenario / maxPerCall / dailyTokenLimit)
│   ├── TokenBudgetStore.java       # 接口 + 嵌套 record TokenUsage
│   ├── InMemoryTokenBudgetStore.java  # ConcurrentHashMap + AtomicReference 实现（由 AiConfig @Bean 注册，非 @Component）
│   ├── TokenBudgetExceededException.java  # 预算超限异常 (RuntimeException)
│   └── TokenBudgetAspect.java      # @Around("@annotation(tokenBudget)") AOP 切面
├── enums/
│   └── AiCallScope.java            # 6 个枚举：PRICING/REVIEW/QA/COPY/AUTO_LISTING/SEMANTIC
├── service/                        # 业务服务
│   ├── AiModelSupport.java         # 静态调用去重（callText/callJson/embed/analyzeImages）
│   ├── NaturalLanguageDetector.java   # 规则引擎 (intent words + 长度检测)
│   ├── ProductTagger.java             # 商品标签引擎 (折扣/图片/信用分)
│   ├── CreditScoreFetcher.java        # 信用分获取接口
│   ├── JdbcCreditScoreFetcher.java    # 信用分获取实现 (批量 JDBC + 降级)
│   ├── AiPricingService.java          # 智能估值
│   ├── AiReviewService.java           # AI 审核
│   ├── AiQaService.java               # 智能问答
│   ├── AiCopyGenerationService.java   # 智能文案
│   ├── AutoListingService.java        # 拍照上架
│   ├── SemanticSearchService.java     # 语义搜索
│   └── CreditScoringService.java      # 信用评分
├── adapter/
│   ├── inbound/job/AiEvalScheduler.java # LLM-as-Judge 离线评估（默认关闭 easyorange.ai.eval.enabled=false）
│   └── outbound/
│       ├── AiCallLogRecorder.java       # AI 调用日志（eo_ai_call_log，失败仅告警不阻塞主链路）
│       ├── AiSearchEnhancerAdapter.java # AI 导购搜索增强管道 (4 路并行，ForkJoinPool 虚拟线程)
│       └── tool/                        # 搜索增强工具集（SearchToolRegistry / IntentDetectionTool 等）
├── dto/                            # 业务 DTO (AiReviewRequest/Result, CopyGenerationRequest/Result, PricingRequest/Suggestion, AutoListingResult, CreditScoreResult, QaRequest/Response, SemanticSearchQuery/Result)
└── controller/                     # API 接口 (可选, 部分控制器在 easyorange-application)
```

## 架构决策

- **Spring AI 2.0 全面框架化（ADR-0008）**：自研 `LlmPort` / `VisionPort` / `DeepSeekLlmAdapter` / `PythonLlmAdapter` / `QwenVlVisionAdapter` / `CachingLlmAdapter` / `CachingVisionAdapter` / `AiMetricsService` / `adapter/dto/` 全部删除。六个业务服务 + 语义搜索 + 搜索增强直接注入 `ChatModel` / `EmbeddingModel` bean。决策翻转记录：ADR-0003 曾在 2025-11 拒绝 Spring AI 1.0（不稳定），Spring AI 2.0.0 GA 后迁移
- **模型 Bean（`AiModelConfig`）**：三个 bean 统一走 `OpenAiSetup.setupSyncClient`（OpenAI 兼容线协议）——`chatModel`（`@Primary`，DeepSeek `deepseek-chat`）、`visionChatModel`（Qwen-VL `qwen-vl-max`，注入处用 `@Qualifier("visionChatModel")`）、`embeddingModel`（DashScope `text-embedding-v3`，dimensions=1024 与 ES `dense_vector` 映射对齐）
- **调用去重（`AiModelSupport`）**：静态工具收敛四类重复模式，不构成端口/适配器抽象：`callText`（system+user 双消息）、`callJson`（`response_format=json_object`）、`embed`（float[]→List<Float>）、`analyzeImages`（多图 Media + UserMessage.builder）
- **供应商可换（options 切换）**：改 `AiModelConfig` 的 baseUrl/apiKey/model（或 `application.yaml` 的 `easyorange.ai.*`），无需改业务代码；`easyorange.ai.provider` 字段与 `easyorange-python/` 侧车已删除（2026-08-03）
- **跨模块 Port**：`SemanticSearchService` / `AiSearchEnhancerAdapter` 通过 consumer 模块定义的 port 接口查询（`ProductSearchQueryPort` / `AiSearchEnhancerPort`），本模块作为实现方
- **纯规则零 LLM**：`NaturalLanguageDetector` 和 `ProductTagger` 不调任何 LLM，通过规则引擎 + 数据库查询完成，确保亚毫秒级响应
- **并行容错**：`AiSearchEnhancer` 内 4 个子步骤使用 `CompletableFuture` 并行执行，单步骤超时/失败不影响其他步骤。5s 总超时控制。使用 `ForkJoinPool.commonPool()`（Java 21+ 虚拟线程），无需自定义线程池。取消操作使用 `cancel(false)` 避免中断虚拟线程 carrier 线程
- **Embedding 真实现**：查询侧 `SemanticSearchService` 用 `embeddingModel.embed(keyword)` 生成查询向量经 `ProductSearchQueryPort` 传入 ES kNN；索引侧 `ElasticsearchProductSearchIndexAdapter`（easyorange-application 模块）注入 `ObjectProvider<EmbeddingModel>` best-effort 写入 `nameEmbedding`（失败降级 null，不阻塞索引）
- **LLM-as-Judge 离线评估**（2026-08-08 新增）：`AiCallLogRecorder` 记录每次 LLM/Embedding 调用到 `eo_ai_call_log`，`AiEvalScheduler` 定时对未评审成功调用打分（1-5 + 评语）；默认关闭（`easyorange.ai.eval.enabled=false`），把 AI 输出质量从「感觉还行」变成「可量化、可回归」
- **限流拦截器**：`AiRateLimitInterceptor` 拦截 `/api/ai/**`，按端点独立令牌桶 (5-30次/分)，超限时优先返回 stale 缓存

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
- 调用前检查 `累计用量 + maxTokensPerCall > dailyTokenLimit` 抛 `TokenBudgetExceededException`；调用后以 `maxTokensPerCall` 作为预估用量记入 `TokenBudgetStore`

**配置**：`application.yaml` → `easyorange.ai.*`

## 新增 AI 能力

1. 在 `service/` 实现业务逻辑，直接注入 `ChatModel` / `EmbeddingModel`
2. 多消息 / JSON / 多图 / embedding 调用用 `AiModelSupport` 去重
3. 若为 LLM 生成型场景，标注 `@TokenBudget(scenario, ...)`（`AiCallScope` 枚举名小写）
4. 若为新端点，在 `AiCallScope` 枚举中新增条目并配置 `AiRateLimitInterceptor` 限流值
5. 若为语义搜索相关，把向量写入 ES（`ElasticsearchProductSearchIndexAdapter`）或查询侧生成查询向量

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

| 测试类 | 覆盖场景 |
|--------|---------|
| `NaturalLanguageDetectorTest` | null/空白/长度边界/意图词组合 |
| `ProductTaggerTest` | 折扣/图片/信用分/综合场景 |
| `AiSearchEnhancerTest` | 前置条件/缓存命中/正常流程/异常降级 |
| `AiCallScopeTest` | URI 映射/TTL/限流配置 |
| `AiPricingServiceTest` | 正常/降级/JSON 解析失败（ChatModel mock + textResponse helper） |
| `AiQaServiceTest` | 问答正常/降级（ChatModel mock） |
| `AiReviewServiceTest` | 审核正常/降级（ChatModel mock） |
| `AiModelSupportTest` | callText/callJson/embed/analyzeImages 四类调用去重 |
| `AiCopyGenerationServiceTest` | 文案生成正常/风格分支/降级/模板缺失 |
| `AutoListingServiceTest` | 拍照上架正常/视觉降级/文本降级/模板缺失 |
| `JdbcCreditScoreFetcherTest` | 批量查询/空输入/降级逐个查询 |
| `SemanticSearchServiceTest` | 空白/null/端口缺失/空向量/正常 kNN 查询 |
| `AiRateLimitInterceptorTest` | 非 AI 路径/限流/fail-open/429/X-Forwarded-For |
| `YamlPromptRegistryTest` | YAML 加载 / 版本路由 / 缺失异常 / 资源解析 (7 测试) |
| `TokenBudgetAspectTest` | 预算未超通过 / 超限抛 TokenBudgetExceededException / maxPerCall=0 跳过 / dailyLimit=0 不限 |
| `InMemoryTokenBudgetStoreTest` | recordUsage 累加 / getTodayUsage 跨日重置 / 并发安全 |

> 测试统一用 Mockito mock `ChatModel` / `EmbeddingModel`，`textResponse(text)` 构造 `ChatResponse(List.of(new Generation(new AssistantMessage(text))))`。Prompt 匹配用 `argThat`（注意 null-safe，避免 Mockito 对 stubbing 期 null 参数触发 NPE）。
