# ADR 0005 — 消息中间件选型采用 RabbitMQ（Topic Exchange + DLQ），拒绝 Kafka / Pulsar / NATS JetStream / Redis Streams 作为默认实现

- **状态**：接受
- **日期**：2026-07-30
- **决策者**：后端架构
- **标签**：`messaging` `event-driven` `rabbitmq` `kafka` `nats` `pulsar` `outbox` `dlq`

> **现状更新（2026-08-07）**：事件消费者从决策时点的 11 收敛为 **10**（2026-08-04 支付 Saga 移除收口时，现役 DLQ 队列 10 个）；DLQ 重试实现收敛为 `DlqRetryScheduler` 内 `x-retry-count` 头驱动（固定 5 分钟扫描周期，退避由主队列 RetryTemplate 承担），`ExponentialBackoffRetryStrategy` 已删除并入该类，消费者统一处理基类 `AbstractDomainEventConsumer` 重构为 `EventConsumerHandler`。本记录保留 2026-07-30 决策时点的 11 消费者口径。

---

## 上下文（Context）

EasyOrange 的事件驱动架构已经落地：
- 1 个 **Topic Exchange** `eo.domain.events`，路由键由事件类名自动派生（`ProductCreatedEvent` → `product.created`）
- **11 个独立事件消费者**（每个模块独占一个队列 `eo.{name}`），完全是 pub/sub 模式：**同一条领域事件被多个下游各消费各的、各 ack 各的、各有各的失败重试链路**
- **DLQ 三级重试链路**：队列级 `x-dead-letter-exchange` → 失败消息自动路由到 `eo.{name}.dlq` → `DlqRetryScheduler` 每 5 分钟扫描 DLQ → 按 `x-retry-count` 指数退避重投（1min/5min/15min，自死信时间起算，手动 ack 不丢消息）→ 超过 `max-retries=3` 的毒消息转储 `eo.dlq.terminal` 等待人工介入
- **Spring Modulith Outbox 模式**：业务表 + `EVENT_PUBLICATION` 表与应用事务同原子写入，崩溃恢复时 Modulith 自动重发未完成事件
- **幂等**：`EventIdempotencyChecker` 基于 Redis SETNX + TTL
- **量级**：C2C 资产流转业务（商品创建/审核、下单/支付、通知、审计日志），预估每天几千到几万条事件，单节点吞吐 ~10k msg/s 绰绰有余
- **定位限制**：本项目是 LLM × DDD 架构工程化实战项目，**中间态可观测性 + 可讲解性 > 极致吞吐**（非真实生产百万级）

已在使用的 RabbitMQ 能力清单：
- Topic Exchange + routing key 路由
- 队列级 DLX + DLQ 绑定（`x-dead-letter-exchange` / `x-dead-letter-routing-key`）
- 手动 ack / nack（`AbstractDomainEventConsumer` 统一处理）
- 消息头注入 traceId（`EventMetadataMessagePostProcessor`），消费者端 decode 回 MDC

强制约束：
- **不能破坏 Spring Modulith Outbox + EventIdempotencyChecker + DlqRetryScheduler 的 3 层可靠性链路**
- **切换成本必须可接受**（不接受花 3-5 天重写 11 消费者 + 重试链路 + 测试）
- **本地启动必须仍然零运维（单 Docker 容器）**，不能引入多容器组件（如 Pulsar 的 BookKeeper）
- `@ConditionalOnProperty(matchIfMissing=true)` 降级能力必须保留（无 MQ 环境可直接起应用走 In-Memory 事件）

## 决策（Decision）

**采用 RabbitMQ 4.x（Topic Exchange + 队列级 DLQ）作为默认消息总线实现；拒绝 Kafka / Pulsar / NATS JetStream / Redis Streams 作为默认实现，但在架构上保留 `MessageBus` Port 抽象，允许后续接入 NATS JetStream 或 Kafka 作为可选 Adapter（`@ConditionalOnProperty` 切换）。**

核心依据（按 EasyOrange 的真实诉求排序）：

1. **DLQ + 重试机制原生匹配度（权重 40%）**：RabbitMQ 的队列级 `x-dead-letter-exchange` 是原生声明式能力，在 `RabbitMQConfig` 里每个队列声明时绑定 2 个参数即完成；Kafka / Redis Streams **没有原生死信概念**，需要在消费端手动实现「失败 → 发去 dlq topic → 调度器重投」全链路，开发与测试成本翻倍；NATS JetStream 有原生 `max_deliver + discard policy=dead` 但声明粒度在 Consumer，不如 Rabbit 队列级顺手；Pulsar 有原生 deadLetterTopic 但搭配 Pulsar Function 才顺滑。
2. **消息模型贴合度（权重 30%）**：EasyOrange 是「1 事件 → 11 独立下游消费」——RabbitMQ 的 Exchange → 多独立队列模型是原生的，每个下游独立 ack/nack/DLQ/重试互不干扰；Kafka 用 11 个不同 consumer group 硬做是可行的，但「每个 consumer group 一个 offset + 无原生 DLQ」会把重试链路拆成 11 份分散逻辑，讲解与调试都更难；NATS JetStream 的 Pull Consumer 独立位点模型匹配度较高；Pulsar Shared subscription 也 OK。
3. **替换工程成本（权重 20%）**：RabbitMQ 是 0 成本（已落地，11 消费者 + DLQ 调度器 + 幂等 + traceId 全链路稳定运行）；NATS JetStream 备选 Adapter 估算 1-2 天（官方 `nats-spring-boot-starter` 成熟）；Kafka 估算 3-5 天（@RabbitListener 重写 + DLQ 自实现 + Spring Modulith Kafka 对接 + 测试 mock 全换）；Pulsar 估算 4-6 天（无成熟官方 starter + 本地运维重）。
4. **本地运维与可观测性（权重 10%）**：RabbitMQ 单容器 `docker run rabbitmq:4.0-management` 5 秒起，自带 Management UI 直接看交换机绑定、队列深度、死信堆积；NATS JetStream 单容器也轻（`nats:2.10 -js`）但 UI/可视化弱；Kafka KRaft 单节点能起但 consumer group lag 查看不如 Rabbit 直观；Pulsar 本地至少 3 容器（Broker + BookKeeper + ZooKeeper），太重。

**保留 Port 抽象的备选方案**：不替换 RabbitMQ 默认实现，仅在 `easyorange-framework` 中增加 `MessageBus` Port 接口（publish/subscribe/DLQ 重投能力抽象）+ `NatsJetStreamMessageBusAdapter` 可选实现，用 `@ConditionalOnProperty(name = "easyorange.messaging.bus", havingValue = "nats")` 切换。这个做法既保留对多 MQ 的选型对比能力，又不破坏现有可靠性链路，开发成本 1-2 天，收益极高。

## 后果（Consequences）

### 正向后果

- 无切换风险：现有 11 消费者 + Outbox + DLQ 三级重试 + traceId 全链路完全保留，测试不需要重写
- 选型可充分辩护：「为什么不用 Kafka？」可直接引用本 ADR 的 4 条决策依据（DLQ/模型/成本/运维），体现**选型思维（量、场景、成本、模型匹配综合判断）> 追新思维（「 Kafka 最火就用 Kafka」）**
- 可切换兜底：保留 `MessageBus` Port 抽象 + NATS 备选 Adapter 后，可现场演示一键切换，验证 Port 抽象的可替换性
- 本地启动轻：`docker compose up` 只多一个 RabbitMQ 容器，开发者不需要管多组件依赖

### 负向后果

- 存在「默认 Kafka = 消息队列标准」的认知偏差风险，需要本 ADR 记录 4 条决策依据（见 §备选方案 下每条的拒绝理由）才能充分辩护
- RabbitMQ 吞吐上限低于 Kafka / NATS Core，但在当前项目量级（<10k msg/s）是纯摆设，不构成真实瓶颈
- DLQ `x-message-ttl` 与调度器延迟可能叠加（队列级 TTL 到了自动进 DLQ，调度器 5 分钟才扫一次），实际延迟会比「指数退避值」略大；当前采用调度器扫描模型（非纯 TTL 重投），可观测性更好，这个 trade-off 可接受

### 缓解措施

- README.md §拒绝项 新增「未默认采用 Kafka/NATS/Pulsar」说明，直接链接本 ADR
- 记录「为什么不用 Kafka？」的 4 段决策依据（见 §备选方案 下 Kafka 的拒绝理由）
- 若后续真要切换：抽 `MessageBus` Port + 先接 NATS JetStream 备选 Adapter（成本最低、收益最高），再考虑 Kafka

## 备选方案（Alternatives Considered）

- **Apache Kafka（KRaft 模式 / 无 ZooKeeper）**：拒绝。核心理由 3 条：① **无原生 DLQ**，三级重试要自己造轮子（consumer 捕获异常→手动发 dlq topic→单独调度器重投→位点提交要配合手动 ack），投入产出比在当前量级下太低；② **模型不匹配**，EasyOrange 是 1 事件 → 11 独立下游 ack，Kafka 要搞 11 个 consumer group，每个组维护 offset + 各自重试逻辑，分散难讲；③ **运维与收益失衡**，KRaft 单节点能起但分区/副本因子/consumer lag 的可观测性不如 Rabbit UI，在当前「每天几万条事件」量级下，高吞吐优势是摆设。不是不会用 Kafka，是场景和成本都没到。
- **Apache Pulsar**：拒绝。核心理由 2 条：① **本地运维太重**（Broker + BookKeeper + ZooKeeper 三组件，docker compose 至少 3 容器 + 磁盘挂载），不满足「本项目单容器零运维」的约束；② **Spring Boot 生态成熟度不够**，`spring-pulsar` 是社区级不如 Rabbit/Kafka 官方 starter 稳定，个人项目花时间踩 Pulsar 的坑性价比太低。
- **NATS JetStream 2.10+**：**未默认采用，但保留作为首选备选 Adapter**。优点：① 单进程超轻（`nats:2.10 -js`，50MB 镜像）；② 原生 `max_deliver` + discard dead letter policy（接近 Rabbit 的 DLQ 能力）；③ 消费模型（Subject 路由 + Pull Consumer 独立位点）完全匹配 11 独立消费者场景。缺点：① 官方 `nats-spring-boot-starter` 的注解式消费（`@NatsListener` / `@JetStreamListener`）成熟度不如 `@RabbitListener`，ACK 语义需要手动控制（Ack/Nak/Term）；② 大众认知度不如 Kafka，容易被追问「为什么用 NATS 不用 Kafka」——正好用本 ADR 回应。保留作为 `@ConditionalOnProperty` 可切换的备选实现，1-2 天可落地。
- **Redis Streams**：拒绝。核心理由：① **无原生 DLQ / 死信能力**，三级重试、终端毒消息全要自己写；② 消息保留策略（maxlen / trim）是硬截断，没有 DLQ 转储语义；③ Redis Streams 是「Redis 捎带的能力」，容易被问「为什么不用正经 MQ」；④ 幂等/至少一次语义是自己封装（XACK + pending list），复用现有 EventIdempotencyChecker 成本不如 Rabbit 低。
- **ActiveMQ Artemis**：拒绝。无显著优于 RabbitMQ 的匹配点，DLQ 能力等价但生态和知名度不如 Rabbit，切换没有净收益。
- **RocketMQ**：拒绝。Spring Boot 生态 starter 成熟度 OK，但 DLQ 是 Broker 级固定的 `%DLQ%` 队列前缀 + 固定 16 次重试，无法自定义每个消费者自己的 DLQ 路由与退避策略，匹配度低于 Rabbit。

## 备注（Notes）

- 相关文档：
  - [doc/工程指标.md](../../doc/工程指标.md) §1.3 AI 工程化 8 件套
  - [AGENTS.md](../../AGENTS.md) §11 领域事件机制 + traceId 传递链路
  - [README.md](../../README.md) §架构总览 + 拒绝项清单
- 相关代码：
  - [RabbitMQConfig.java](../../easyorange-backend/easyorange-framework/src/main/java/com/cartethyia/easyorange/framework/messaging/config/RabbitMQConfig.java)：交换机 + 10 队列 + DLQ 绑定声明（2026-08-04 起，见顶部现状更新）
  - [DlqRetryScheduler.java](../../easyorange-backend/easyorange-framework/src/main/java/com/cartethyia/easyorange/framework/event/dlq/DlqRetryScheduler.java)：DLQ 分级重试调度（`x-retry-count` 头驱动，重试 ≥ 3 转储 `eo.dlq.terminal`；原 `ExponentialBackoffRetryStrategy` 已并入本类，见顶部现状更新）
  - [EventConsumerHandler.java](../../easyorange-backend/easyorange-framework/src/main/java/com/cartethyia/easyorange/framework/event/core/EventConsumerHandler.java)：消费者统一处理（ack/nack/幂等/异常链，前身为 `AbstractDomainEventConsumer`）
  - [EventMetadataMessagePostProcessor.java](../../easyorange-backend/easyorange-framework/src/main/java/com/cartethyia/easyorange/framework/event/metadata/EventMetadataMessagePostProcessor.java)：traceId 注入 MQ 消息头
- 重评估触发条件：
  1. 业务事件量级预估持续 > 10k msg/s 超过 1 周
  2. 出现「顺序消费分区」硬诉求（如同一 orderId 关联事件必须严格有序且并发 > 单消费者）
  3. 「MessageBus Port + NATS 备选 Adapter」落地后发现 NATS 在本项目维度全面优于 Rabbit，可重新评估切换默认值
- 触发后首选顺序：**NATS JetStream（成本最低、模型最匹配）→ 再考虑 Kafka（大众认知强但成本高）**，不要直接跳 Kafka。
