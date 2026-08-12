# ADR 0007 — 拒绝 Saga：订单创建回归「本地单事务 + 分布式锁 + Outbox」

- **状态**：接受
- **日期**：2026-08-02
- **决策者**：后端架构
- **标签**：`transaction` `order` `observability` `anti-saga`
- **替代**：ADR-0001 的「订单创建采用 Saga 编排」决策（含补偿机制与状态机持久化）

---

## 上下文（Context）

ADR-0001 决定订单创建采用 Saga 编排 + 反向补偿，ADR-0007 初版进一步收敛为「单事务编排 + Saga 表仅作可观测性」。落地检查发现演进后的实现仍然是一层装饰，且单库场景下比「不做 Saga」更差：

1. **原子性本就不需要 Saga**：`OrderCommandHandler.handle(CreateOrderCommand)` 上的 `@Transactional` 使订单、库存扣减（`ProductOrderPort.decreaseStock`，REQUIRED 传播）与支付创建（`PaymentGatewayPort.createPayment`，REQUIRED 传播）全部进入**同一 MySQL 事务**，任一步失败数据库整体回滚，无需任何编排/补偿。
2. **补偿要么冗余、要么死**：事务内补偿 lambda 与回滚重复劳动（已被初版删除）；若未来跨库/异步，内存补偿在进程崩溃后无法重建，持久化的只有状态快照，二者脱节。
3. **失败状态从未落库**：`SagaCoordinator` 的写操作即使 `REQUIRES_NEW` 独立提交，也只为每次成功下单多付出 3~4 次额外写事务；失败场景的业务现场（订单行等）仍随业务事务回滚，`eo_saga` 表留下的只是一行 `FAILED(errorMessage)`，卡单检测的真实抓手是 MySQL 事务本身而非 Saga 表。
4. **「比不做更好」不成立**：单库下纯本地事务 + 分布式锁 + Outbox 在正确性、成本、代码量上全部优于带 Saga 表的方案——保留 Saga 的唯一收益是「形式上更符合分布式事务惯例」，而架构应由实际的一致性、成本与可维护性支撑，不应由形式驱动。

约束未变：单数据库部署（MySQL 8.4）、平台不经手资金、C2C 直发、模块边界用 Port/Adapter + `<optional>` 隔离。

## 决策（Decision）

**移除订单创建的 Saga 层**，回归最简可靠形态：

1. **原子性 = 本地单事务**。`OrderCommandHandler.handle(CreateOrderCommand)` 在单一 `@Transactional(rollbackFor = Exception.class)` 内依次执行：获取分布式锁 → 准备商品 → 创建订单 + 发布 `OrderCreatedEvent`（Outbox 同事务原子）→ `decreaseStock` → `createPayment`。任一步失败事务整体回滚，抛 `OrderCreationException`，**无补偿路径**。
2. **并发控制 = 分布式锁**。`LockPort` / [RedissonLockAdapter.java](../../easyorange-backend/easyorange-order/src/main/java/com/cartethyia/easyorange/order/adapter/outbound/lock/RedissonLockAdapter.java)（Redisson，key=`eo:order:lock:product:{productId}`，按 `productId` 排序防死锁，10s 获取超时，leaseTime=`-1` 由 watchdog 续期，锁在事务提交后释放）负责同商品下单**排队串行**；库存扣减由 `ProductRepository` 乐观锁版本检查**兜底防超卖**（并发时抛 `ConcurrentUpdateException` 使订单回滚）。
3. **副作用 = Outbox 事件**。库存/支付为同事务直写；下游状态变更（取消/退款恢复库存、完成标记售出）由 `OrderLifecycleEventConsumer` 消费订单生命周期事件异步触发。

已删除的 Saga 代码（`git rm`）：`CreateOrderSaga`、`SagaCoordinator`、`SagaTimeoutScheduler`、`SagaException`、`SagaRepository`、`SagaState`、`SagaStatus`、`SagaDO`、`SagaMapper`、`SagaRepositoryImpl`、`OrderCompensationService`。`eo_saga` 表由 `V2__drop_order_saga_table.sql` 删除（`DROP TABLE eo_saga_status`）。

关键实现：

- 下单入口：[OrderCommandHandler.java](../../easyorange-backend/easyorange-order/src/main/java/com/cartethyia/easyorange/order/application/command/OrderCommandHandler.java)
- 锁：[RedissonLockAdapter.java](../../easyorange-backend/easyorange-order/src/main/java/com/cartethyia/easyorange/order/adapter/outbound/lock/RedissonLockAdapter.java)（`LockPort` 的 Redisson 实现）、执行：`OrderPreparation.java`
- 生命周期消费者：[OrderLifecycleEventConsumer.java](../../easyorange-backend/easyorange-order/src/main/java/com/cartethyia/easyorange/order/adapter/inbound/messaging/OrderLifecycleEventConsumer.java)（队列 `eo.order.lifecycle`）

核心驱动力：

- 删除后代码更少、语义更诚实，评审中「为什么下单用 Saga」不再需要辩解
- 「拒绝 Saga」本身成为叙事：单库场景正确识别过度设计，比挂着失效的状态机更能体现架构判断力
- 预留真实演进路径：仅当 product/payment 模块拆分独立数据源/独立部署时，才需要重新引入跨服务编排

## 后果（Consequences）

### 正向后果

- 每次下单少 3~4 次 `REQUIRES_NEW` 独立写事务，RT 与连接池占用下降
- 删除约 10 个 Saga 相关类，模块结构更贴近实际运行语义
- 测试更直观：`OrderCommandHandlerCreateTest` 断言成功路径与失败回滚，无状态机分支

### 负向后果

- 失去 Saga 状态机的「仪式感」叙事，需以 ADR 决策质量替代
- 若未来出现真实跨库/异步边界，需重新设计补偿（当前单库不存在）
- 下单链路中间态仅靠订单状态 + 事件消费可见，不再有独立的 saga 生命周期表

### 缓解措施

- 超时检测与人工介入由既有机制承担：`OrderTimeoutTask`（30 分钟未支付自动 CANCELLED + 库存恢复）、DLQ 三级重试
- 演进触发条件记录于本文档备注，避免未来无依据地加回 Saga

## 备选方案（Alternatives Considered）

- **保留 Saga 表作可观测性（上一版）**：拒绝。失败现场随业务事务回滚，`eo_saga` 只剩一行状态码，观测价值低于其代码与写路径成本；「比不做更好」的判断不成立。
- **真跨事务 Saga（拆事务 + 补偿）**：拒绝。单库部署下本地事务提供比最终一致更强的一致性与更低成本，强行拆事务只削弱正确性；仅在模块独立部署时值得。
- **2PC / XA（如 Seata AT 模式）**：拒绝。需全局事务协调器，穿透 Port/Adapter 边界，锁等待长，与 C2C 直发场景的轻平台边界冲突（原 ADR-0001 结论保留）。
- **TCC**：拒绝。各参与模块需实现 Try/Confirm/Cancel 三套接口，侵入大，工程成本收益不匹配。

## 备注（Notes）

- 替代关系：ADR-0001「订单创建采用 Saga 而非 2PC」整体被本 ADR 替代（含补偿机制与状态机）；「拒绝 2PC」的结论保留。
- 相关 ADR：[0001-order-saga-vs-2pc.md](0001-order-saga-vs-2pc.md)（已标记 Superseded）
- 后续演进触发条件：product 或 payment 模块拆分独立数据源/独立部署，或出现真实跨库异步副作用时，重新评估跨服务编排（真 Saga / 事务性 Outbox 补偿）
