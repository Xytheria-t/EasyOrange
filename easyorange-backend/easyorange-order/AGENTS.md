# easyorange-order 模块指南

订单管理模块，DDD + CQRS + Saga 架构，处理订单全生命周期。

## 目录结构

```
order/
├── adapter/
│   ├── inbound/
│   │   ├── web/
│   │   │   ├── controller/
│   │   │   │   ├── OrderCommandController.java  # 写端点（@Validated 触发 Bean Validation）
│   │   │   │   └── OrderQueryController.java    # 读端点（基于 OrderListQuery）
│   │   │   ├── dto/request/
│   │   │   │   ├── CreateOrderRequest.java
│   │   │   │   └── QueryOrderRequest.java
│   │   ├── job/                             # 定时任务
│   │   │   ├── OrderTimeoutTask.java        # 订单超时取消
│   │   │   └── OrderAutoConfirmTask.java    # 自动确认收货
│   │   └── mq/subscriber/                   # 事件订阅
│   │       └── OrderSagaEventConsumer.java  # 单一 Saga 事件消费者（替代多个分散 Subscriber）
│   └── outbound/
│       ├── persistence/                     # 持久化
│       │   ├── MybatisOrderRepository.java
│       │   ├── MybatisOrderReadRepository.java
│       │   ├── SagaRepositoryImpl.java
│       │   ├── OrderDO.java, SagaDO.java
│       │   ├── OrderMapper.java, SagaMapper.java
│       │   ├── OrderItemDO.java             # eo_order_item 实体
│       │   ├── OrderItemMapper.java         # 行项 MyBatis Mapper
│       │   ├── OrderEntityMapper.java       # MapStruct: DO ↔ Domain
│       │   └── typehandler/                 # OrderStatusTypeHandler, PaymentStatusTypeHandler（VARCHAR ↔ 枚举）
│       ├── cache/                           # 缓存
│       │   ├── RedisOrderCacheAdapter.java  # 实现 OrderCachePort
│       │   └── OrderCacheConstant.java
│       └── config/
│           └── OrderTimeoutProperties.java  # 超时配置
├── application/
│   ├── saga/                                 # Saga 编排（应用层）
│   │   ├── CreateOrderSaga.java            # 创建订单 Saga 编排（重构后仅 157 行）
│   │   └── support/                         # Saga 支持类（职责分离）
│   │       ├── DistributedLockManager.java  # 分布式锁管理
│   │       ├── SagaCoordinator.java         # Saga 状态管理
│   │       ├── OrderCompensationService.java # 订单补偿操作
│   │       ├── OrderPreparationService.java  # 商品数据准备
│   │       └── OrderCreationExecutor.java    # 订单创建执行
│   ├── command/                             # 命令（CQRS Write，sealed OrderCommand 接口）
│   │   ├── OrderCommandHandler.java
│   │   ├── OrderCommand.java                 # sealed 接口，permits 7 个命令 record
│   │   ├── CreateOrderCommand.java / CreateOrderResult.java
│   │   ├── PayOrderCommand.java
│   │   ├── CancelOrderCommand.java
│   │   ├── ShipOrderCommand.java
│   │   ├── ConfirmReceiptCommand.java
│   │   └── RefundOrderCommand.java
│   ├── query/                               # 查询 (CQRS Read)
│   │   ├── OrderQueryHandler.java
│   │   ├── OrderListQuery.java              # record 收敛查询参数（orderNo, status: OrderStatus, buyerId, sellerId, pageNum, pageSize）
│   │   └── assembler/
│   │       └── OrderReadModelAssembler.java  # ReadModel → OrderVO（应用层组装）
│   └── dto/
│       └── OrderVO.java                      # 响应 VO
├── domain/
│   ├── aggregate/
│   │   ├── OrderAggregate.java             # 订单聚合根（不可变，字段 final）
│   │   ├── OrderCreateSpec.java            # record 收敛 createOrder() 工厂参数
│   │   └── OrderReconstructSpec.java       # record 收敛 from() 重建参数
│   ├── saga/                                 # Saga 支持类型（纯领域）
│   │   ├── SagaRepository.java            # Saga 仓储接口
│   │   ├── SagaState.java, SagaStatus.java
│   │   ├── SagaException.java              # Saga 异常（含 sagaId/state 字段，涵盖锁获取/序列化/补偿场景）
│   │   ├── PaymentGatewayAdapterException.java    # 支付网关异常
│   │   └── OrderCreationException.java
│   ├── valueobject/
│   │   ├── OrderId.java, OrderNo.java
│   │   ├── Address.java, Phone.java
│   │   ├── ProductId.java, UserId.java
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
│   │   ├── OrderRefundedEvent.java
│   │   └── StockReservationRequestedEvent.java
│   ├── readmodel/
│   │   ├── OrderReadModel.java
│   │   └── OrderItemReadModel.java
│   ├── port/                              # 出站端口
│   │   ├── OrderCachePort.java             # 缓存端口
│   │   ├── ProductInventoryPort.java       # 库存端口
│   │   ├── ProductQueryPort.java           # 商品查询端口
│   │   ├── PaymentGatewayPort.java         # 支付网关端口
│   │   ├── UserInfoPort.java              # 用户信息端口
│   │   └── OrderQueryCondition.java        # record 查询条件（status 为 OrderStatus 枚举）
│   ├── repository/                         # 仓储接口
│   │   ├── OrderRepository.java            # 写仓储
│   │   └── OrderReadRepository.java        # 读仓储（countByStatus 入参为 OrderStatus 枚举）
│   ├── constant/
│   │   ├── OrderConstant.java
│   │   ├── OrderStatus.java                # code 为 String："PENDING_PAYMENT"/"PAID"/"SHIPPED"/...
│   │   └── OrderResultCode.java
│   └── exception/
│       ├── OrderDomainException.java
│       ├── OrderNotFoundException.java
│       ├── OrderStatusException.java
│       ├── OrderPermissionException.java
│       └── OrderOperationException.java
```

> **跨模块适配器位置**：order 模块定义的 `ProductInventoryPort` / `ProductQueryPort` / `PaymentGatewayPort` / `UserInfoPort` / `OrderCachePort` 的实现不在 order 模块内，而在 `easyorange-application/adapter/outbound/` 下：`product/OrderProductInventoryAdapter`、`product/OrderProductQueryAdapter`、`payment/OrderPaymentGatewayAdapter`、`user/OrderUserInfoAdapter`。Maven 依赖标记 `<optional>true</optional>` 实现编译期隔离。

> **Money 值对象**：`Money` 不在 order 模块，位于 `easyorange-common`。order 模块通过 `Money` 使用金额，但不重复定义。

## Saga 模式

创建订单使用 Saga 编排分布式事务，已重构为职责分离架构：

**架构改进**：
- CreateOrderSaga 从 327 行减至 157 行，依赖从 10 个减至 4 个
- 分布式锁、状态管理、补偿逻辑、订单准备分离到独立支持类
- 异常处理从 broad catch 改为具体异常类型（SagaException、PaymentGatewayAdapterException 等）

**执行流程**：
```
CreateOrderSaga.execute():
  1. DistributedLockManager 获取商品锁（按 productId 排序避免死锁）
  2. SagaCoordinator 创建初始 Saga 状态
  3. OrderPreparationService 准备商品数据（校验在线、库存、非自购）
  4. OrderCreationExecutor 创建订单 + 发布事件
  5. PaymentGatewayPort 创建支付记录
  6. 失败时 OrderCompensationService 执行补偿（逆序取消订单）
```

- `SagaState` 久化到 `eo_saga` 表，支持故障恢复
- `SagaStatus`: PENDING → ORDER_CREATED → PAYMENT_CREATED → COMPLETED / COMPENSATING → COMPENSATED

## CQRS 架构

**Command 侧**: `OrderCommandController` → `OrderCommandHandler` → `OrderAggregate` → `OrderRepository`

**Query 侧**: `OrderQueryController` → `OrderQueryHandler` → `OrderReadRepository` → `OrderReadModel`

## 对象映射策略

模块内有两层映射（与 User 模块一致），职责分离：

| Mapper | 方向 | 位置 | 说明 |
|--------|------|------|------|
| `OrderEntityMapper` | DO ↔ Domain | `adapter/outbound/persistence/` | MapStruct 接口：OrderDO ↔ OrderAggregate、OrderItemDO ↔ OrderItem |
| `OrderReadModelAssembler` | ReadModel → VO | `application/query/assembler/` | OrderReadModel → OrderVO（含脱敏、商品信息填充） |

`OrderDO` 是纯数据库实体，不含映射逻辑。所有持久化映射集中在 `OrderEntityMapper`。

## 跨模块通信

通过 `port/` 接口解耦，`adapter/outbound/messaging/` 实现适配器：

| 端口 | 适配器 | 目标模块 |
|------|--------|---------|
| `ProductInventoryPort` | `ProductInventoryAdapter` | product |
| `ProductQueryPort` | `ProductQueryAdapter` | product |
| `PaymentGatewayPort` | `PaymentGatewayAdapter` | payment |
| `UserInfoPort` | `UserInfoAdapter` | user |
| `OrderCachePort` | `RedisOrderCacheAdapter` | Redis |

所有跨模块依赖已标记为 `<optional>true</optional>`，通过 Port 接口 + 适配器模式完全隔离。

## 订单状态机

```
PENDING_PAYMENT ──→ PAID ──→ SHIPPED ──→ COMPLETED
       │                │         │
       ↓                ↓         ↓
   CANCELLED        CANCELLED   REFUNDED
```

状态码使用 String code（`OrderStatus.PENDING_PAYMENT.getCode()` → `"PENDING_PAYMENT"`），由 `OrderStatusTypeHandler` / `PaymentStatusTypeHandler` 完成 VARCHAR 列互转，详见下方「枚举字符串化」章节。

## 定时任务

- `OrderTimeoutTask`: 未支付订单超时自动取消
- `OrderAutoConfirmTask`: 已发货订单超时自动确认收货

## 常见开发任务

### 添加订单新状态

1. `OrderStatus` 枚举新增值（`code` 为 String，如 `"EXCHANGED"`）
2. `OrderAggregate` 添加状态转换方法和校验（返回 `OrderTransition<XxxEvent>`）
3. 添加对应领域事件
4. `OrderCommandHandler` 添加命令处理（命令为 record）
5. 更新 Saga 补偿逻辑（如需）
6. Flyway 迁移：`status` 列 CHECK 约束追加新 code
7. 测试

### 添加新查询维度

1. `OrderListQuery` record 添加字段
2. 请求 DTO `adapter/inbound/web/dto/request/` 添加字段
3. Controller 提取参数构造 `OrderListQuery` 传给 `OrderQueryHandler.listOrders()`
4. `OrderReadRepository` 修改查询
5. `OrderReadModel` 添加字段
6. `OrderReadModelAssembler` 更新
7. 测试

## 枚举字符串化

`OrderStatus` / `PaymentStatus` 的 `code` 字段为 String（非 Integer），统一全链路字符串化：

- **DB 层**：`eo_order.status` / `eo_order.payment_status` 为 `VARCHAR(20)`，带 CHECK 约束
- **MyBatis**：`OrderStatusTypeHandler` / `PaymentStatusTypeHandler`（继承 `BaseEnumTypeHandler`）完成 enum ↔ String 互转
- **领域层**：`OrderAggregate` / `OrderReconstructSpec` 直接使用枚举类型，无 String.valueOf 转换
- **读模型 / VO**：`OrderReadModel` / `OrderVO` 的 status 字段为 `String code`
- **JSON 序列化**：`@JsonValue` 标注在 `code` 上，前端收到的就是 `"PENDING_PAYMENT"` 而非 `0`

## Spec Record 与 Command Record

聚合根工厂与重建入口通过 spec record 收敛长参数列表：

| Spec / Command | 用途 | 关键字段 |
|----------------|------|---------|
| `OrderCreateSpec` | `OrderAggregate.createOrder()` 工厂参数 | orderId, buyerId, sellerId, items, address, phone, remark |
| `OrderReconstructSpec` | `OrderAggregate.from()` 重建参数 | id, orderNo, buyerId, sellerId, items, totalAmount, status, paymentStatus, ... |
| `OrderTransition<E>` | 状态转换结果（聚合根新实例 + 领域事件） | aggregate, event |
| `OrderCommand` | sealed 接口（permits 7 个命令 record） | — |
| `CreateOrderCommand` | 创建订单命令（record） | items, address, phone, remark, paymentMethod |
| `PayOrderCommand` / `ShipOrderCommand` / `ConfirmReceiptCommand` | 单字段命令（record） | orderId |
| `CancelOrderCommand` / `RefundOrderCommand` | 带原因命令（record） | orderId, reason |
| `OrderListQuery` | 列表查询参数收敛 | orderNo, status: OrderStatus, buyerId, sellerId, pageNum, pageSize |
