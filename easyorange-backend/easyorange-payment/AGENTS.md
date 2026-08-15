# easyorange-payment 模块指南

支付处理模块，DDD + CQRS 架构，处理支付全流程、退款、幂等、分布式锁。

## 目录结构

```
payment/
├── adapter/
│   ├── inbound/web/
│   │   ├── controller/
│   │   │   ├── PaymentCommandController.java    # 支付写端点（@Validated 触发 Bean Validation）
│   │   │   ├── PaymentQueryController.java      # 支付读端点（基于 PaymentListQuery）
│   │   │   └── MockPaymentController.java       # 模拟支付（开发环境）
│   │   ├── assembler/
│   │   │   ├── PaymentCommandMapper.java        # MapStruct：Request → Command
│   │   │   └── PaymentViewAssembler.java        # Aggregate → Response DTO
│   │   ├── request/                              # CreatePaymentRequest, RefundRequest, PaymentCallback, QueryPaymentRequest, MockPaymentRequest
│   │   └── response/                             # PaymentResponse, PaymentConfigResponse
│   └── outbound/
│       ├── gateway/
│       │   └── PaymentGatewayAdapter.java        # 支付网关适配器（实现 PaymentGatewayPort）
│       ├── persistence/
│       │   ├── PaymentRepositoryImpl.java     # 写仓储（实现 PaymentRepository + PaymentQueryRepository）
│       │   ├── IdempotencyKeyRepositoryImpl.java
│       │   ├── PaymentConfigRepository.java
│       │   ├── converter/
│       │   │   └── PaymentDataMapper.java        # MapStruct：PaymentDO ↔ Payment（基于 PaymentCreateSpec / PaymentReconstructSpec）
│       │   ├── mapper/                            # PaymentMapper, IdempotencyKeyMapper, PaymentConfigMapper
│       │   └── {PaymentDO, IdempotencyKeyDO, PaymentConfigDO}
│       └── security/
│           └── CallbackSignatureVerifier.java    # HMAC-SHA256 回调验签（实现 CallbackSignatureVerifierPort）
├── application/
│   ├── command/                                  # 命令（CQRS Write，sealed PaymentCommand 接口）
│   │   ├── PaymentCommandHandler.java             # 命令分发器（create/pay/close/refund）
│   │   ├── PaymentCommand.java                    # sealed interface，permits 4 个命令 record
│   │   ├── CreatePaymentCommand.java              # @NotBlank orderId / @NotNull @Positive amount / @NotBlank paymentMethod
│   │   ├── PayCommand.java                        # @NotBlank paymentNo
│   │   ├── RefundPaymentCommand.java              # @NotBlank paymentId / @NotNull @Positive refundAmount / @NotBlank refundReason
│   │   └── ClosePaymentCommand.java               # @NotBlank paymentId
│   ├── query/                                    # 查询（CQRS Read）
│   │   ├── PaymentQueryHandler.java
│   │   └── PaymentListQuery.java                  # record 收敛查询参数（userId, status: PaymentStatus, pageNum, pageSize）
│   ├── port/query/
│   │   └── PaymentQueryRepository.java            # 读仓储（status 参数为 PaymentStatus 枚举，类型安全）
│   ├── idempotency/
│   │   └── IdempotencyService.java                # 幂等服务（SHA-256 请求哈希）
│   ├── lock/
│   │   └── DistributedLockWrapper.java            # 分布式锁封装
│   ├── metrics/
│   │   ├── PaymentMetricsService.java             # Micrometer 指标
│   │   └── PaymentMetricsConsumer.java            # RabbitMQ 消费者（监听支付指标事件）
├── domain/
│   ├── aggregate/
│   │   ├── Payment.java                  # 不可变聚合根（字段 final，状态转换返回 Transition<Payment, E> / 新实例）
│   │   ├── PaymentCreateSpec.java                 # record 收敛 create() 工厂参数
│   │   ├── PaymentReconstructSpec.java            # record 收敛 from() 重建参数（15 字段）
│   │   └── PaymentStatusGuard.java                # 支付状态机合法转换谓词（canPay/canRefund/...）
│   ├── valueobject/
│   │   ├── PaymentId.java                         # String UUID v7
│   │   ├── PaymentNo.java
│   │   ├── PaymentMethodInfo.java                 # record
│   │   └── IdempotencyKey.java
│   ├── event/                                     # 领域事件（record 实现 DomainEvent）
│   │   ├── PaymentCreatedEvent.java
│   │   ├── PaymentSucceededEvent.java
│   │   ├── PaymentFailedEvent.java
│   │   ├── PaymentRefundedEvent.java
│   │   └── PaymentClosedEvent.java
│   ├── port/                                      # 出站端口
│   │   ├── PaymentGatewayPort.java
│   │   ├── CallbackSignatureVerifierPort.java
│   │   ├── PaymentResult.java                     # 网关支付结果
│   │   └── RefundResult.java                      # 网关退款结果
│   ├── repository/
│   │   ├── PaymentRepository.java                 # 写仓储
│   │   └── IdempotencyKeyRepositoryPort.java
│   ├── constant/
│   │   ├── PaymentStatus.java                     # code 为 String："PENDING"/"SUCCESS"/"REFUNDED"/...（已语义化）
│   │   ├── PaymentMethod.java                     # code 为 String："WECHAT"/"ALIPAY"/"BALANCE"
│   │   └── PaymentResultCode.java
│   └── exception/
│       └── PaymentDomainException.java             # 统一支付异常（含 of() 工厂方法）
└── constant/
    └── PaymentConstant.java
```

## 领域事件

领域事件通过 `DomainEventPublisher` 发布到 RabbitMQ Topic Exchange（`eo.domain.events`），由各模块 `@RabbitListener` 消费者异步处理。

## 幂等保护

- `IdempotencyService` 对请求计算 SHA-256 哈希
- 幂等键存储由 framework 的 `IdempotencyKeyFilter` + Redis 承载（`eo_idempotency_key` 表已删除）
- 重复请求直接返回之前的结果

## 分布式锁

- `DistributedLockWrapper` 封装 Redis 分布式锁
- 支付创建、退款等关键操作加锁防止并发冲突

## 支付状态机（不可变聚合根 + Transition）

所有状态转换返回 `Transition<Payment, E>` record（聚合根新实例 + 领域事件），不修改自身。简单状态切换直接返回新 `Payment` 实例。

```
PENDING → PAYING → SUCCESS
  ↓         ↓        ↓
CLOSED    FAILED   REFUNDING → REFUNDED
                      ↓
                PARTIALLY_REFUNDED
                      ↓ (compensation)
                    SUCCESS
```

- 两阶段支付：`preparePay()` → `Payment` → 网关调用 → `confirmPay(PaymentResult)` → `Transition<Payment, PaymentConfirmEvent>`
- 两阶段退款：`prepareRefund(BigDecimal)` → `Payment` → 网关调用 → `confirmRefund(RefundResult, BigDecimal)` → `Transition<Payment, PaymentRefundedEvent>`
- 单步退款：`directRefund(String refundReason)` → `Transition<Payment, PaymentRefundedEvent>`
- 失败回退：`cancelPay()` / `cancelRefund()` 返回新 `Payment` 实例回退状态（两阶段网关失败时回退，不跨服务编排）
- Guard 方法：`canPay()` / `canRefund()` / `canClose()` / `canFail()` / `canConfirmPay()` / `canConfirmRefund()`

## Spec Record 与 Command Record

聚合根工厂与重建入口通过 spec record 收敛长参数列表：

| Spec / Command | 用途 | 关键字段 |
|----------------|------|---------|
| `PaymentCreateSpec` | `Payment.create()` 工厂参数 | paymentId, orderId, userId, amount, paymentMethod, attach |
| `PaymentReconstructSpec` | `Payment.from()` 重建参数 | id, paymentNo, orderId, userId, amount, refundedAmount, paymentMethod, status, transactionId, refundReason, refundTime, attach, createTime, updateTime, version |
| `Transition<Payment, E>` | 状态转换结果（聚合根新实例 + 领域事件） | aggregate, event |
| `PaymentCommand` | sealed 接口（permits 4 个命令 record） | — |
| `CreatePaymentCommand` | 创建支付命令 | orderId, amount, paymentMethod, payPassword, attach |
| `PayCommand` / `ClosePaymentCommand` | 单字段命令 | paymentNo / paymentId |
| `RefundPaymentCommand` | 退款命令 | paymentId, refundAmount, refundReason |
| `PaymentListQuery` | 列表查询参数收敛 | userId, status: PaymentStatus, pageNum, pageSize |

## 枚举字符串化

`PaymentStatus` / `PaymentMethod` 的 `code` 字段为 String（非 Integer），全链路字符串化：

- **DB 层**：`eo_payment.status` / `eo_payment.payment_method` 为 `VARCHAR(20)`，带 CHECK 约束
- **MyBatis**：枚举 `code` 字段标 `@EnumValue`，内置 `MybatisEnumTypeHandler` 完成 enum ↔ String 互转
- **领域层**：`Payment` / `PaymentReconstructSpec` 直接使用枚举类型，无 String.valueOf 转换
- **查询端口**：`PaymentQueryRepository.findByUserIdAndStatus(String, PaymentStatus, ...)` 入参为枚举类型
- **JSON 序列化**：`@JsonValue` 标注在 `code` 上，前端收到的就是 `"SUCCESS"` / `"WECHAT"` 而非 `1`

## 常见开发任务

### 添加新支付方式

1. `PaymentMethod` 枚举新增值（code 为 String，如 `"UNIONPAY"`）
2. `PaymentGatewayAdapter` 添加新网关调用逻辑
3. Flyway 迁移：`eo_payment.payment_method` 列 CHECK 约束追加新 code
4. 新枚举值自动适配（`fromCode()` throw on unknown）
5. 添加模拟支付支持（`MockPaymentController` `@Profile("dev")` 通过 `PaymentCreateSpec` 创建聚合根）
6. 测试

### 添加新支付事件

1. 创建事件 record 实现 `DomainEvent`
2. 在状态转换方法中返回 `Transition<Payment, XxxEvent>`
3. Handler 通过 `domainEventPublisher.publish(transition.event())` 发布
4. 路由键由事件类名自动派生（`PaymentXxxEvent` → `payment.xxx`），无需手动注册
5. 创建 `@RabbitListener` 消费者处理事件
6. 测试

## 安全要点

- 支付回调必须验签（`CallbackSignatureVerifierPort`）
- 金额使用 `Money` 值对象（`easyorange-common`），避免浮点精度问题
- 版本号乐观锁（`PaymentDO` 上的 `@Version` 注解 + 聚合根内 `int version` 字段）
- 幂等保护防止重复支付/退款
- Command 字段使用 Bean Validation（`@NotBlank` / `@NotNull` / `@Positive`）在 Controller 入口校验
