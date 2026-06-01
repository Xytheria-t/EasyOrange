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
│   │       ├── OrderRefundedEventSubscriber.java
│   │       └── OrderNotificationEventSubscriber.java
│   └── outbound/
│       ├── persistence/                     # 持久化
│       │   ├── MybatisOrderRepository.java
│       │   ├── MybatisOrderReadRepository.java
│       │   ├── SagaRepositoryImpl.java
│       │   ├── OrderDO.java, SagaDO.java
│       │   ├── OrderMapper.java, SagaMapper.java
│       │   ├── OrderItemDO.java             # eo_order_item 实体
│       │   ├── OrderItemMapper.java         # 行项 MyBatis Mapper
│       │   └── OrderDataConverter.java
│       ├── cache/                           # 缓存
│       │   └── RedisOrderCacheAdapter.java  # 实现 OrderCachePort
│       └── messaging/                       # 跨模块适配器
│           ├── ProductInventoryAdapter.java  # → product 扣减库存
│           ├── ProductQueryAdapter.java      # → product 查询
│           ├── PaymentGatewayAdapter.java    # → payment 创建支付
│           └── UserInfoAdapter.java          # → user 查询用户
├── application/
│   ├── saga/                                 # Saga 编排（应用层）
│   │   └── CreateOrderSaga.java            # 创建订单 Saga 编排
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
│   │   └── OrderQuery.java
│   ├── assembler/
│   │   └── OrderVOAssembler.java
│   └── dto/
│       └── OrderVO.java                      # 响应 VO（与缓存系统耦合，暂留 application）
├── domain/
│   ├── aggregate/
│   │   └── OrderAggregate.java             # 订单聚合根 (不可变)
│   ├── saga/                                 # Saga 支持类型（纯领域）
│   │   ├── SagaRepository.java            # Saga 仓储接口
│   │   ├── SagaState.java, SagaStatus.java
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
│   │   ├── OrderCreatedEvent.java
│   │   ├── OrderPaidEvent.java
│   │   ├── OrderShippedEvent.java
│   │   ├── OrderCompletedEvent.java
│   │   ├── OrderCancelledEvent.java
│   │   └── OrderRefundedEvent.java
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

创建订单使用 Saga 编排分布式事务：

```
CreateOrderSaga.execute():
  1. 创建订单 (OrderAggregate + DomainEventPublisher)
     ├── 遍历 items，逐项调用 ProductInventoryPort.getSnapshot()
     │   └── 校验：商品在线、非自购、库存充足
     ├── 批量调用 ProductQueryPort.getProductsByIds() 加载商品详情
     │   └── 填充 ProductSnapshot（name/image/description/conditionLevel）
     ├── 计算 totalAmount
     ├── 保存 OrderAggregate + 批量插入 OrderItem
     └── 发布 OrderCreatedEvent（附带 items 列表）
  2. 创建支付 (PaymentGatewayPort)    ← 补偿: 取消订单
  失败时按逆序执行补偿操作
```

- `SagaState` 持久化到 `eo_saga` 表，支持故障恢复
- `SagaStatus`: PENDING → EXECUTING → COMPENSATING → COMPLETED/FAILED

## CQRS 架构

**Command 侧**: `OrderCommandController` → `OrderCommandHandler` → `OrderAggregate` → `OrderRepository`

**Query 侧**: `OrderQueryController` → `OrderQueryHandler` → `OrderReadRepository` → `OrderReadModel`

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
6. `OrderVOAssembler` 更新
7. 测试
