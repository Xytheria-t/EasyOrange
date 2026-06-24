# AI 替卖家运营 — 详细机制

> 本文档面向工程师：详细介绍"AI 替卖家运营"工作流的业务规则、状态机、消息协议。
> 顶层规则/红线在 [AGENTS.md](../../AGENTS.md)，本文件是**深入文档**。

---

## 一、模式定义

`ConsignmentMode` 枚举（`easyorange-product` 模块）：

| 枚举 | code | 含义 |
|------|------|------|
| `MANUAL` | 0 | 卖家自营：卖家自己定价、自己议价 |
| `AI_MANAGED` | 1 | AI 替卖家运营：设底价后 AI 全自动议价 / 改价 / 撮合 |

业务规则：`AI_MANAGED` 模式**必须**设置 `floorPrice`（底价），否则提交校验失败。

## 二、四个 AI 决策点

| 决策 | 触发时机 | 实现 | 副作用 |
|------|---------|------|--------|
| 1. 智能定价 | 卖家提交资产 | `AiPricingService`（ai 模块） | 给出建议价 + 底价 |
| 2. AI 营销文案 | 上架前 | `AiCopyGenerationService` | 4 风格文案生成 |
| 3. AI 实时议价 | 买家出价 | `OfferRuleEngine` + `DeepSeekNegotiationMessageAdapter` | 接受/还价/拒绝 |
| 4. AI 阶梯降价 | 每天凌晨 2 点 | `ProductPriceAdjustTask` | 自动改 `current_price_level` |

## 三、AI 议价规则引擎

`OfferRuleEngine`（`easyorange-product` 模块）四档决策：

| 买家出价 | 决策 | 副作用 |
|---------|------|--------|
| `offer ≥ floorPrice` | `ACCEPT` | 自动创建订单 |
| `offer ≥ floorPrice × 0.9` | `COUNTER`（还价到 `floorPrice × 0.95`） | 等待买家再次出价 |
| `offer < floorPrice × 0.9` 且当前已到底价阶梯 | `REJECT` | 出价被拒 |
| `offer < floorPrice × 0.9` 但 `offer ≥ floorPrice × 0.85` | `LAST_CHANCE` | 最后一次还价机会 |

## 四、阶梯降价

`ProductPriceAdjustTask` 每天凌晨 2 点执行，按 `listed_at` 计算已上架天数：

| 上架天数 | 价格处理 |
|---------|---------|
| Day 1-3 | 持价（不调整） |
| Day 4-5 | 价格下调 5% |
| Day 6 | 价格下调 10% |
| Day 7+ | 价格降至 `floorPrice`（底价） |

降价通过 `current_price_level` 字段记录状态。

## 五、订单闭环

```
买家出价 → OfferRuleEngine.ACCEPT
       → OrderCreationPort 创建订单（PENDING_PAYMENT）
       → 30 分钟超时（OrderTimeoutTask）
       → 超时未付款 → 自动 CANCELLED → 商品重新可售
       → 付款成功 → SHIPPED → 卖家发货 → COMPLETED
```

## 六、议价 WebSocket 协议

`@MessageMapping("/offer.make")` 处理买家出价：

| 消息类型 | code | 方向 | 说明 |
|---------|------|------|------|
| OFFER | 6 | 客户端→服务端 | 买家发起出价 |
| OFFER_ACCEPTED | 7 | 服务端→客户端 | AI 接受出价 |
| OFFER_REJECTED | 8 | 服务端→客户端 | AI 拒绝出价 |
| COUNTER_OFFER | 9 | 服务端→客户端 | AI 发起还价 |

前端 STOMP 客户端：`useOfferSocket`（`easyorange-frontend/src/hooks/`）。

## 七、C2C 直发（轻平台边界）

> 平台**不碰货、不囤货、不经手资金**。物流走卖家→买家 C2C 直发。
> 平台收入来源：成交抽成 + 议价溢价分成（详见 [PRODUCT_DIRECTION.md](../../PRODUCT_DIRECTION.md) 第六章）。

---

**相关模块**：`easyorange-product` / `easyorange-ai` / `easyorange-order` / `easyorange-message` / `easyorange-application`

**相关文档**：
- 顶层规则 [AGENTS.md](../../AGENTS.md)
- 产品定位 [PRODUCT_DIRECTION.md](../../PRODUCT_DIRECTION.md)
- 架构 [doc/架构/架构-系统架构.md](../架构/架构-系统架构.md)
- API 速查 [API-速查.md](./API-速查.md)
