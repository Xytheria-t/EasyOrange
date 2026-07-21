# easyorange-order 模块指南

订单管理模块，DDD + CQRS + Saga 架构，处理订单全生命周期。

## 目录结构

```
order/
├── adapter/
│   ├── inbound/
│   │   ├── web/
│   │   │   ├── controller/
│   │   │   │   ├── OrderCommandController.java  # 写端点
│   │   │   │   └── OrderQueryController.java    # 读端点
│   │   │   ├── dto/request/
│   │   │   │   ├── CreateOrderRequest.java
│   │   │   │   └── QueryOrderRequest.java
│   │   ├── job/                             # 定时任务
│   │   │   ├── OrderTimeoutTask.java        # 订单超时取消
│   │   │   └── OrderAutoConfirmTask.java    # 自动确认收货
│   │   └── mq/subscriber/                   # 事件订阅
│   │       ├── OrderCreatedEventSubscriber.java
│   │       ├── OrderCancelledEventSubscriber.java
│   │       ├── OrderCompletedEventSubscriber.java
│   │       └── OrderRefundedEventSubscriber.java
│   └── outbound/
│       ├── persistence/                     # 持久化
│       │   ├── MybatisOrderRepository.java
│       │   ├── MybatisOrderReadRepository.java
│       │   ├── SagaRepositoryImpl.java
│       │   ├── OrderDO.java, SagaDO.java
│       │   ├── OrderMapper.java, SagaMapper.java
│       │   ├── OrderItemDO.java             # eo_order_item 实体
│       │   ├── OrderItemMapper.java         # 行项 MyBatis Mapper
│       │   └── OrderEntityMapper.java       # MapStruct: DO ↔ Domain
│       ├── cache/                           # 缓存
│       │   └── RedisOrderCacheAdapter.java  # 实现 OrderCachePort
│       └── messaging/                       # 跨模块适配器
│           ├── ProductInventoryAdapter.java  # → product 扣减库存
│           ├── ProductQueryAdapter.java      # → product 查询
│           ├── PaymentGatewayAdapter.java    # → payment 创建支付
│           └── UserInfoAdapter.java          # → user 查询用户
├── application/
│   ├── saga/                                 # Saga 编排（应用层）
│   │   ├── CreateOrderSaga.java            # 创建订单 Saga 编排（重构后仅 157 行）
│   │   └── support/                         # Saga 支持类（职责分离）
│   │       ├── DistributedLockManager.java  # 分布式锁管理
│   │       ├── SagaCoordinator.java         # Saga 状态管理
│   │       ├── OrderCompensationService.java # 订单补偿操作
│   │       ├── OrderPreparationService.java  # 商品数据准备
│   │       └── OrderCreationExecutor.java    # 订单创建执行
│   ├── command/                             # 命令 (CQRS Write)
│   │   ├── OrderCommandHandler.java
│   │   ├── CreateOrderCommand.java / CreateOrderResult.java
│   │   ├── PayOrderCommand.java
│   │   ├── CancelOrderCommand.java
│   │   ├── ShipOrderCommand.java
│   │   ├── ConfirmReceiptCommand.java
│   │   └── RefundOrderCommand.java
│   ├── query/                               # 查询 (CQRS Read)
│   │   ├── OrderQueryHandler.java
│   │   ├── OrderQuery.java
│   │   └── assembler/
│   │       └── OrderReadModelAssembler.java  # ReadModel → OrderVO（应用层组装）
│   └── dto/
│       └── OrderVO.java                      # 响应 VO
├── domain/
│   ├── aggregate/
│   │   └── OrderAggregate.java             # 订单聚合根 (不可变)
│   ├── saga/                                 # Saga 支持类型（纯领域）
│   │   ├── SagaRepository.java            # Saga 仓储接口
│   │   ├── SagaState.java, SagaStatus.java
│   │   ├── SagaException.java              # Saga 异常（含 sagaId/state 字段，涵盖锁获取/序列化/补偿场景）
│   │   ├── PaymentGatewayAdapterException.java    # 支付网关异常
│   │   └── OrderCreationException.java
│   ├── valueobject/
│   │   ├── OrderId.java, OrderNo.java
│   │   ├── Money.java
│   │   ├── Address.java, Phone.java
│   │   ├── ProductId.java, UserId.java
│   │   ├── OrderItem.java                 # 行项值对象（含 ProductSnapshot）
│   │   ├── ProductSnapshot.java           # 下单时商品快照
│   │   ├── PaymentStatus.java             # 支付状态枚举（UNPAID/PAID/REFUNDED）
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
│   │   └── OrderQueryCondition.java
│   ├── repository/                         # 仓储接口
│   │   ├── OrderRepository.java            # 写仓储
│   │   └── OrderReadRepository.java        # 读仓储
│   ├── constant/
│   │   ├── OrderConstant.java
│   │   ├── OrderStatus.java
│   │   └── OrderResultCode.java
│   └── exception/
│       ├── OrderDomainException.java
│       ├── OrderNotFoundException.java
│       ├── OrderStatusException.java
│       ├── OrderPermissionException.java
│       └── OrderOperationException.java
└── config/
    └── OrderTimeoutProperties.java         # 超时配置
```

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
DRAFT → PAID → SHIPPED → COMPLETED
  ↓       ↓       ↓
CANCELLED CANCELLED REFUNDED
```

## 定时任务

- `OrderTimeoutTask`: 未支付订单超时自动取消
- `OrderAutoConfirmTask`: 已发货订单超时自动确认收货

## 常见开发任务

### 添加订单新状态

1. `OrderStatus` 枚举新增值
2. `OrderAggregate` 添加状态转换方法和校验
3. 添加对应领域事件
4. `OrderCommandHandler` 添加命令处理
5. 更新 Saga 补偿逻辑（如需）
6. Flyway 迁移
7. 测试

### 添加新查询维度

1. `OrderQuery` 添加字段
2. 请求 DTO `adapter/inbound/web/dto/request/` 添加字段
3. Controller 提取参数传递原始类型给 `OrderQueryHandler`
4. `OrderReadRepository` 修改查询
5. `OrderReadModel` 添加字段
6. `OrderReadModelAssembler` 更新
7. 测试
