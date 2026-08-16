# ADR 0001 — 订单创建跨模块事务采用 Saga 而非 2PC

> **状态**：**已替代（Superseded by ADR-0007）** — 被 [ADR-0007](0007-order-local-tx-over-saga.md)（2026-08-02）整体替代：订单创建已移除 Saga 层，回归「本地单事务 + 分布式锁 + Outbox」。本 ADR 仅保留「拒绝 2PC/XA/TCC」的结论；其余内容（编排器、补偿、状态机、超时检测）为历史记录，代码与表结构均已删除。

- **状态**：已替代
- **日期**：2026-07-14
- **决策者**：后端架构
- **标签**：`saga` `distributed-transaction` `order` `event-driven`

---

## 上下文（Context）

EasyOrange 的 C2C 资产流转业务中，「认领方下单」是一个跨三个限界上下文的写操作链：

1. **order 模块**：创建订单（`PENDING_PAYMENT`）
2. **product 模块**：锁定 / 扣减商品库存，标记商品 `SOLD`
3. **payment 模块**：创建支付记录，等待认领方付款

模块边界已经通过 DDD 六边形 + Port/Adapter + `<optional>true</optional>` Maven 隔离落地（见 [架构-系统架构.md](../../doc/架构/架构-系统架构.md)），跨模块通信已大量使用 RabbitMQ Topic Exchange + 11 个消费者 + DLQ（如 `OrderCreatedEvent`、`StockReservationRequestedEvent`、`PaymentInitiationRequestedEvent`）。

强制约束：

- **平台不经手资金**（见 `doc/PRODUCT_DIRECTION.md`），资金流不进平台账户，支付仅是「记账 + 状态推进」
- **C2C 直发**：平台不持有库存，库存锁定的语义是「商品下架 + 订单项关联」，不是仓储扣减
- **可观测性优先**：项目定位是 LLM × DDD 工程化实战项目（`README.md`），中间态可观测比强一致更重要
- 已有 RabbitMQ 基础设施，无 Seata / XA Coordinator

## 决策（Decision）

订单创建链路采用 **Saga 编排（Orchestration）+ 反向补偿**，不使用 2PC/XA。

核心实现：

- 编排器：[CreateOrderSaga.java](../../easyorange-backend/easyorange-order/src/main/java/com/cartethyia/easyorange/order/application/saga/CreateOrderSaga.java)
- 步骤：创建订单 → 同步扣库存 → 创建支付；失败时逆序执行补偿（`restoreStock` → `cancelOrder`）
- 状态机持久化到 `eo_saga` 表：`PENDING → ORDER_CREATED → PAYMENT_CREATED → COMPLETED` / `COMPENSATING → COMPENSATED`；`SagaTimeoutScheduler` 每 60s 扫描 30 分钟无更新的活跃 saga，重试达到 `MAX_RETRY_COUNT` 的标记 `MANUAL_INTERVENTION`，其余标记 `TIMEOUT` 等待人工介入
- 分布式锁按 `productId` 排序获取（`DistributedLockManager`），避免死锁
- 库存扣减在 Saga 同步步骤中完成（`CreateOrderSaga` 内直接调用 `ProductInventoryPort.decreaseStock()`），不依赖异步事件路径

Saga 同步流：

```
CreateOrderSaga.execute()  ─ @Transactional ─
  Step 1: 创建订单 + 注册 cancelOrder 补偿
  Step 2: ProductInventoryPort.decreaseStock() + 注册 restoreStock 补偿  ← 同步
  Step 3: 创建支付
  失败时逆序: restoreStock → cancelOrder
```

核心驱动力：

- 与已有 RabbitMQ 事件驱动基础设施天然对齐，无需引入新中间件
- 中间态可观测（`SagaStatus` 落库 + 日志），便于讲解与排查
- 模块边界已用 Port 隔离，Saga 编排器只依赖 `PaymentGatewayPort` 等抽象，不直接耦合 payment 模块实现
- 业务上不存在「资金 double spend」风险（平台不经手资金），最终一致即可，强一致收益不抵成本

## 后果（Consequences）

### 正向后果

- 无 2PC 长锁等待，下单链路 RT 友好
- 单模块故障不会拖垮整条链路（payment 宕机时 order 仍可创建，补偿后续触发）
- Saga 状态持久化可查，超时自动检测（`SagaTimeoutScheduler` 30 分钟无更新），符合「LLM × DDD 工程化实战项目」的可观测诉求
- 与现有 RabbitMQ DLQ + 指数退避机制自然衔接

### 负向后果

- 存在中间态可见窗口（订单已建但支付未建），客户端需轮询或接收事件
- 补偿逻辑需独立测试覆盖（`CreateOrderSagaTest` 已守卫，单库单事务下由回滚兜底，见 ADR-0007）
- 不保证强一致，仅保证最终一致 + 业务可补偿

### 缓解措施

- 订单 30 分钟超时任务（`OrderTimeoutTask`）自动 `CANCELLED` 并触发商品重新可售
- `SagaTimeoutScheduler` 自动检测超时（30 分钟无更新），重试次数耗尽时标记 `MANUAL_INTERVENTION` 等待运维介入
- 补偿路径有专门的单测覆盖（`CreateOrderSagaTest`，见 ADR-0007）

## 备选方案（Alternatives Considered）

- **2PC / XA（如 Seata AT 模式）**：拒绝。需要全局事务协调器，引入新中间件；跨模块 Port/Adapter 边界会被资源管理器穿透破坏；锁等待长，与 C2C 直发场景的轻平台边界冲突。
- **TCC（Try-Confirm-Cancel）**：拒绝。每个参与模块都要实现 Try/Confirm/Cancel 三套接口，对 product / payment 模块侵入大；项目模块多但业务聚焦核心流程（见 `doc/PRODUCT_DIRECTION.md`），TCC 的工程成本收益不匹配。
- **本地消息表（最终一致，无编排器）**：拒绝。订单创建是「编排式」流程（步骤有强先后与补偿依赖），纯事件链路（Choreography）会让失败路径难追踪；项目已有 Saga 状态机诉求，本地消息表更适合单发出队场景。
- **纯事件 Choreography**：拒绝。无编排器时，订单创建的多步骤流程分散在各消费者，链路整体不可见，跨模块追踪与失败排查成本高。

## 备注（Notes）

- **Superseded** by [ADR-0007](0007-order-local-tx-over-saga.md)（2026-08-02）：订单创建移除 Saga 层，回归本地单事务 + 分布式锁 + Outbox
- 相关文档：[doc/集成/AI-资产管理.md](../../doc/集成/AI-资产管理.md)（订单闭环）、[doc/架构/架构-系统架构.md](../../doc/架构/架构-系统架构.md)
- 重评估触发：当业务引入资金托管（不再 C2C 直发）或 product/payment 模块拆分独立数据源时，重新评估跨服务编排（真 Saga / 事务性 Outbox 补偿），见 ADR-0007 备注。
