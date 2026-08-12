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

**相关模块**：`easyorange-product` / `easyorange-ai` / `easyorange-order` / `easyorange-message` / `easyorange-application`

**相关文档**：
- 顶层规则 [AGENTS.md](../../AGENTS.md)
- 业务场景说明 [PRODUCT_DIRECTION.md](../PRODUCT_DIRECTION.md)
- 架构 [doc/架构/架构-系统架构.md](../架构/架构-系统架构.md)
- API 速查 [API-速查.md](./API-速查.md)
