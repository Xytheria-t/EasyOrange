# easyorange-payment 模块指南

支付处理模块，DDD + CQRS 架构，处理支付全流程、退款、幂等、分布式锁。

## 目录结构

```
payment/
├── adapter/
│   ├── inbound/web/
│   │   ├── controller/
│   │   │   ├── PaymentCommandController.java    # 支付写端点
│   │   │   ├── PaymentQueryController.java      # 支付读端点
│   │   │   ├── MockPaymentController.java       # 模拟支付 (开发环境)
│   │   ├── assembler/PaymentCommandAssembler.java
│   │   ├── request/                         # CreatePaymentRequest, RefundRequest, etc.
│   │   └── response/                        # PaymentResponse, PaymentConfigResponse
│   └── outbound/
│       ├── gateway/
│       │   └── PaymentGatewayAdapter.java   # 支付网关适配器 (实现 PaymentGatewayPort)
│               └── persistence/
│           ├── MybatisPaymentRepository.java
│           ├── MybatisIdempotencyKeyRepository.java
│           ├── PaymentConfigRepository.java
│           ├── converter/
│           │   └── PaymentConverter.java
│           ├── mapper/                      # PaymentMapper, IdempotencyKeyMapper, PaymentConfigMapper
│           └── persistence/                # PaymentDO, IdempotencyKeyDO, PaymentConfigDO
├── application/
│   ├── command/                             # 命令 (CQRS Write)
│   │   ├── PaymentCommandHandler.java
│   │   ├── CreatePaymentCommand.java
│   │   ├── PayCommand.java
│   │   ├── RefundPaymentCommand.java
│   │   └── ClosePaymentCommand.java
│   ├── query/                               # 查询 (CQRS Read)
│   │   └── PaymentQueryHandler.java
│   ├── event/                    # [空] 事件处理已迁移到 RabbitMQ 消费者
│   ├── idempotency/
│   │   └── IdempotencyService.java          # 幂等服务 (SHA-256 请求哈希)
│   ├── lock/
│   │   └── DistributedLockWrapper.java      # 分布式锁封装
│   ├── metrics/
│   │   ├── PaymentMetricsService.java       # 支付指标
│   │   └── PaymentMetricsListener.java
│   └── mock/
│       └── MockPaymentUseCase.java          # 模拟支付用例
├── domain/
│   ├── aggregate/
│   │   └── PaymentAggregate.java            # 支付聚合根
│   ├── factory/
│   │   └── PaymentFactory.java              # 支付工厂
│   ├── saga/
│   │   ├── SagaOrchestrator.java            # Saga 编排器
│   │   ├── SagaStepResult.java
│   │   └── SagaExecutionException.java
│   ├── specification/
│   │   └── PaymentSpecification.java        # 支付规格 (业务规则)
│   ├── valueobject/
│   │   ├── PaymentId.java, PaymentNo.java
│   │   ├── PaymentAmount.java
│   │   ├── IdempotencyKey.java
│   │   └── PaymentMethodInfo.java
│   ├── event/
│   │   ├── PaymentCreatedEvent.java
│   │   ├── PaymentSucceededEvent.java
│   │   ├── PaymentFailedEvent.java
│   │   ├── PaymentRefundedEvent.java
│   │   ├── PaymentClosedEvent.java
│   │   └── CompensationFailedAlertEvent.java  # 补偿失败告警事件
│   ├── port/
│   │   ├── PaymentGatewayPort.java          # 支付网关端口
│   │   ├── IdempotencyKeyRepositoryPort.java # 幂等键仓储端口
│   │   ├── CallbackSignatureVerifierPort.java # 回调签名验证端口
│   │   ├── PaymentQueryRepositoryPort.java  # 查询仓储端口（已从 repository/ 迁入）
│   │   ├── PaymentResult.java, RefundResult.java
│   ├── repository/
│   │   └── PaymentRepositoryPort.java       # 支付仓储端口
│   ├── constant/
│   │   ├── PaymentStatus.java
│   │   ├── PaymentMethod.java
│   │   └── PaymentResultCode.java
│   └── exception/
│       ├── PaymentDomainException.java     # 统一支付异常（含 of()工厂方法，覆盖 notFound/invalidStatus/gateway 等场景）
│       └── SagaCompensationFailedException.java  # Saga 补偿失败异常（含 CompensationFailure 列表）
└── constant/
    └── PaymentConstant.java
```

## 领域事件

领域事件通过 `DomainEventPublisher` 发布到 RabbitMQ Topic Exchange（`eo.domain.events`），由各模块 `@RabbitListener` 消费者异步处理。

## 幂等保护

- `IdempotencyService` 对请求计算 SHA-256 哈希
- 幂等键存储在 `eo_idempotency_key` 表
- 重复请求直接返回之前的结果

## 分布式锁

- `DistributedLockWrapper` 封装 Redis 分布式锁
- 支付创建、退款等关键操作加锁防止并发冲突

## 支付状态机（不可变聚合根）

所有状态转换返回 Result record（新聚合根实例 + 领域事件），不修改自身。

```
PENDING → PAYING → SUCCESS
  ↓         ↓        ↓
CLOSED    FAILED   REFUNDING → REFUNDED
                      ↓
                PARTIALLY_REFUNDED
                      ↓ (compensation)
                    SUCCESS
```

- 两阶段支付：`preparePay()` → `PayPreparedResult` → 网关调用 → `confirmPay(PaymentResult)` → `PayConfirmedResult`
- 两阶段退款：`prepareRefund()` → `RefundPreparedResult` → 网关调用 → `confirmRefund(RefundResult)` → `RefundConfirmedResult`
- Saga 补偿：`cancelPay()` / `cancelRefund()` 回退到前一状态
- **补偿失败处理**：补偿操作失败时抛出 `SagaCompensationFailedException`，发布 `CompensationFailedAlertEvent` 告警事件，不会被静默吞掉
- Guard 方法：`canPay()` / `canRefund()` / `canClose()` / `canFail()` / `canConfirmPay()` / `canConfirmRefund()`

## 常见开发任务

### 添加新支付方式

1. `PaymentMethod` 枚举新增值
2. `PaymentGatewayAdapter` 添加新网关调用逻辑
3. `PaymentFactory` 适配新支付方式
4. 添加模拟支付支持 (MockPaymentUseCase)
5. Flyway 迁移（如需新表/字段）
6. 测试

### 添加新支付事件

1. 创建事件 record 实现 `DomainEvent`
2. 在状态转换方法中，在 Result record 的 `event()` 中返回事件
3. Handler 通过 `domainEventPublisher.publish(result.event())` 发布
4. 在 `RoutingKeyResolver.EVENT_ROUTING_KEYS` 注册路由键（`payment.{aggregate}.{event}`）
5. 创建 `@RabbitListener` 消费者处理事件
6. 测试

## 安全要点

- 支付回调必须验签 (`CallbackSignatureVerifierPort`)
- 金额使用 `PaymentAmount` 值对象，避免浮点精度问题
- 版本号乐观锁（`PaymentDO` 上的 `@Version` 注解 + 聚合根内手动递增 `int version`）
- 幂等保护防止重复支付/退款
