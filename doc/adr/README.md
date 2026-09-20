# ADR 索引 — 架构决策记录

> 编号 0001 起为决策记录（含「已替代历史」篇），0000 为模板；篇数与最新篇见[结构计数](../工程指标.md#结构计数)。**推荐阅读顺序**：先读「现役决策」（按主题：ID 与数据建模 → 事务一致性 → 模块边界 → 消息选型 → CQRS → AI → DDD 治理），再读「演进方案」（不实施，触发条件未到），最后按需翻阅「已替代历史」（决策反转链）。

## 现役决策（按主题阅读顺序）

| 编号 | 主题 | 状态 | 关键结论 | 日期 |
|------|------|------|---------|------|
| [0011](0011-uuid-v7-primary-key.md) | ID 与数据建模 | 接受 | 全库主键统一 **UUID v7**（应用层生成、DB 不回填），订单号由主键派生；拒绝自增与 Snowflake | 2026-09-16 |
| [0007](0007-order-local-tx-over-saga.md) | 订单一致性 | 接受 | **拒绝 Saga**：单库用「本地单事务 + 分布式锁 + Outbox」，无补偿路径 | 2026-08-02 |
| [0006](0006-module-decoupling-port-adapter-acl.md) | 模块解耦 | 接受 | 跨模块统一 Port/Adapter + Maven `<optional>` 隔离，拒绝共享内核/微服务化 | 2026-08-08 |
| [0005](0005-messaging-rabbitmq.md) | 消息选型 | 接受 | RabbitMQ（Topic + DLQ）作默认总线，拒绝 Kafka/Pulsar/NATS/Redis Streams | 2026-07-30 |
| [0002](0002-cqrs-scope-4-modules.md) | CQRS 范围 | 接受 | 仅 product/order/payment/message 四模块上 CQRS，ArchUnit 守卫边界 | 2026-07-14 |
| [0008](0008-ai-spring-ai-framework.md) | AI 框架化 | 接受 | 全面框架化 Spring AI 2.0，删除自研 Port/Adapter/指标（**Supersedes ADR-0003**） | 2026-08-03 |
| [0012](0012-rag-hybrid-retrieval-rrf.md) | AI 检索 | 接受 | RAG 改两路独立召回 + RRF 排名融合，删除无效余弦重排与向量回传；语料扩到 23 篇恢复 hit@5 判别力 | 2026-09-17 |
| [0004](0004-ai-bulkhead-token-budget.md) | AI 治理 | 部分已替代 | `@TokenBudget` AOP 日预算仍现役；Bulkhead 已删（**部分替代**，见 0008） | 2026-07-26 |
| [0009](0009-domain-service-placement.md) | DDD 治理 | 接受 | 领域服务数量是领域性质产物，禁止按数量对齐模块 | 2026-08-06 |

## 演进方案（Proposed，不实施）

| 编号 | 主题 | 状态 | 要点 | 日期 |
|------|------|------|------|------|
| [0010](0010-order-saga-evolution-plan.md) | 订单一致性（演进） | 提议 | **真 Saga 演进方案**：拆库判据 + 编排 + Outbox 补偿自愈 + 代码骨架，触发条件未到不实施 | 2026-08-14 |

## 已替代历史（决策反转链）

| 编号 | 主题 | 被替代原因 | 替代者 |
|------|------|-----------|--------|
| [0001](0001-order-saga-vs-2pc.md) | Saga vs 2PC | 单库场景下 Saga 编排是过度设计；仅「拒绝 2PC/XA/TCC」结论保留 | [0007](0007-order-local-tx-over-saga.md) |
| [0003](0003-ai-port-adapter-decorator.md) | 自研 AI 抽象 | Spring AI 2.0 GA 后自研基础设施为重复造轮子（STP 原则） | [0008](0008-ai-spring-ai-framework.md) |

## 工具

- [0000-template.md](0000-template.md) — 新 ADR 模板与编写约定（正文即现状、新增后须同步更新本索引）
