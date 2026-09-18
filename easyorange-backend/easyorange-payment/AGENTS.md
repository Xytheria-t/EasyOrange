# easyorange-payment 模块指南

支付处理模块，DDD + CQRS 架构，处理支付、回调确认与退款。

## 目录结构

```
payment/
├── adapter/
│   ├── inbound/
│   │   ├── web/
│   │   │   ├── controller/       # PaymentCommandController / PaymentQueryController / MockPaymentController（@Profile("dev")）
│   │   │   ├── assembler/        # PaymentCommandMapper（Request → Command）/ PaymentViewAssembler（Aggregate → Response）
│   │   │   ├── request/          # CreatePaymentRequest / RefundRequest / PaymentCallback / QueryPaymentRequest / MockPaymentRequest
│   │   │   └── response/         # PaymentResponse
│   │   └── messaging/
│   │       └── PaymentMetricsConsumer.java       # 支付事件 → Micrometer 计数器（队列 eo.payment.metrics）
│   └── outbound/
│       ├── config/
│       │   └── PaymentCallbackProperties.java    # 回调验签密钥与开关（payment.callback.*）
│       ├── gateway/
│       │   └── PaymentGatewayAdapter.java        # 实现 PaymentGatewayPort
│       ├── persistence/
│       │   ├── PaymentDO.java, PaymentMapper.java
│       │   ├── PaymentRepositoryImpl.java        # 实现 PaymentRepository + PaymentQueryRepository
│       │   └── converter/PaymentDataMapper.java  # MapStruct：PaymentDO ↔ Payment（基于 PaymentCreateSpec / PaymentReconstructSpec）
│       └── security/
│           └── CallbackSignatureVerifier.java    # HMAC-SHA256 回调验签（实现 CallbackSignatureVerifierPort）
├── application/
│   ├── command/                  # PaymentCommandHandler（用例收口）/ PaymentPhaseExecutor（独立 Bean，保证 @Transactional 生效）
│   │                             # + CreatePaymentCommand / PayCommand / PaymentCallbackCommand / RefundPaymentCommand / ClosePaymentCommand
│   ├── query/                    # PaymentQueryHandler / PaymentListQuery
│   └── port/query/               # PaymentQueryRepository（status 入参为 PaymentStatus 枚举）
└── domain/
    ├── aggregate/                # Payment / PaymentCreateSpec / PaymentReconstructSpec / PaymentStatusGuard（静态谓词）
    ├── constant/                 # PaymentStatus / PaymentMethod（code 为 String）/ PaymentResultCode / PaymentConstant
    ├── event/                    # PaymentCreatedEvent / PaymentSucceededEvent / PaymentConfirmEvent / PaymentFailedEvent / PaymentRefundedEvent / PaymentClosedEvent
    ├── exception/                # PaymentDomainException
    ├── port/                     # PaymentGatewayPort / CallbackSignatureVerifierPort / PaymentResult / RefundResult
    ├── repository/               # PaymentRepository（写仓储）
    └── valueobject/              # PaymentId / PaymentNo / PaymentMethodInfo
```

## 领域事件

领域事件经 `DomainEventPublisher` 发到 RabbitMQ Topic Exchange（`eo.domain.events`），路由键按事件类名派生（`PaymentXxxEvent` → `payment.xxx`）。

- `PaymentSucceededEvent`（routing key `payment.succeeded`）消费方：本模块 `PaymentMetricsConsumer`（队列 `eo.payment.metrics`）累加指标；order 模块 `PaymentSucceededEventConsumer`（队列 `eo.order.payment`）桥接「支付成功 → 订单置 PAID」，事件含 `orderId`。
- `MockPaymentController`（`@Profile("dev")`）成功路径经 `PaymentCommandHandler` 走正规两阶段流程发布事件，与真实网关回调同路径。

## 幂等与并发控制

- 协议级幂等（Idempotency-Key 防重放）由 framework `IdempotencyKeyFilter` 承担，模块内无幂等表与幂等服务。
- 支付、回调确认、退款经 framework `DistributedLockPort` 串行化：锁 key 为 `payment:lock:pay:{paymentNo}` / `payment:lock:refund:{paymentId}`，等待 0 秒不排队；锁争用映射 `PaymentResultCode.PAYMENT_BUSY`（429 可重试，由网关重试兜底）。

## 支付状态机（不可变聚合根 + Transition）

所有状态转换返回 `Transition<Payment, E>` record（聚合根新实例 + 领域事件），不修改自身。简单状态切换直接返回新 `Payment` 实例。

```
PENDING → PAYING → SUCCESS
  ↓         ↓        ↓
CLOSED    FAILED   REFUNDING → REFUNDED
                      ↓
                PARTIALLY_REFUNDED
                      ↓ (补偿)
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
| `CreatePaymentCommand` | 创建支付命令 | orderId, amount, paymentMethod, payPassword, attach |
| `PayCommand` | 支付命令 | paymentNo, transactionId, attach |
| `PaymentCallbackCommand` | 回调确认命令 | paymentNo, transactionId, amount（非空时校验与支付单一致） |
| `RefundPaymentCommand` | 退款命令 | paymentId, userId, refundAmount, refundReason |
| `ClosePaymentCommand` | 关闭命令 | paymentId, userId |
| `PaymentListQuery` | 列表查询参数收敛 | userId, status: PaymentStatus, pageNum, pageSize |

## 枚举字符串化

`PaymentStatus` / `PaymentMethod` 的 `code` 字段为 String（非 Integer），全链路字符串化：

- **DB 层**：`eo_payment.status` / `eo_payment.payment_method` 为 `VARCHAR(20)`，带 CHECK 约束
- **领域层**：`Payment` / `PaymentReconstructSpec` 直接使用枚举类型，无 String.valueOf 转换
- **查询端口**：`PaymentQueryRepository.findByUserIdAndStatus(String, PaymentStatus, ...)` 入参为枚举类型
- **JSON 序列化**：`@JsonValue` 标注在 `code` 上（枚举实现 `BaseCodeEnum`），前端收到的就是 `"SUCCESS"` / `"WECHAT"` 而非 `1`

## 常见开发任务

### 添加新支付方式

1. `PaymentMethod` 枚举新增值（code 为 String，如 `"UNIONPAY"`）
2. `PaymentGatewayAdapter` 添加新网关调用逻辑
3. Flyway 迁移：`eo_payment.payment_method` 列 CHECK 约束追加新 code
4. 新枚举值自动适配（`fromCode()` throw on unknown）
5. MockPaymentController 支持新方式（`@Profile("dev")`，经 `PaymentCreateSpec` 创建聚合根）
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
- 版本号乐观锁（`PaymentDO` 上的 `@Version` 注解 + 聚合根内 `int version` 字段）
- 支付 / 回调 / 退款由 `DistributedLockPort` + `IdempotencyKeyFilter` 共同防并发与重放
- Command 字段使用 Bean Validation（`@NotBlank` / `@NotNull` / `@Positive`）在 Controller 入口校验
