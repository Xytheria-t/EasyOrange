# AI 能力清单 — 详细机制

> 本文档面向工程师：从**架构落地**角度，介绍 EasyOrange 项目中所有 AI 能力的端口抽象、缓存装饰、限流降级、调用流程。AI 部分的核心价值在于 Port/Adapter 六边形架构 + 多级缓存 + 限流降级的工程化深度，而非 AI 本身的商业价值。
>
> 平台在 AI 侧的边界：资产方按固定价格上架，平台不参与议价、不自动调价、不持有底价。AI 在两端走生产级工程实践（Port/Adapter + 多级缓存 + 限流降级 + AiMetrics + Prompt 版本化 + Token 预算），不替人做交易决定。
>
> 顶层规则/红线在 [AGENTS.md](../../AGENTS.md)，本文件是**深入文档**。

---

## 一、定位

EasyOrange 在 AI 工程上的**架构侧关注点**（8 件套）：

- 端口-适配器（`LlmPort` / `VisionPort` / `EmbeddingPort`）隔离业务与具体 AI 供应商
- `@Primary` 装饰器（`CachingLlmAdapter` / `CachingVisionAdapter`）实现 L1 + L2 多级缓存
- 限流拦截器（`AiRateLimitInterceptor`）+ 异常降级（Redis 不可用时 fail-open）
- AiMetrics 可观测（4 类 Micrometer 指标：缓存命中率 / LLM 延迟 / Vision 延迟 / 限流计数 → `/actuator/prometheus`）
- Prompt 版本化（`ai/prompt/` — `YamlPromptRegistry` 启动时加载 `classpath:prompts/*.yml` + `PromptRenderer` `{var}` 渲染 + `quoteReplacement` 安全）
- Token 预算治理（`ai/budget/` — `@TokenBudget` 注解 + `TokenBudgetAspect` AOP 切面 + `InMemoryTokenBudgetStore` 日预算控制，超限抛 `TokenBudgetExceededException`）
- Bulkhead 隔离舱（Resilience4j 并发隔离 — `aiLlm` 8 / `aiVision` 4 / `dbHeavy` 16，AI 与 DB 调用互不挤占）
- 路由键自动派生（`ProductCreatedEvent` → `product.created`）
- 业务侧：**资产方按固定价格上架资产，平台不参与议价、不自动调价、不持有底价**

> **平台边界**：平台不碰货、不囤货、不经手资金。物流走资产方→认领方 C2C 直发。

---

## 二、六个 AI 决策点（双端对称）

| 决策点 | 触发时机 | 实现 | 架构侧价值 |
|--------|---------|------|----------|
| 1. 智能估值 | 资产方提交资产 | `AiPricingService`（ai 模块） | LLM 端口 + 缓存 + 限流 |
| 2. AI 营销文案 | 上架前 | `AiCopyGenerationService` | 4 风格文案生成 + 缓存键分桶 |
| 3. AI 信用画像（资产方） | 认领方浏览时 | `CreditScoringService` | 5 维雷达图 + 规则引擎 |
| 4. AI 智能找货 | 认领方搜索时 | `SemanticSearchService` + `AiSearchEnhancer` | ES 聚合 + LLM 增强 + 缓存 |
| 5. AI 物品评估 | 认领方看货时 | `AutoListingService`（拍照识别） | Vision 端口 + 多模态 |
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

前端 STOMP 客户端：`useStompChat`（`easyorange-frontend/src/components/hooks/chat/`）。

---

## 六、C2C 直发（轻平台边界）

> 平台**不碰货、不囤货、不经手资金**。物流走资产方→认领方 C2C 直发。
> 业务简化原则：业务场景的简化是为了让架构与工程本身成为主角。

---

**相关模块**：`easyorange-product` / `easyorange-ai` / `easyorange-order` / `easyorange-message` / `easyorange-application`

**相关文档**：
- 顶层规则 [AGENTS.md](../../AGENTS.md)
- 业务场景说明 [PRODUCT_DIRECTION.md](../../PRODUCT_DIRECTION.md)
- 架构 [doc/架构/架构-系统架构.md](../架构/架构-系统架构.md)
- API 速查 [API-速查.md](./API-速查.md)
