# easyorange-order 模块指南

订单管理模块，DDD + CQRS 架构，处理订单全生命周期。

## 目录结构

```
order/
├── adapter/
│   ├── inbound/
│   │   ├── web/
│   │   │   ├── controller/
│   │   │   │   ├── OrderCommandController.java  # 写端点（委托 OrderCommandAssembler 转换命令）
│   │   │   │   └── OrderQueryController.java    # 读端点（基于 OrderListQuery）
│   │   │   ├── assembler/
│   │   │   │   └── OrderCommandAssembler.java   # Request DTO → Command 转换
│   │   │   ├── dto/request/
│   │   │   │   ├── CreateOrderRequest.java
│   │   │   │   ├── CancelOrderRequest.java      # 取消原因（record + @NotBlank）
│   │   │   │   ├── RefundOrderRequest.java      # 退款原因（record + @NotBlank）
│   │   │   │   └── QueryOrderRequest.java
│   │   ├── job/                             # 定时任务
│   │   │   ├── OrderTimeoutTask.java        # 订单超时取消
│   │   │   └── OrderAutoConfirmTask.java    # 自动确认收货
│   └── outbound/
│       ├── persistence/                     # 持久化
│       │   ├── OrderRepositoryImpl.java
│       │   ├── OrderQueryRepositoryImpl.java
│       │   ├── OrderDO.java
│       │   ├── OrderMapper.java
│       │   ├── OrderItemDO.java             # eo_order_item 实体
│       │   ├── OrderItemMapper.java         # 行项 MyBatis Mapper
│       │   ├── OrderDataMapper.java        # MapStruct: DO ↔ Domain
│       │   ├── cache/                           # 缓存
│       │   ├── RedisOrderCacheAdapter.java  # 实现 OrderCachePort
│       │   └── OrderCacheConstant.java
│       └── config/
│           └── OrderTimeoutProperties.java  # 超时配置
├── application/
│   ├── command/                             # 命令（CQRS Write，全部命令由 Handler 收口）
│   │   ├── OrderCommandHandler.java         # 订单命令唯一执行器（创建/支付/取消/发货/确认收货/退款）
│   │   ├── OrderPreparation.java            # 创建流水线的订单项准备组件（校验/构建）
│   │   ├── CreateOrderCommand.java / CreateOrderResult.java
│   │   ├── PayOrderCommand.java
│   │   ├── CancelOrderCommand.java
│   │   ├── ShipOrderCommand.java
│   │   ├── ConfirmReceiptCommand.java
│   │   └── RefundOrderCommand.java
│   ├── query/                               # 查询 (CQRS Read)
│   │   ├── OrderQueryHandler.java
│   │   ├── OrderListQuery.java              # record 收敛查询参数（orderNo, status: OrderStatus, buyerId, sellerId, pageNum, pageSize）
│   │   ├── readmodel/
│   │   │   ├── OrderReadModel.java
│   │   │   └── OrderItemReadModel.java
│   │   └── assembler/
│   │       └── OrderReadModelAssembler.java  # ReadModel → OrderVO（应用层组装）
│   ├── port/query/
│   │   └── OrderQueryRepository.java        # 读仓储（countByStatus 入参为 OrderStatus 枚举）
│   ├── event/                               # 事件订阅
│   │   └── OrderLifecycleEventConsumer.java # RabbitMQ 订单生命周期消费者（取消/退款恢复库存, 完成标记售出）
│   └── dto/
│       └── OrderVO.java                      # 响应 VO
├── domain/
│   ├── aggregate/
│   │   ├── Order.java             # 订单聚合根（不可变，字段 final）
│   │   ├── OrderCreateSpec.java            # record 收敛 createOrder() 工厂参数
│   │   └── OrderReconstructSpec.java       # record 收敛 from() 重建参数
│   ├── valueobject/
│   │   ├── OrderId.java, OrderNo.java
│   │   ├── Address.java, Phone.java
│   │   ├── UserId.java
│   │   ├── OrderItem.java                 # 行项值对象（含 ProductSnapshot）
│   │   ├── ProductSnapshot.java           # 下单时商品快照
│   │   └── PaymentStatus.java             # 支付状态枚举（UNPAID/PAID/REFUNDED）
│   ├── event/
│   │   ├── OrderEvent.java                   # sealed 接口（含 default aggregateId），所有事件实现此接口
│   │   ├── OrderCreatedEvent.java
│   │   ├── OrderPaidEvent.java
│   │   ├── OrderShippedEvent.java
│   │   ├── OrderCompletedEvent.java
│   │   ├── OrderCancelledEvent.java
│   │   └── OrderRefundedEvent.java
│   ├── port/                              # 出站端口
│   │   ├── OrderCachePort.java             # 缓存端口
│   │   ├── ProductInventoryPort.java       # 订单生命周期产品操作端口
│   │   ├── ProductQueryPort.java           # 商品查询端口
│   │   ├── PaymentGatewayPort.java         # 支付网关端口
│   │   ├── UserInfoPort.java              # 用户信息端口
│   │   └── OrderQueryCondition.java        # record 查询条件（status 为 OrderStatus 枚举）
│   ├── repository/                         # 仓储接口
│   │   └── OrderRepository.java            # 写仓储
│   ├── constant/
│   │   ├── OrderConstant.java
│   │   ├── OrderStatus.java                # 状态枚举：code 为 String（"PENDING_PAYMENT"/"PAID"/...）
│   │   ├── OrderAction.java                # 状态机唯一事实来源：动作（前置状态→目标状态+支付副作用）
│   │   └── OrderResultCode.java
│   └── exception/
│       ├── OrderDomainException.java
│       ├── OrderPermissionException.java
│       └── OrderOperationException.java
```

> **跨模块适配器位置**：order 模块定义的 `ProductInventoryPort` / `ProductQueryPort` / `PaymentGatewayPort` 的实现不在 order 模块内，而在 `easyorange-application/adapter/outbound/` 下：`product/ProductInventoryAdapter`、`product/ProductQueryAdapter`、`payment/OrderPaymentGatewayAdapter`。`OrderCachePort` 的实现 `RedisOrderCacheAdapter` 位于 order 模块自身 `adapter/outbound/cache/`，因其仅操作订单域缓存。Maven 依赖标记 `<optional>true</optional>` 实现编译期隔离。

> **Money 值对象**：`Money` 不在 order 模块，位于 `easyorange-common`。order 模块通过 `Money` 使用金额，但不重复定义。
> **ProductId 值对象**：`ProductId` 同样位于 `easyorange-common`（`common/domain/ProductId.java`，与 `Money` 同模式，带 `@JsonValue`/`@JsonCreator`），order 与 product 模块共享同一实现，不各自重复定义（2026-08-08 收敛）。

## 下单链路（拒绝 Saga）

创建订单不使用 Saga 编排，采用**本地单事务 + 分布式锁 + Outbox 事件**。语义见 [ADR-0007](../../doc/adr/0007-order-local-tx-over-saga.md)：**原子性由单 `@Transactional` 回滚兜底，失败随事务整体回滚，无需反向补偿**（单库场景下补偿与回滚重复、失败状态随事务回滚丢失）。

**执行流程**：
```
OrderCommandHandler.handle(CreateOrderCommand) ─ @Transactional(rollbackFor=Exception.class) ─
  1. DistributedLockPort 获取商品锁（key=eo:order:lock:product:{productId}，按 productId 排序避免死锁）
  2. OrderPreparation 准备商品数据（校验在线、库存、非自购）
  3. Order.createOrder 创建订单 + 发布事件（Outbox 同事务原子）
  4. ProductInventoryPort.decreaseStock() 同步扣库存（同事务）
  5. PaymentGatewayPort 创建支付记录（同事务）
  6. 任一步失败 → 业务事务整体回滚，抛 OrderCreationException（库存/支付同事务回滚，无补偿路径）
```

**库存恢复**：仅由 `OrderLifecycleEventConsumer` 消费订单取消/退款事件时调用 `ProductInventoryPort.restoreStock()` 恢复；完成事件触发 `markAsSold`。

## CQRS 架构

**Command 侧**: `OrderCommandController` → `OrderCommandHandler` → `Order` → `OrderRepository`

**Query 侧**: `OrderQueryController` → `OrderQueryHandler` → `OrderQueryRepository` → `OrderReadModel`

## 对象映射策略

模块内有两层映射（与 User 模块一致），职责分离：

| Mapper | 方向 | 位置 | 说明 |
|--------|------|------|------|
| `OrderDataMapper` | DO ↔ Domain | `adapter/outbound/persistence/` | MapStruct 接口：OrderDO ↔ Order、OrderItemDO ↔ OrderItem |
| `OrderReadModelAssembler` | ReadModel → VO | `application/query/assembler/` | OrderReadModel → OrderVO（含脱敏、商品信息填充） |

`OrderDO` 是纯数据库实体，不含映射逻辑。所有持久化映射集中在 `OrderDataMapper`。

## 跨模块通信

通过 `port/` 接口解耦，`adapter/outbound/messaging/` 实现适配器：

| 端口 | 适配器 | 目标模块 |
|------|--------|---------|
| `ProductInventoryPort` | `ProductInventoryAdapter` | product |
| `ProductQueryPort` | `ProductQueryAdapter` | product |
| `PaymentGatewayPort` | `OrderPaymentGatewayAdapter` | payment |
| `OrderCachePort` | `RedisOrderCacheAdapter` | Redis |

所有跨模块依赖已标记为 `<optional>true</optional>`，通过 Port 接口 + 适配器模式完全隔离。

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
- 状态码使用 String code（`OrderStatus.PENDING_PAYMENT.getCode()` → `"PENDING_PAYMENT"`），经 `@EnumValue` 注解完成 VARCHAR 列互转，详见下方「枚举字符串化」章节。

## 定时任务

- `OrderTimeoutTask`: 未支付订单超时自动取消
- `OrderAutoConfirmTask`: 已发货订单超时自动确认收货

## 常见开发任务

### 添加订单新转换

1. 在 `OrderAction` 枚举新增动作（声明前置状态、目标状态、目标支付状态、是否需要原因、错误码）
2. `Order` 添加转换方法，内部委托 `transitionTo(新动作, reason)` 并构造对应领域事件（返回 `Transition<Order, XxxEvent>`）
3. 添加对应领域事件
4. `OrderCommandHandler` 添加命令处理（命令为 record）
5. 如涉及下单链路，检查 `OrderCommandHandler.handle(CreateOrderCommand)` 执行顺序与事务回滚语义（单事务内，无需补偿）
6. Flyway 迁移：`status` 列 CHECK 约束追加新 code
7. 在 `OrderActionTest` 中补充前置状态/目标状态断言，`OrderTest` 补充转换用例

### 添加新查询维度

1. `OrderListQuery` record 添加字段
2. 请求 DTO `adapter/inbound/web/dto/request/` 添加字段
3. Controller 提取参数构造 `OrderListQuery` 传给 `OrderQueryHandler.listOrders()`
4. `OrderQueryRepository` 修改查询
5. `OrderReadModel` 添加字段
6. `OrderReadModelAssembler` 更新
7. 测试

## 枚举字符串化

`OrderStatus` / `PaymentStatus` 的 `code` 字段为 String（非 Integer），统一全链路字符串化：

- **DB 层**：`eo_order.status` / `eo_order.payment_status` 为 `VARCHAR(20)`，带 CHECK 约束
- **MyBatis**：枚举 `code` 字段标 `@EnumValue`，内置 `MybatisEnumTypeHandler` 完成 enum ↔ String 互转
- **领域层**：`Order` / `OrderReconstructSpec` 直接使用枚举类型，无 String.valueOf 转换
- **读模型 / VO**：`OrderReadModel` / `OrderVO` 的 status 字段为 `String code`
- **JSON 序列化**：`@JsonValue` 标注在 `code` 上，前端收到的就是 `"PENDING_PAYMENT"` 而非 `0`

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
