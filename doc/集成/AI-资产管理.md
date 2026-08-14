# AI 能力清单 — 详细机制

> 本文档面向工程师：从**架构落地**角度，介绍 EasyOrange 项目中所有 AI 能力的模型 Bean、限流降级、调用流程。AI 部分的核心价值在于 Spring AI 2.0 框架化 + 限流降级 + Token 预算的工程化深度，而非 AI 本身的商业价值。
>
> 平台在 AI 侧的边界：资产方按固定价格上架，平台不参与议价、不自动调价、不持有底价。AI 在两端走生产级工程实践（Spring AI 2.0 + 令牌桶限流 + stale 降级 + Prompt 版本化 + Token 预算），不替人做交易决定。
>
> 顶层规则/红线在 [AGENTS.md](../../AGENTS.md)，本文件是**深入文档**。

---

## 一、定位

EasyOrange 在 AI 工程上的**架构侧关注点**（8 件套）：

- Spring AI 2.0 模型 Bean（`AiModelConfig` — `chatModel` @Primary DeepSeek / `visionChatModel` Qwen-VL / `embeddingModel` DashScope，统一 `OpenAiSetup.setupSyncClient` OpenAI 兼容线协议，ADR-0008）
- 调用去重（`AiModelSupport` — `callText` / `callJson` / `embed` / `analyzeImages`）
- 限流拦截器（`AiRateLimitInterceptor`）+ 异常降级（Redis 不可用时 fail-open + stale 缓存降级）
- 可观测性（Spring AI 2.0 内置 Observation + Micrometer → `/actuator/prometheus`，原 `AiMetricsService` 已删除）
- Prompt 版本化（`ai/prompt/` — `YamlPromptRegistry` 启动时加载 `classpath:prompts/*.yml`，模板即 system prompt，业务变量由服务内联 `String.format` 填充）
- Token 预算治理（`ai/budget/` — `@TokenBudget` 注解 + `TokenBudgetAspect` AOP 切面 + `InMemoryTokenBudgetStore` 日预算控制，超限抛 `TokenBudgetExceededException`）
- Embedding 真实现（查询侧 kNN + 索引侧 `nameEmbedding` 写入，dimensions=1024 与 ES `dense_vector` 映射对齐）
- 路由键自动派生（`ProductCreatedEvent` → `product.created`）
- 业务侧：**资产方按固定价格上架资产，平台不参与议价、不自动调价、不持有底价**

> **平台边界**：平台不碰货、不囤货、不经手资金。物流走资产方→认领方 C2C 直发。

---

## 二、六个 AI 决策点（双端对称）

| 决策点 | 触发时机 | 实现 | 架构侧价值 |
|--------|---------|------|----------|
| 1. 智能估值 | 资产方提交资产 | `AiPricingService`（ai 模块） | ChatModel + 限流 + Token 预算 |
| 2. AI 营销文案 | 上架前 | `AiCopyGenerationService` | 4 风格文案生成 |
| 3. AI 信用画像（资产方） | 认领方浏览时 | `CreditScoringService` | 5 维雷达图 + 规则引擎 |
| 4. AI 智能找货 | 认领方搜索时 | `SemanticSearchService` + `AiSearchEnhancer` | ES kNN + LLM 增强 + 缓存 |
| 5. AI 物品评估 | 认领方看货时 | `AutoListingService`（拍照识别） | VisionChatModel 多模态 |
| 6. AI 信用画像（认领方） | 认领方下单时 | `CreditScoringService` | 5 维雷达图 |

---

## 三、资产方固定价格工作流

资产方按固定价格发布资产，平台不参与议价：

```
拍图 → 选分类 → AI 营销文案(可选) → 提交 → 平台审核 → 上架 → 等待认领方下单
```

- 资产方在发布表单中输入 `price`（售价）
- `AiPricingService` 提供 `suggestedPrice` / `minPrice` / `maxPrice` 作为参考（不强制使用）
- 上架后价格由资产方在编辑资产时手动调整，平台不做自动调价

---

## 四、订单闭环

```
认领方下单 → 创建订单(PENDING_PAYMENT)
       → 30 分钟超时(OrderTimeoutTask)
       → 超时未付款 → 自动 CANCELLED → 商品重新可售
       → 付款成功 → SHIPPED → 资产方发货 → COMPLETED
```

---

## 五、WebSocket 实时通信协议

`@MessageMapping("/chat.send")` 处理认领方与资产方的实时聊天：

| 消息类型 | code | 方向 | 说明 |
|---------|------|------|------|
| CHAT | 1 | 双向 | 一对一私聊消息 |
| TYPING | 2 | 双向 | 正在输入提示 |
| RECALL | 3 | Client→Server | 撤回消息 |
| READ | 4 | Client→Server | 已读回执 |

> 注：议价 WebSocket 端点 `/offer.make`（`OfferMessageType` 枚举 / `OfferProcessingPort`）已于 2026-06-25 下线。

前端 STOMP 客户端：`useStompChat`（`easyorange-frontend/src/hooks/chat/`）。

---

## 六、C2C 直发（轻平台边界）

> 平台**不碰货、不囤货、不经手资金**。物流走资产方→认领方 C2C 直发。
> 业务简化原则：业务场景的简化是为了让架构与工程本身成为主角。


---

## 七、AI 对话与 RAG 完整链路（2026-08-14 新增）

> 面试口径：从「轻量 RAG（标题级）」演进到「RAG 完整链路 + 评估闭环 + 流式对话 + 反馈飞轮」，
> 对应 01 §五 盲区标注里 RAG 分块/评测集/流式/Agent 记忆四条「绕法」→「实答」。

### 7.1 知识库摄入管线（解析 → 分块 → embed → ES 索引）

- 表：`eo_knowledge_doc`（标题/正文/来源/索引状态 PENDING|INDEXED|FAILED/分块数）+ 种子文档 `R__seed_knowledge_docs.sql`（kb-0001~kb-0005 平台规则）
- 分块：`KnowledgeIngestionService.chunkContent` — 固定 chunk size 500 + overlap 50，切点优先落换行（不切断句子）
- Embedding：text-embedding-v3（1024 维），单块 embed 失败降级 null 照常写入（best-effort）
- 索引：ES `knowledge_docs` 索引（dense_vector 1024 + IK 分词，`knowledge-mapping.json`）
- 补索引：`KnowledgeBootstrapIndexer` 启动时重试 PENDING 文档（保持文档 ID 稳定，金标准集引用同一批 ID）
- 管理端：`/api/admin/knowledge`（新增即摄入 / 列表 / 删除 / 补索引）

### 7.2 混合召回 + Cosine 重排（引用溯源）

`KnowledgeRetrievalService`：查询向量化 → ES kNN（num_candidates=100）+ BM25（title^2/content）混合召回 → **Java 原生 Cosine 重排收口** → 返回带 docId/title 的命中；ES 关闭时降级 MySQL LIKE（`KnowledgeFallbackAdapter`）。聊天回答末尾用 `[来源:标题]` 标注（`AiChatService`）。

### 7.3 AI 对话（多轮 Agent + SSE 流式）

- 编排（单步 ReAct，`AiChatService`）：记忆装配（Redis 会话窗口 + 用户画像表）→ 工具决策（LLM 输出 JSON 决定是否检索知识库，顺带提取用户偏好）→ 执行工具 → 生成回答
- 记忆：短期 = Redis List（`eo:chat:session:{sessionId}`，TTL 24h，最近 N 轮）；长期 = `eo_user_preference` 用户画像表（跨会话持久，聊天时注入 prompt）
- 流式：`POST /api/ai/chat/stream` → SseEmitter，事件协议 token / sources / done / error；前端 fetch + ReadableStream 消费（可带 Authorization 头）
- 预算：流式方法在流结束前返回，`@TokenBudget` AOP 拦不住 → `AiChatService` 手动执行同一套预算检查（超限 onError 降级）

### 7.4 评估进 CI（金标准集 + Judge 回归 + 门禁）

- 金标准集：`eval/golden-set.yaml` 30 条用例（20 chat 生成质量 + 10 检索质量），`eval/baselines.yaml` 基线（chat: 4.0）
- 生成质量：`GoldenSetEvaluator.evaluateGeneration` — 对每条用例调真实对话 → `AiJudge` 对照参考打分（`judgeAgainstReference`）→ 聚合平均分
- 检索质量：`evaluateRetrieval` — hit@5 / MRR，逐条落 `eo_retrieval_metric`（按 run_id 聚合）
- 门禁：`EvalGate` 低于「基线 - 0.3」即失败；`GoldenSetRegressionIT`（failsafe，`EASYORANGE_AI_API_KEY` 存在时执行）卡 CI
- 定时：`AiEvalScheduler`（生成 Judge，3 点）+ `RetrievalEvalScheduler`（检索指标，3:15，默认关闭）

### 7.5 反馈飞轮（👍/👎 → 自动扩充评测集）

- 入库：`POST /api/ai/feedback` → `eo_ai_feedback`（scope/问题/回答/helpful/评语/关联调用日志）
- 导出：`GET /api/admin/ai/feedback/export` → 未导出反馈渲染为 golden-set.yaml 用例片段（导出即标记 exported=1），人工审核后合入评测集

### 7.6 成本优化（语义缓存 + 模型路由）

- 语义缓存：`SemanticCacheService` — 查询向量化 → Redis Hash 内 Cosine 相似度匹配（阈值 0.92）→ 相似问题复用历史回答；条目超上限淘汰最旧；Redis/embedding 不可用 fail-open
- 模型路由：`AiModelRouter` — 场景 → 模型 bean 名（`easyorange.ai.routing.scenarios`），接入第二个模型只需改配置

### 7.7 可观测（AI dashboard）

`infra/grafana/provisioning/dashboards/ai-overview.json`：LLM 调用延迟 p50/p95（spring_ai 直方图）、/api/ai/* QPS 与 429 限流率、AI 调用量（eo_ai_call_log 小时聚合）、**Judge 均分趋势（日）**、RAG 检索指标（最近 10 次回归 hit@5/MRR）——新增 MySQL 数据源（`datasources.yml`）。


---

**相关模块**：`easyorange-product` / `easyorange-ai` / `easyorange-order` / `easyorange-message` / `easyorange-application`

**相关文档**：
- 顶层规则 [AGENTS.md](../../AGENTS.md)
- 业务场景说明 [PRODUCT_DIRECTION.md](../PRODUCT_DIRECTION.md)
- 架构 [doc/架构/架构-系统架构.md](../架构/架构-系统架构.md)
- API 速查 [API-速查.md](./API-速查.md)
