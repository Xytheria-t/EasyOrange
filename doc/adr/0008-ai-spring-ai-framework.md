# ADR 0008 — AI 集成全面框架化为 Spring AI 2.0，删除自研 Port/Adapter/装饰器/指标基础设施

- **状态**：接受（Supersedes ADR-0003）
- **日期**：2026-08-03
- **决策者**：后端架构
- **标签**：`ai` `spring-ai` `stp` `framework-first` `openai`

---

## 上下文（Context）

EasyOrange 的 6 个 AI 决策点（智能估值 / AI 营销文案 / AI 信用画像 / AI 智能找货 / AI 物品评估 / 内容审核）自 2025-11 起基于自研基础设施构建，到 2026-07 累计了以下代码：

- **自研 Port/Adapter + 装饰器**：`LlmPort` / `VisionPort` 接口、`DeepSeekLlmAdapter` / `PythonLlmAdapter` / `QwenVlVisionAdapter` 底层实现、`CachingLlmAdapter` / `CachingVisionAdapter` 的 `@Primary` 装饰器（L1 Caffeine + L2 Redis 多级缓存）
- **自研 DTO 与 HTTP 调用**：`DeepSeekRequest` / `DeepSeekResponse` / `QwenVlRequest` / `QwenVlResponse`，手写 RestClient + ObjectMapper JSON 解析，线协议是 OpenAI Chat Completions / Embeddings 兼容格式
- **自研可观测性**：`AiMetricsService`（缓存命中率 / LLM 延迟 / Vision 延迟 / 限流计数），`Resilience4jConfig` 里 `aiLlm` / `aiVision` / `dbHeavy` 三个 Retry + Bulkhead 隔离仓
- **Python 侧车**：`easyorange-python/`（FastAPI + OpenAI 兼容线协议）作为第二个 LLM 供应商，通过 `easyorange.ai.provider=python` 切换

这带来四个持续成本：

1. **重复造轮子**：Spring AI 已经提供 `ChatModel` / `EmbeddingModel` 抽象 + OpenAI 兼容客户端（OpenAI 线协议对 DeepSeek / Qwen-VL compatible-mode / DashScope 全部天然兼容），自研 DTO + RestClient 是完全复刻。
2. **Embedding 是桩实现**：原 `OpenAiEmbeddingResponse` 适配器返回固定空向量（`List.of()`），语义搜索 kNN 从未真正跑通；ES 索引 `dense_vector dims=1024` 映射里 `nameEmbedding` 一直是 null。
3. **跨模块耦合**：`easyorange-python` 侧车 + `@ConditionalOnProperty` 双注册是自建供应商路由，Spring AI 的 options（baseUrl / apiKey / model）天然支持多供应商实例。
4. **STP 原则冲突**：项目规范「标准 API 优先（STP）：零新增自定义代码是最优方案——删掉手写代码，换成框架配置即可」。自研 AI 基础设施恰好是最大的「手写代码」存量。

触发条件：Spring AI 1.0 在 ADR-0003 决策时（2025-11）尚不稳定（ADR-0003 第 84 行明确拒绝）。Spring AI 2.0.0 GA（2026，与 Spring Boot 4.0.x / Java 25 对齐）已发布稳定版本，且其 `OpenAiChatModel` / `OpenAiEmbeddingModel` 支持通过 `OpenAiSetup.setupSyncClient(baseUrl, apiKey, ...)` 指向任意 OpenAI 兼容端点。

## 决策（Decision）

**全面框架化**：删除 AI 模块自研的 Port/Adapter/装饰器/自定义 DTO/自定义指标/Python 侧车，六个业务服务 + 语义搜索 + 搜索增强直接注入 Spring AI 的 `ChatModel` / `EmbeddingModel` bean。

### 1. 模型 Bean（[AiModelConfig.java](../../easyorange-backend/easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/config/AiModelConfig.java)）

三个 bean 统一走 `OpenAiSetup.setupSyncClient`：

| Bean | 端点 | 模型 | 注入处 |
|------|------|------|--------|
| `chatModel`（`@Primary`） | DeepSeek `https://api.deepseek.com` | `deepseek-chat` | 估值 / 文案 / 审核 / 问答 / 搜索增强 |
| `visionChatModel` | DashScope `https://dashscope.aliyuncs.com/compatible-mode/v1` | `qwen-vl-max` | 拍照上架图片识别 |
| `embeddingModel` | DashScope `https://dashscope.aliyuncs.com/compatible-mode/v1` | `text-embedding-v3`（dimensions=1024） | 语义搜索 + ES 索引写入 |

`OpenAiChatAutoConfiguration` 的 `@ConditionalOnMissingBean`（按返回类型推断）会因自定义 bean 存在而安全退让，不产生重复 bean。

### 2. 调用去重（[AiModelSupport.java](../../easyorange-backend/easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/service/AiModelSupport.java)）

`callText` / `callJson`（`response_format=json_object`）/ `embed`（`float[] → List<Float>`）/ `analyzeImages`（多图 Media + `UserMessage.builder`）四个静态工具收敛重复调用模式，**不是**端口/适配器抽象，只是代码去重。

### 3. 删除清单

- 删除 `port/LlmPort`、`port/VisionPort`、`adapter/DeepSeekLlmAdapter`、`PythonLlmAdapter`、`QwenVlVisionAdapter`、`CachingLlmAdapter`、`CachingVisionAdapter`、`adapter/dto/`、`metrics/AiMetricsService`
- 删除 `easyorange-python/` 目录、`compose.yaml` 的 `python-ai` service、`.env.example` 的 `AI_PYTHON_BASE_URL`
- 删除 `Resilience4jConfig` 中 `aiLlm`/`aiVision`/`dbHeavy` Retry + Bulkhead（并发隔离交给 openai-java 客户端内置连接池，`AiModelConfig` 配 `MAX_RETRIES=2`）
- 删除 `AiProperties.provider` / `python` 块、`easyorange.ai.provider` 配置

### 4. 保留清单

- `@TokenBudget` AOP（预算治理与框架无关，保留；已移除 `AiMetricsService` 依赖）
- `AiRateLimitInterceptor`（Redis 令牌桶 + stale 降级，保留；已移除 `AiMetricsService` 依赖）
- `SemanticSearchService` / `AiSearchEnhancerAdapter` 的缓存与降级业务逻辑，仅把 LLM 调用点换成 `ChatModel`

### 5. Embedding 变真实现

- 查询侧：`SemanticSearchService.search()` 用 `embeddingModel.embed(keyword)` 生成查询向量，经 `ProductSearchQueryPort` 传入 `ElasticsearchProductSearchQueryAdapter` 的 kNN 查询（`nameEmbedding` 字段）
- 索引侧：`ElasticsearchProductSearchIndexAdapter.buildDocument()` 注入 `ObjectProvider<EmbeddingModel>`，best-effort 写入 `nameEmbedding`（失败降级 null，不阻塞索引），维度 1024 与 `product-mapping.json` 的 `dense_vector dims=1024` 对齐

## 后果（Consequences）

### 正向后果

- **STP 落地**：删掉全部手写 AI 基础设施，换成 Spring AI 框架 bean；估算净删约 20+ 类（含 5 个测试）
- **Embedding 真正可用**：语义搜索 kNN 从桩实现变为真实向量检索
- **供应商切换更简单**：多供应商 = 多个 options（baseUrl/apiKey/model），不再需要 `@ConditionalOnProperty` 二选一 + Python 侧车
- **可观测性由框架提供**：Spring AI 2.0 通过 `ObservationRegistry` 内置 Chat/Embedding 观测（`spring-ai-autoconfigure-model-chat-observation`），Prometheus 暴露由框架承担
- **与生态对齐**：Spring Boot 4 / Java 25 / Spring AI 2.0 同代，BOM 统一管理版本

### 负向后果

- **自定义指标丢失**：`easyorange.ai.cache.*` / `easyorange.ai.ratelimit.*` 等自定义指标删除；缓存命中率类指标需依赖 Spring AI 内置观测或后续自行补充
- **JSON 结构化输出依赖 OpenAI 协议**：`callJson` 用 `OpenAiChatModel.ResponseFormat`（OpenAI 特有），若切换到非 OpenAI 兼容供应商需另改
- **供应商 Bean 名称约定**：视觉 bean 注入需 `@Qualifier("visionChatModel")`，字段级注解与 Lombok 构造器注入顺序有约定成本
- **Spring AI 2.0 尚新**：依赖框架自身的稳定性与 API 演进节奏（见 #5647 风险）

### 缓解措施

- 指标：AI 模块启动时仍注册 `AiRateLimitInterceptor`（限流）与 `@TokenBudget`（预算），核心治理能力不随自定义指标删除而消失
- JSON 输出：解析与降级逻辑保留在调用方（ObjectMapper + try/catch），换供应商只需改 `AiModelSupport.callJson` 一处
- 并发隔离：openai-java 客户端内置连接池 + `MAX_RETRIES=2`，必要时再按供应商级调优

## 备选方案（Alternatives Considered）

- **保留自研 Port/Adapter，仅换底层实现**：拒绝。那会保留 7 个 adapter 类 + 自研 DTO + 自研缓存装饰器，只是把「手写 RestClient」换成「手写 Spring AI 包装」，与 STP 精神冲突，且多级缓存装饰器与 Spring AI 内置能力重复。
- **用 Spring AI ChatClient 而非裸 ChatModel**：拒绝。`ChatClient` 是更上层的流式/函数调用 DSL，本项目场景是「单轮 system+user + JSON 输出 + 多图」，裸 `ChatModel.call(Prompt)` 测试面最小（mock `ChatResponse`），`AiModelSupport` 已覆盖去重。
- **Embedding 走非托管自建（BGE-M3 本地）**：拒绝。需自建模型服务 + 运维，与「托管 API」的成本模型不符；仅在 DashScope `text-embedding-v3` 不可用（GitHub spring-ai #5647：2.0.0-M2 曾返 404）时作为备选 SiliconFlow BAAI/bge-m3。
- **保留 Python 侧车作为供应商**：拒绝。`easyorange.ai.provider=python` 是自建供应商路由，Spring AI options 已覆盖；删除侧车简化部署与代码面。

## 备注（Notes）

- Supersedes [ADR 0003](./0003-ai-port-adapter-decorator.md)（其第 84 行「用 Spring AI Starter：拒绝」决策翻转）；Related to [ADR 0004](./0004-ai-bulkhead-token-budget.md)（`@TokenBudget` 保留，Bulkhead 隔离仓删除）
- 相关文档：[doc/集成/AI-资产管理.md](../集成/AI-资产管理.md)、根目录 `AGENTS.md`「AI 能力清单」
- 相关代码：[AiModelConfig.java](../../easyorange-backend/easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/config/AiModelConfig.java)、[AiModelSupport.java](../../easyorange-backend/easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/service/AiModelSupport.java)、[ElasticsearchProductSearchIndexAdapter.java](../../easyorange-backend/easyorange-application/src/main/java/com/cartethyia/easyorange/adapter/outbound/elasticsearch/ElasticsearchProductSearchIndexAdapter.java)
- 后续演进触发：Spring AI 新版本升级时评估 API 变更；上线前 curl 验证 DashScope `text-embedding-v3` endpoint 可用性（#5647），失败则切 SiliconFlow BAAI/bge-m3（同为 OpenAI 兼容线协议）
