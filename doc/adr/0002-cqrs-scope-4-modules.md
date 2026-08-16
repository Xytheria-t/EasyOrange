# ADR 0002 — CQRS 仅在 product/order/payment/message 四模块落地

- **状态**：接受
- **日期**：2026-07-14
- **决策者**：后端架构
- **标签**：`cqrs` `architecture-boundary` `archunit`

---

## 上下文（Context）

EasyOrange 后端是 11 个 Maven 模块（见 `README.md` 与 `easyorange-backend/AGENTS.md`）。CQRS 模式在 application/domain 层要求 Command 侧与 Query 侧使用不同的 Repository 接口与数据模型，Controller 层按需组织（如 product 模块合并为一个 `ProductController`，order/payment/message 维持 `*CommandController` + `*QueryController` 分离）。

哪些模块该上 CQRS，需要明确边界。否则会出现：

- 全模块上 CQRS → 复杂度爆炸，简单模块被强行套两层
- 完全不上 CQRS → 重点查询场景（全文搜索、订单列表分页）扩展性差
- 部分模块上但无守卫 → 边界模糊，新人不知道该往哪里加代码

约束：

- 项目是「LLM × DDD 工程化实战项目」，需要展示 CQRS 的真实落地，但**业务聚焦核心流程**（固定价格 + C2C 直发，见 `doc/PRODUCT_DIRECTION.md`），不需要为展示而过度铺开
- 已有 ArchUnit 架构守卫（`ArchitectureRulesTest.java`），可强制 CQRS 边界
- 各模块的读写比差异极大：product 读多写少（搜索 / 详情 / 列表），user 几乎是对称的 CRUD

## 决策（Decision）

**只在 product / order / payment / message 四个模块落地 CQRS**，其余模块（user / favorite / ai / admin / framework / common / application）不上 CQRS。

四模块共性与决策驱动力：

- **product**：读多写少 + ES 全文搜索聚合，命令/查询分离收益明显
- **order**：写链路为本地单事务 + 分布式锁 + Outbox（拒绝 Saga，见 [ADR 0007](0007-order-local-tx-over-saga.md)），查询侧需独立 ReadModel 支撑「我的订单 / 卖出订单」分页
- **payment**：写操作幂等性强（`IdempotencyKeyFilter`），查询侧需独立支付流水视图
- **message**：站内信 + WebSocket 实时消息，读多写多但查询维度独立（会话列表 / 未读数 / 历史消息），与命令（发送 / 撤回 / 已读）天然分离

代码位置（以 message 为例验证边界）：

- Command：[MessageCommandHandler.java](../../easyorange-backend/easyorange-message/src/main/java/com/cartethyia/easyorange/message/application/command/MessageCommandHandler.java)
- Query：[MessageQueryHandler.java](../../easyorange-backend/easyorange-message/src/main/java/com/cartethyia/easyorange/message/application/query/MessageQueryHandler.java)、[ConversationQueryHandler.java](../../easyorange-backend/easyorange-message/src/main/java/com/cartethyia/easyorange/message/application/query/ConversationQueryHandler.java)

边界由 `easyorange-application` 下的 ArchUnit 守卫强制：`*CommandHandler` 禁止依赖 `*QueryHandler`，反之亦然。

为什么不是 3 个或 5 个：

| 候选模块 | 是否纳入 | 理由 |
|---------|---------|------|
| product | 是 | ES 全文搜索 + facets 聚合，读模型独立价值最高 |
| order | 是 | 写链路与查询分离，避免长事务拖累分页查询 |
| payment | 是 | 写操作幂等 + 流水查询独立，CQRS 收益清晰 |
| message | 是 | 会话列表 / 未读数 / 历史消息查询维度独立，与命令天然分离 |
| user | 否 | 读写比均衡，CRUD 即可，强套 CQRS 是空壳 |
| favorite | 否 | 六边形架构已足够，无独立查询维度 |
| ai | 否 | 核心是 Port/Adapter + 装饰器（见 ADR 0003），非读写分离诉求 |

## 后果（Consequences）

### 正向后果

- 复杂度可控：4 个模块承担 CQRS 成本，其余模块保持简单的 `*AppService` 单服务模式
- 重点模块收益明显：product 全文搜索、order 订单列表分页、payment 流水查询、message 会话列表都获得了独立 ReadModel
- 边界由 ArchUnit 守卫，新人加代码时「该往 command 还是 query」一目了然
- order 写链路在 command 侧，查询不影响写事务

### 负向后果

- 4 模块与其余模块的代码风格不一致，新人需理解两种模式
- 同一聚合根的读写模型可能出现字段漂移（写侧加字段，读侧忘加）
- ReadModel 与写库的最终一致需要事件驱动同步，多了一层心智负担

### 缓解措施

- `easyorange-backend/AGENTS.md` 中明确列出 CQRS 模块清单，作为新人 onboarding 必读
- ArchUnit 守卫禁止 Command 依赖 Query，反向亦然
- Assembler 模式统一 DTO 转换（`adapter/inbound/web/assembler/`），减少字段漂移
- 各模块 `AGENTS.md` 描述 CQRS 边界（如 `easyorange-order/AGENTS.md` 的「CQRS 架构」节）

## 备选方案（Alternatives Considered）

- **全模块上 CQRS**：拒绝。user / favorite / ai 等模块读写比均衡或以调用外部 API 为主（ai 模块核心是 Port/Adapter + 装饰器，见 ADR 0003），强行套 CQRS 会引入无意义的 CommandHandler 空壳，违反 KISS / YAGNI。
- **完全不上 CQRS**：拒绝。product 的 ES 全文搜索 + facets 聚合、order 的多维度分页查询，用单一 Service + Repository 难以承载，扩展性差；也无法在「LLM × DDD 工程化实战项目」中讲清楚 CQRS。
- **只在 product 上 CQRS**：拒绝。order 的写链路与查询分离、message 的会话/未读数查询维度独立，都有真实诉求；只做 product 会丢失这些场景的展示价值。
- **未来扩展到 user / favorite**：暂不排除。当 user 模块的「个人主页 / 信用画像」查询维度独立演化、或 favorite 出现大流量收藏列表分页时，再评估是否新增 ADR 扩展。

## 备注（Notes）

- 相关文档：[easyorange-backend/AGENTS.md](../../easyorange-backend/AGENTS.md)「CQRS 模式」节、[README.md](../../README.md)「架构模式落地」表
- 相关代码：[MessageCommandHandler.java](../../easyorange-backend/easyorange-message/src/main/java/com/cartethyia/easyorange/message/application/command/MessageCommandHandler.java)、[MessageQueryHandler.java](../../easyorange-backend/easyorange-message/src/main/java/com/cartethyia/easyorange/message/application/query/MessageQueryHandler.java)
- 相关 ADR：[ADR 0007](./0007-order-local-tx-over-saga.md)（order 写链路：本地单事务 + 分布式锁 + Outbox，拒绝 Saga）、[ADR 0003](./0003-ai-port-adapter-decorator.md)（ai 模块选择 Port/Adapter 而非 CQRS；注：该 ADR 已被 [ADR 0008](./0008-ai-spring-ai-framework.md) 替代，ai 模块现为 Spring AI 框架化，「不上 CQRS」结论不变）
- 重评估触发：user 或 favorite 模块出现独立的复杂查询维度、或 ArchUnit 守卫频繁被绕过时，重新评估 CQRS 范围。
