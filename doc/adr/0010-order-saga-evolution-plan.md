# ADR 0010 — 真 Saga / 事务性 Outbox 补偿演进方案（下单链路跨数据源时）

- **状态**：提议
- **日期**：2026-08-14
- **决策者**：后端架构
- **标签**：`transaction` `order` `saga` `outbox` `evolution`

---

## 上下文（Context）

[ADR-0007](0007-order-local-tx-over-saga.md) 在**单数据库部署**下拒绝 Saga：本地单事务 + 分布式锁 + Outbox 在正确性、成本、代码量上全面优于编排式 Saga。但该决策的成立依赖一个明确前提——**order / product / payment 共享同一个 MySQL 数据源**。面试叙事里也承诺了「真正的演进触发条件是 product/payment 模块拆分独立数据源 / 独立部署，那时才重新评估真 Saga / 事务性 Outbox 补偿」。

本文档把「那时」的落地方案提前设计好（方案级，不是口头答），包含：

1. 拆分触发的判据（什么量级/什么业务变化才值得拆）
2. 跨数据源后的下单链路演进（本地单事务 → 编排 Saga）
3. 事务性 Outbox 补偿（在「编排失败」与「补偿执行」之间引入持久化状态机）
4. 代码骨架（~300 行核心，演示编排器与补偿注册的形态）

## 决策（Decision）

### 一、拆分判据（不满足就不拆）

| 判据 | 阈值 | 说明 |
|---|---|---|
| 订单写入 QPS | > 5k/s 且持续 | 单库写入锁竞争成为瓶颈（当前 k6 只读 640 req/s，写路径远未到） |
| 支付对账/风控独立 SLA | 支付模块需要独立发布窗口 | 支付变更不再随订单模块发版 |
| 库存独立团队/独立缓存 | product 模块引入独立 Redis 集群 | 库存热点商品单行锁竞争 |
| 故障爆炸半径 | 订单表故障拖垮支付查询 | 监控里同实例故障扩散 |

> 注意：**拆分数据源不等于拆分部署**。可以先拆库（物理隔离、逻辑单事务不可用）再拆服务；本文档按「拆库即触发」设计，服务拆分只是把编排器从进程内变跨进程。

### 二、拆分后的下单链路（编排 Saga）

```
CreateOrderCommand
   │
   ▼
┌─────────────────────────────────────────────────────────────┐
│ SagaOrchestrator.begin(orderId, sagaId)                     │
│   eo_saga(id, status=STARTED, payload, compensation_log)    │  ← 状态机持久化
│                                                             │
│   ① createOrder（order 库）        —— 本地事务 + Outbox      │
│        └─ 失败 → compensate: 无（订单未建）→ saga FAILED     │
│   ② decreaseStock（product 库）    —— 本地事务               │
│        └─ 失败 → compensate: ① 建单作废（标记 CANCELLED）    │
│   ③ createPayment（payment 库）    —— 本地事务               │
│        └─ 失败 → compensate: ② 恢复库存 + ① 建单作废         │
│                                                             │
│   SagaScheduler 每 5min 扫 eo_saga:                          │
│     STARTED 且卡住超时 → 重放当前步骤（幂等）                 │
│     COMPENSATING → 按 compensation_log 逆序补偿              │
│     FAILED → 转储人工（与 DLQ terminal 同一套语义）           │
└─────────────────────────────────────────────────────────────┘
```

关键点（与 ADR-0007 拒绝 Saga 的理由一一对应，说明「为什么现在可以」）：

1. **原子性来源变了**：单库时 MySQL 事务就是原子性；拆库后不再有跨库事务，编排器成为唯一的一致性载体——Saga 从「装饰」变成「必要」。
2. **补偿不再冗余**：单库时补偿与回滚重复；跨库后失败步骤的本地事务已提交，补偿是唯一撤销手段，`compensation_log` 记录「已执行步骤」保证只补偿一次（幂等）。
3. **状态机不再空转**：`eo_saga` 表记录每步结果与补偿进度，崩溃后由 `SagaScheduler` 重放——这就是 ADR-0007 曾批评的「失败状态从未落库」的补位。
4. **本地单事务保留**：每个参与步骤内部仍是本地事务 + 各自 Outbox（订单事件、支付事件各自可靠发布），Saga 只编排跨库的先后与补偿，不替代局部事务。

### 三、事务性 Outbox 补偿（编排失败时的自愈）

`eo_saga` 的每一步执行都通过 Outbox 发布「步骤完成事件」（`SagaStepCompletedEvent`），消费方做两件事：

- **推进状态机**：`SagaStateConsumer` 更新 `eo_saga.step_index`；
- **触发补偿**：任一步失败发布 `SagaStepFailedEvent`，`SagaCompensationConsumer` 逆序执行 `compensation_log` 中已成功步骤的补偿命令（每个补偿命令本身是本地事务 + 幂等键，重复执行安全）。

这样编排器本身可以崩溃——补偿由事件驱动恢复，而不是依赖编排器进程存活（ADR-0007 指出的「内存补偿在进程崩溃后无法重建」被 Outbox + 事件消费解决）。

### 四、代码骨架（示意，不落地）

```java
// 编排器：按顺序执行步骤，失败逆序补偿；步骤注册表代替手写 if/else
public class OrderSagaOrchestrator {

    private final SagaStateStore stateStore;      // eo_saga 持久化
    private final List<SagaStep> steps;           // ①建单 ②扣库存 ③建支付

    public SagaResult execute(CreateOrderCommand cmd) {
        String sagaId = stateStore.begin(cmd);
        for (int i = 0; i < steps.size(); i++) {
            SagaStep step = steps.get(i);
            try {
                step.execute(cmd);                 // 每步：本地事务 + 各自 Outbox
                stateStore.recordSuccess(sagaId, step.name());
            } catch (Exception e) {
                compensate(sagaId, i);             // 逆序执行 0..i-1 的补偿
                stateStore.markFailed(sagaId, e.getMessage());
                throw new SagaExecutionException(sagaId, e);
            }
        }
        stateStore.markCompleted(sagaId);
        return new SagaResult(sagaId, "COMPLETED");
    }

    private void compensate(String sagaId, int upToExclusive) {
        for (int i = upToExclusive - 1; i >= 0; i--) {
            steps.get(i).compensate(sagaId);       // 每个补偿：本地事务 + 幂等键
        }
    }
}

// 步骤：原子操作 = 本地事务 + 补偿命令
public interface SagaStep {
    String name();
    void execute(CreateOrderCommand cmd);
    void compensate(String sagaId);
}

// 示例：扣库存步骤（product 库）——失败时补偿是「恢复库存」
public class DecreaseStockStep implements SagaStep {
    private final ProductInventoryPort inventoryPort;

    public void execute(CreateOrderCommand cmd) {
        inventoryPort.decreaseStock(cmd.productId(), cmd.quantity());   // product 库本地事务
    }

    public void compensate(String sagaId) {
        inventoryPort.restoreStock(sagaId);                             // 幂等：按 sagaId 去重
    }
}
```

配合的 `SagaScheduler`（每 5min 扫 `eo_saga`：STARTED 超时重放 / COMPENSATING 续跑 / FAILED 转储）与现有 `DlqRetryScheduler` 同构，代码量 ~150 行。

## 后果（Consequences）

**正面**：
- 「什么时候才上 Saga」从口头答变成方案级：拆库判据 + 编排形态 + Outbox 补偿自愈 + 代码骨架齐全。
- 现状（ADR-0007 拒绝 Saga）与演进（ADR-0010 真 Saga）形成完整的决策闭环，体现「拒绝不是不会，是场景判断」。

**代价/风险**：
- 拆库是基础设施级变更（双数据源事务边界、Flyway 拆分、部署拓扑），本方案仅是编排层设计，不含数据迁移方案。
- 编排 Saga 的每步补偿命令都要幂等设计（按 sagaId + step 去重），这是方案落地时的主要成本。
- 方案长期保持 Proposed 不实施——与 ADR-0007 的「触发条件未到」保持一致，避免在单库下为假想场景引入复杂度（YAGNI）。

## 备注（Notes）

- 相关 ADR：[ADR-0007](0007-order-local-tx-over-saga.md)（现状：单库拒绝 Saga，本方案是其演进预案）、[ADR-0001](0001-order-saga-vs-2pc.md)（已替代历史：Saga vs 2PC 的完整对比）、[ADR-0005](0005-messaging-rabbitmq.md)（MQ 选型，`SagaScheduler` 与 `DlqRetryScheduler` 同构）
- 相关文档：[doc/interview/01-话术与总纲.md](../interview/01-话术与总纲.md)（面试叙事「演进触发条件」应答）
- 实施触发条件：正文「拆分判据」任一阈值满足（订单写入 QPS > 5k/s 且持续、支付模块需独立发布窗口、库存引入独立 Redis、故障爆炸半径扩大）时，按本方案重新评估
