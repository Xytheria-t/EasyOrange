# easyorange-payment 模块指南

支付处理模块，DDD + CQRS + Outbox 架构，处理支付全流程、退款、幂等、分布式锁。

## 目录结构

```
payment/
├── adapter/
│   ├── inbound/web/
│   │   ├── PaymentCommandController.java    # 支付写端点
│   │   ├── PaymentQueryController.java      # 支付读端点
│   │   ├── MockPaymentController.java       # 模拟支付 (开发环境)
│   │   ├── assembler/PaymentCommandAssembler.java
│   │   ├── request/                         # CreatePaymentRequest, RefundRequest, etc.
│   │   └── response/                        # PaymentResponse, PaymentConfigResponse
│   └── outbound/
│       ├── gateway/
│       │   └── PaymentGatewayAdapter.java   # 支付网关适配器 (实现 PaymentGatewayPort)
│       └── persistence/
│           ├── MybatisPaymentRepository.java
│           ├── MybatisIdempotencyKeyRepository.java
│           ├── JdbcDomainEventStore.java    # Outbox 事件存储 (委托 Framework OutboxRepository)
│           ├── PaymentConfigRepository.java
│           ├── converter/
│           │   └── PaymentConverter.java
│           ├── mapper/                      # PaymentMapper, IdempotencyKeyMapper, PaymentConfigMapper
│           └── po/                          # PaymentPO, IdempotencyKeyPO, PaymentConfigPO
├── application/
│   ├── command/                             # 命令 (CQRS Write)
│   │   ├── PaymentCommandHandler.java
│   │   ├── CreatePaymentCommand.java
│   │   ├── PayCommand.java
│   │   ├── RefundPaymentCommand.java
│   │   └── ClosePaymentCommand.java
│   ├── query/                               # 查询 (CQRS Read)
│   │   ├── PaymentQueryHandler.java
│   │   ├── PaymentQuery.java
│   │   └── PaymentView.java
│   ├── event/
│   │   ├── OutboxEventPublisher.java        # Outbox 模式事件发布
│   │   └── PaymentEventListener.java        # 领域事件持久化到 Outbox
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
│   │   └── PaymentMethodVO.java
│   ├── event/
│   │   ├── PaymentCreatedEvent.java
│   │   ├── PaymentSucceededEvent.java
│   │   ├── PaymentFailedEvent.java
│   │   ├── PaymentRefundedEvent.java
│   │   └── PaymentClosedEvent.java
│   ├── port/output/
│   │   ├── PaymentRepositoryPort.java       # 支付仓储端口
│   │   ├── PaymentQueryRepositoryPort.java  # 查询仓储端口
│   │   ├── PaymentGatewayPort.java          # 支付网关端口
│   │   ├── DomainEventStorePort.java        # 事件存储端口 (Outbox, 使用 Framework OutboxMessage)
│   │   ├── IdempotencyKeyRepositoryPort.java # 幂等键仓储端口
│   │   ├── CallbackSignatureVerifierPort.java # 回调签名验证端口
│   │   ├── PaymentResult.java, RefundResult.java
│   ├── constant/
│   │   ├── PaymentStatus.java
│   │   ├── PaymentMethod.java
│   │   └── PaymentResultCode.java
│   └── exception/
│       ├── PaymentDomainException.java
│       ├── PaymentNotFoundException.java
│       ├── PaymentInvalidStatusException.java
│       ├── PaymentGatewayException.java
│       ├── OptimisticLockException.java
│       ├── RefundNotAllowedException.java
│       └── CallbackSignInvalidException.java
├── constant/
│   └── PaymentConstant.java
└── infrastructure/
    └── security/
        └── CallbackSignatureVerifier.java   # 回调签名验证实现
```

## Outbox 模式

保证领域事件可靠投递：

1. 业务操作与事件存储在同一事务中 (`DomainEventStorePort`)
2. `OutboxEventPublisher` 异步扫描未投递事件并发布
3. 事件存储在 `eo_domain_event` 表，状态: PENDING → PUBLISHED → FAILED
4. 事件实体统一使用 Framework 模块的 `OutboxMessage`

## 幂等保护

- `IdempotencyService` 对请求计算 SHA-256 哈希
- 幂等键存储在 `eo_idempotency_key` 表
- 重复请求直接返回之前的结果

## 分布式锁

- `DistributedLockWrapper` 封装 Redis 分布式锁
- 支付创建、退款等关键操作加锁防止并发冲突

## 支付状态机

```
PENDING → PROCESSING → SUCCEEDED
  ↓           ↓
CLOSED     FAILED
  ↓
REFUNDED (部分/全额)
```

## 常见开发任务

### 添加新支付方式

1. `PaymentMethod` 枚举新增值
2. `PaymentGatewayAdapter` 添加新网关调用逻辑
3. `PaymentFactory` 适配新支付方式
4. 添加模拟支付支持 (MockPaymentUseCase)
5. Flyway 迁移（如需新表/字段）
6. 测试

### 添加新支付事件

1. 创建事件类继承 `BaseDomainEvent`
2. `PaymentAggregate` 中发布事件
3. `PaymentEventListener` 自动持久化到 Outbox
4. 测试

## 安全要点

- 支付回调必须验签 (`CallbackSignatureVerifierPort`)
- 金额使用 `PaymentAmount` 值对象，避免浮点精度问题
- 乐观锁 (`@Version`) 防止并发修改
- 幂等保护防止重复支付/退款
