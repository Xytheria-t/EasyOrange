# easyorange-order 模块指南

订单管理模块，DDD + CQRS 架构，处理订单全生命周期。

## 目录结构

```
order/
├── adapter/
│   ├── inbound/
│   │   ├── web/
│   │   │   ├── controller/                  # OrderCommandController（写端点）/ OrderQueryController（读端点）
│   │   │   ├── assembler/OrderCommandAssembler.java   # Request DTO → Command
│   │   │   └── dto/request/                 # CreateOrderRequest / CancelOrderRequest / RefundOrderRequest（record + @NotBlank）/ QueryOrderRequest
│   │   ├── job/                             # OrderTimeoutTask（超时取消）/ OrderAutoConfirmTask（自动确认收货）/ OrderStateMigrationExecutor
│   │   └── messaging/                       # 跨模块事件消费者
│   │       ├── OrderLifecycleEventConsumer.java    # 取消/退款恢复库存，完成标记售出
│   │       └── PaymentSucceededEventConsumer.java  # payment.succeeded → 订单置 PAID
│   └── outbound/
│       ├── persistence/                     # OrderDO / OrderItemDO / OrderMapper / OrderItemMapper
│       │                                    #   / OrderRepositoryImpl / OrderQueryRepositoryImpl / OrderDataMapper（手写 @Component：DO ↔ Domain）
│       ├── cache/RedisOrderCacheAdapter.java       # 实现 OrderCachePort
│       └── config/                          # OrderTimeoutProperties（order.timeout.*）/ OrderAutoConfirmProperties（order.auto-confirm.*）
├── application/
│   ├── command/                             # OrderCommandHandler（订单命令唯一执行器）+ OrderItemPreparer（创建流水线的订单项校验/构建）
│   │                                        #   / CreateOrderCommand / CreateOrderResult / PayOrderCommand / CancelOrderCommand
│   │                                        #   / ShipOrderCommand / ConfirmReceiptCommand / RefundOrderCommand
│   ├── service/OrderCacheEvictor.java       # 事务提交后失效买卖家列表缓存（命令/任务共用）
│   ├── query/                               # OrderQueryHandler / OrderListQuery（record：orderNo, status: OrderStatus, buyerId, sellerId, pageNum, pageSize）
│   │   ├── readmodel/                       # OrderReadModel / OrderItemReadModel
│   │   └── assembler/OrderReadModelAssembler.java  # ReadModel → OrderVO（应用层组装）
│   ├── port/query/OrderQueryRepository.java # 读仓储（countByStatus 入参为 OrderStatus 枚举）
│   └── dto/OrderVO.java
└── domain/
    ├── aggregate/                           # Order（不可变，字段 final）/ OrderCreateSpec / OrderReconstructSpec
    ├── constant/                            # OrderConstant / OrderStatus / OrderAction（状态机唯一事实来源）/ OrderResultCode / ClosureKind
    ├── event/                               # OrderEvent（sealed）+ OrderCreated/Paid/Shipped/Completed/Cancelled/RefundedEvent + OrderItemRef
    ├── exception/OrderDomainException.java
    ├── port/                                # OrderCachePort / ProductInventoryPort / PaymentGatewayPort / UserInfoPort / OrderQueryCondition
    ├── repository/OrderRepository.java
    └── valueobject/                         # OrderId / OrderNo / Address / Phone / UserId / OrderItem / OrderItemSnapshot / PaymentStatus / Version
```

> **跨模块适配器位置**：order 定义的 `ProductInventoryPort` / `PaymentGatewayPort` / `UserInfoPort` 的实现在 `easyorange-application/adapter/outbound/` 的 `product/`、`payment/`、`user/` 子包；`OrderCachePort` 的实现 `RedisOrderCacheAdapter` 位于 order 模块自身 `adapter/outbound/cache/`，因其仅操作订单域缓存。

## 下单链路（拒绝 Saga）

创建订单不使用 Saga 编排，采用**本地单事务 + 分布式锁 + Outbox 事件**。语义见 [ADR-0007](../../doc/adr/0007-order-local-tx-over-saga.md)：**原子性由单 `@Transactional` 回滚兜底，失败随事务整体回滚，无需反向补偿**（单库场景下补偿与回滚重复、失败状态随事务回滚丢失）。

**执行流程**：
```
OrderCommandHandler.createOrder(CreateOrderCommand) ─ 分布式锁在事务外获取、提交后释放 ─
  1. DistributedLockPort 获取商品锁（key=eo:order:lock:product:{productId}，按 productId 排序避免死锁，等待 10s）
  2. OrderItemPreparer 准备订单项（一次批量读齐资产快照，校验存在/在线/库存/同一资产方）
  3. Order.createOrder 创建订单 + 发布事件（Outbox 同事务原子）
  4. ProductInventoryPort.decreaseStock() 同步扣库存（同事务，订单 ID 即库存流水幂等键）
  5. PaymentGatewayPort 创建支付记录（同事务）
  6. 任一步失败 → 业务事务整体回滚，抛 OrderDomainException（B3009；库存/支付同事务回滚，无补偿路径）
```

**库存恢复**：仅由 `OrderLifecycleEventConsumer` 消费订单取消/退款事件时调用 `ProductInventoryPort.restoreStock(orderId, productId, quantity)` 恢复，数量取自事件明细（`OrderItemRef`，与下单扣减对称）；完成事件触发 `markAsSold`。重复投递由 product 侧库存流水的唯一键兜底，消费者只负责把数量和订单 ID 如实传下去。明细为空视为载荷损坏，显性失败进重试/DLQ 人工介入。

**支付桥接（订单 PAID 唯一来源）**：`PUT /api/orders/{id}/pay` 校验买家身份与 `canPay()` 后经 `PaymentGatewayPort.pay` 委托支付模块发起两阶段支付，**不再直接置 PAID**。支付成功由 payment 模块发布 `PaymentSucceededEvent`（routing key `payment.succeeded`，队列 `eo.order.payment`，事件含 orderId），`PaymentSucceededEventConsumer` 消费后调 `OrderCommandHandler.onPaymentSucceeded` 经 `PAY` 守卫置 `PAID` 并发布 `OrderPaidEvent`。消费按 eventId 幂等（`EventConsumerHandler`）；订单已支付时跳过；订单已取消时触发自动退款（`refundPayment`，订单保持取消态不流转）；其余非法状态抛错经重试进 DLQ/terminal 人工介入。

## CQRS 架构

**Command 侧**: `OrderCommandController` → `OrderCommandHandler` → `Order` → `OrderRepository`

**Query 侧**: `OrderQueryController` → `OrderQueryHandler` → `OrderQueryRepository` → `OrderReadModel`

> 订单项展示走**自持留痕快照**：`eo_order_item.product_snapshot` 由 `OrderDataMapper` 解析成 `OrderItemSnapshot`，读侧不再跨模块查商品——资产改名、改价或删除都不改变已下订单的展示。

## 对象映射策略

两层映射职责分离：

| Mapper | 方向 | 位置 | 说明 |
|--------|------|------|------|
| `OrderDataMapper` | DO ↔ Domain | `adapter/outbound/persistence/` | 手写 `@Component`：OrderDO ↔ Order、OrderItemDO ↔ OrderItem（含乐观锁 version 与 ProductSnapshot JSON 互转） |
| `OrderReadModelAssembler` | ReadModel → VO | `application/query/assembler/` | OrderReadModel → OrderVO（含脱敏、商品/用户名填充） |

`OrderDO` 是纯数据库实体，不含映射逻辑。

## 跨模块通信

通过 `port/` 接口解耦，适配器集中在 `easyorange-application/adapter/outbound/`：

| 端口 | 适配器 | 位置 |
|------|--------|------|
| `ProductInventoryPort` | `ProductInventoryAdapter` | `adapter/outbound/product/` |
| `PaymentGatewayPort` | `OrderPaymentGatewayAdapter` | `adapter/outbound/payment/` |
| `UserInfoPort` | `OrderUserInfoAdapter` | `adapter/outbound/user/` |
| `OrderCachePort` | `RedisOrderCacheAdapter` | order 模块 `adapter/outbound/cache/` |

## 订单状态机

**动作驱动（Action-driven）设计**：`OrderAction` 枚举是状态机唯一事实来源，每个动作声明前置状态集合（sources）、目标状态（target）、目标支付状态（targetPaymentStatus，null 表示不变）、是否需要原因、非法错误码及额外支付前置条件（paymentGuard）。`OrderStatus.canTransitionTo()` 由此派生，`Order` 聚合根统一经私有 `transitionTo(action, reason)` 守卫执行——一处校验合法性 + 一处应用副作用（状态 + 支付状态 + 关闭原因/时间），**禁止绕过守卫直接修改状态**。

```
PENDING_PAYMENT ──PAY──→ PAID ──SHIP──→ SHIPPED ──CONFIRM_RECEIPT──→ COMPLETED
       │                   │  │                     │
       │                   │  └──FORCE_CANCEL──→    │
       │                   └──REFUND──→            └──REFUND──→
   CANCEL──→ CANCELLED            │                              REFUNDED
       │        ▲                 │
       └────────┴──FORCE_CANCEL───┘
```

- `CANCEL`（买家）：仅限待付款；`FORCE_CANCEL`（管理端）：待付款或已付款
- `REFUND`（退款）：已付款或已发货，且支付状态必须为已支付（paymentGuard）
- 状态码使用 String code（`OrderStatus.PENDING_PAYMENT.getCode()` → `"PENDING_PAYMENT"`），枚举实现 `BaseCodeEnum`，`@EnumValue` / `@JsonValue` 完成 VARCHAR 列与 JSON 互转

## 定时任务

- `OrderTimeoutTask`: 未支付订单超时自动取消（`order.timeout.*` 配置，每单分布式锁 + 本地事务 + Outbox 原子提交）
- `OrderAutoConfirmTask`: 已发货订单超时自动确认收货（`order.auto-confirm.*` 独立配置，每单分布式锁，防止多实例重复确认）

> **聚合根重建硬约束**：`OrderReconstructSpec.items` 允许为空，但**仅限纯查询路径**（列表/详情读模型）。任何会产生领域事件的写操作（命令、定时任务）必须加载行项重建，否则事件 `productIds`（完成事件）/ `items`（取消、退款事件）为空，导致库存恢复/售出标记静默失效。

## 常见开发任务

### 添加订单新转换

1. 在 `OrderAction` 枚举新增动作（声明前置状态、目标状态、目标支付状态、是否需要原因、错误码）
2. `Order` 添加转换方法，内部委托 `transitionTo(新动作, reason)` 并构造对应领域事件（返回 `Transition<Order, XxxEvent>`）
3. 添加对应领域事件
4. `OrderCommandHandler` 添加命令处理（命令为 record）
5. 如涉及下单链路，检查 `OrderCommandHandler.createOrder(CreateOrderCommand)` 执行顺序与事务回滚语义（单事务内，无需补偿）
6. Flyway 迁移：`status` 列 CHECK 约束追加新 code
7. 在 `OrderActionTest` 中补充前置状态/目标状态断言，`OrderTest` 补充转换用例

### 添加新查询维度

1. `OrderListQuery` record 添加字段
2. 请求 DTO `adapter/inbound/web/dto/request/` 添加字段
3. Controller 提取参数构造 `OrderListQuery` 传给 `OrderQueryHandler.getMyOrders()/getSoldOrders()`
4. `OrderQueryRepository` 修改查询
5. `OrderReadModel` 添加字段
6. `OrderReadModelAssembler` 更新
7. 测试

## Spec Record 与 Command Record

聚合根工厂与重建入口通过 spec record 收敛长参数列表：

| Spec / Command | 用途 | 关键字段 |
|----------------|------|---------|
| `OrderCreateSpec` | `Order.createOrder()` 工厂参数 | orderId, buyerId, sellerId, items, address, phone, remark |
| `OrderReconstructSpec` | `Order.from()` 重建参数 | id, orderNo, buyerId, sellerId, items, totalAmount, status, paymentStatus, ... |
| `Transition<Order, E>` | 状态转换结果（聚合根新实例 + 领域事件） | aggregate, event |
| `CreateOrderCommand` | 创建订单命令（record） | items, address, phone, remark, paymentMethod |
| `PayOrderCommand` / `ShipOrderCommand` / `ConfirmReceiptCommand` | 单字段命令（record） | orderId |
| `CancelOrderCommand` / `RefundOrderCommand` | 带原因命令（record） | orderId, reason |
| `OrderListQuery` | 列表查询参数收敛 | orderNo, status: OrderStatus, buyerId, sellerId, pageNum, pageSize |
