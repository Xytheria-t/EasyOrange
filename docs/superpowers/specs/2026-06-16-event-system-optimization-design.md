# Event System Optimization Design

**Date**: 2026-06-16
**Status**: Approved
**Approach**: Convention-Driven Redesign (Approach B)

## Problem Statement

The current event system has several architectural issues that violate concise best practices:

1. **Critical: Shared Queue Anti-Pattern** — 6 consumer classes share `eo.product.events` queue. In RabbitMQ, multiple consumers on the same queue compete for messages (round-robin), not each receive a copy. An `OrderCreatedEvent` can be delivered to the wrong consumer, causing deserialization failures or silent message loss.

2. **Critical: Competing Containers Bug** — `ProductEventConsumer` and `OrderNotificationEventConsumer` use method-level `@RabbitListener` annotations on the same queue, creating multiple competing containers per class. Messages round-robin between methods instead of being dispatched by type.

3. **Hardcoded RoutingKeyResolver** — A 26-entry static `Map<String, String>` that must be manually updated for every new event type. Violates Open/Closed Principle.

4. **Inconsistent Routing Key Naming** — Mix of `{module}.aggregate.{action}` and `{module}.{domain}.{action}` patterns.

5. **No Dead Letter Queue** — `defaultRequeueRejected: false` means failed messages are silently dropped with no recovery path.

6. **No Consumer Retry** — No exponential backoff for transient failures.

7. **Dead Code: PaymentMetricsListener** — Uses Spring `@EventListener` but events are published via RabbitMQ. Never receives any events.

8. **Redundant BaseDomainEvent Fields** — `aggregateType` (unused by consumers), `version` (always 1), `eventType()` (26 redundant overrides).

## Design Decisions

### D1: Convention-Based Routing Keys

**Decision**: Derive routing keys from event class names mechanically. Zero configuration.

**Convention**: Strip `Event` suffix → camelCase to dot.case → lowercase.

| Event Class | Derived Routing Key |
|---|---|
| `ProductCreatedEvent` | `product.created` |
| `StockReservationRequestedEvent` | `stock.reservation.requested` |
| `OrderPaidEvent` | `order.paid` |
| `PaymentSucceededEvent` | `payment.succeeded` |
| `MessageRecalledEvent` | `message.recalled` |
| `ReportProcessedEvent` | `report.processed` |

**Implementation**: Replace `RoutingKeyResolver`'s 26-entry map with a 3-line method:

```java
public String resolve(BaseDomainEvent event) {
    String typeName = event.eventType(); // "ProductCreated"
    return typeName.replaceAll("([a-z])([A-Z])", "$1.$2").toLowerCase();
}
```

The first segment naturally groups events by domain concept, enabling wildcard bindings like `product.#`, `order.#`, `stock.#`.

### D2: Per-Consumer Queues

**Decision**: Replace 4 shared queues with 9 dedicated queues, each bound to only the routing keys its consumer needs.

| Queue | Consumer Class | Routing Key Bindings | DLQ |
|---|---|---|---|
| `eo.product.cqrs` | `ProductEventConsumer` | `product.#`, `stock.decreased`, `stock.restored` | `eo.product.cqrs.dlq` |
| `eo.order.notification` | `OrderNotificationEventConsumer` | `order.#` | `eo.order.notification.dlq` |
| `eo.order.saga` | `OrderSagaEventConsumer` | `order.created`, `order.cancelled`, `order.completed`, `order.refunded` | `eo.order.saga.dlq` |
| `eo.stock.reservation` | `StockReservationEventConsumer` | `stock.reservation.requested` | `eo.stock.reservation.dlq` |
| `eo.payment.initiation` | `PaymentInitiationEventConsumer` | `payment.initiation.requested` | `eo.payment.initiation.dlq` |
| `eo.audit.notification` | `ProductAuditEventConsumer` | `product.audited` | `eo.audit.notification.dlq` |
| `eo.report.notification` | `ReportProcessedEventConsumer` | `report.processed` | `eo.report.notification.dlq` |
| `eo.message.websocket` | `WebSocketEventConsumer` | `message.recalled` | `eo.message.websocket.dlq` |
| `eo.payment.metrics` | `PaymentMetricsConsumer` | `payment.#` | `eo.payment.metrics.dlq` |

**Removed queues**: `eo.product.events`, `eo.order.events`, `eo.payment.events`, `eo.message.events`

### D3: Merge Order Saga Consumers

**Decision**: Merge 4 separate order saga consumers into single `OrderSagaEventConsumer`.

**Before** (4 classes, 4 competing containers on same queue):
- `OrderCreatedEventConsumer` → publishes `StockReservationRequestedEvent`
- `OrderCancelledEventConsumer` → restores stock
- `OrderCompletedEventConsumer` → marks products as sold
- `OrderRefundedEventConsumer` → restores stock

**After** (1 class, 1 queue, type-based dispatch):
```java
@Component
@RabbitListener(queues = "eo.order.saga", containerFactory = "domainEventContainerFactory")
public class OrderSagaEventConsumer {
    @RabbitHandler
    public void onOrderCreated(OrderCreatedEvent event) { ... }
    @RabbitHandler
    public void onOrderCancelled(OrderCancelledEvent event) { ... }
    @RabbitHandler
    public void onOrderCompleted(OrderCompletedEvent event) { ... }
    @RabbitHandler
    public void onOrderRefunded(OrderRefundedEvent event) { ... }
}
```

### D4: Migrate PaymentMetricsListener

**Decision**: Convert `PaymentMetricsListener` from Spring `@EventListener` (dead code) to RabbitMQ `@RabbitListener`.

**Before** (never receives events):
```java
@Component
public class PaymentMetricsListener {
    @Async @EventListener
    public void onPaymentCreated(PaymentCreatedEvent event) { ... }
    // ...
}
```

**After** (receives events via RabbitMQ):
```java
@Component
@RabbitListener(queues = "eo.payment.metrics", containerFactory = "domainEventContainerFactory")
public class PaymentMetricsConsumer {
    @RabbitHandler
    public void onPaymentCreated(PaymentCreatedEvent event) { ... }
    // ...
}
```

### D5: Simplify BaseDomainEvent

**Decision**: Remove redundant fields, make `eventType()` concrete with convention-based derivation.

**Before**:
```java
public abstract class BaseDomainEvent implements Serializable {
    private final String eventId;
    private final String aggregateType;  // unused by consumers
    private final int version;            // always 1
    private final Instant occurredOn;

    protected BaseDomainEvent(Class<?> aggregateType) { ... }
    protected BaseDomainEvent(Class<?> aggregateType, int version) { ... }
    public abstract String eventType();   // 26 redundant overrides
}
```

**After**:
```java
public abstract class BaseDomainEvent implements Serializable {
    private final String eventId;
    private final Instant occurredOn;

    protected BaseDomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = Instant.now();
    }

    public String eventType() {
        String simpleName = getClass().getSimpleName();
        return simpleName.endsWith("Event")
            ? simpleName.substring(0, simpleName.length() - 5)
            : simpleName;
    }
}
```

**Impact on 26 event classes**: All lose `eventType()` overrides and `super(SomeClass.class)` calls. Constructors simplify from `super(ProductCreatedEvent.class)` to `super()`.

### D6: DLQ + Retry Configuration

**Decision**: Add per-queue DLQ with exponential backoff retry.

**Topology**:
- DLQ exchange: `eo.dlq` (TopicExchange, durable)
- Each queue configured with `x-dead-letter-exchange` → `eo.dlq` and `x-dead-letter-routing-key` → `eo.{name}.dlq`
- Each DLQ: `eo.{name}.dlq` (Quorum, durable, no TTL — for manual inspection)
- Retry: Spring AMQP `RetryInterceptor` with exponential backoff (1s → 2s → 4s, max 3 attempts) before routing to DLQ

**Configuration** (in `RabbitMQConfig`):
```java
private Queue createQueue(String name) {
    return QueueBuilder.durable(name)
        .quorum()
        .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE_NAME)
        .withArgument("x-dead-letter-routing-key", name + ".dlq")
        .build();
}

private Queue createDlq(String name) {
    return QueueBuilder.durable(name + ".dlq")
        .quorum()
        .build();
}
```

### D7: Consumer Pattern — `@RabbitListener` + `@RabbitHandler`

**Decision**: Use class-level `@RabbitListener` + method-level `@RabbitHandler` for multi-method consumers.

**Before** (buggy — competing containers):
```java
@Component
public class ProductEventConsumer {
    @RabbitListener(queues = "eo.order.events", containerFactory = "domainEventContainerFactory")
    public void onProductCreated(ProductCreatedEvent event) { ... }

    @RabbitListener(queues = "eo.order.events", containerFactory = "domainEventContainerFactory")
    public void onProductUpdated(ProductUpdatedEvent event) { ... }
}
```

**After** (correct — single container, type-based dispatch):
```java
@Component
@RabbitListener(queues = "eo.product.cqrs", containerFactory = "domainEventContainerFactory")
public class ProductEventConsumer {
    @RabbitHandler
    public void onProductCreated(ProductCreatedEvent event) { ... }

    @RabbitHandler
    public void onProductUpdated(ProductUpdatedEvent event) { ... }
}
```

This pattern applies to: `ProductEventConsumer`, `OrderNotificationEventConsumer`, `OrderSagaEventConsumer`, `PaymentMetricsConsumer`.

Single-method consumers remain as `@RabbitListener` on the method level (no change needed).

## File Change Summary

### Modified Files

| File | Change |
|---|---|
| `BaseDomainEvent.java` | Remove `aggregateType`, `version`; make `eventType()` concrete; simplify constructor |
| `RabbitMQConfig.java` | Replace 4 shared queues with 9 per-consumer queues + 9 DLQs + DLQ exchange; add retry config |
| `RabbitMQProperties.java` | Add DLQ/retry config properties |
| `RoutingKeyResolver.java` | Replace 26-entry map with convention-based derivation |
| `RabbitMQDomainEventPublisher.java` | No logic change (routing key comes from resolver) |
| `ProductEventConsumer.java` | Change to `@RabbitListener`+`@RabbitHandler`; update queue name |
| `OrderNotificationEventConsumer.java` | Change to `@RabbitListener`+`@RabbitHandler`; update queue name |
| `PaymentMetricsListener.java` → `PaymentMetricsConsumer.java` | Migrate from `@EventListener` to `@RabbitListener`; rename class |
| All 26 event classes | Remove `eventType()` overrides; simplify constructors (`super()` instead of `super(XxxEvent.class)`) |

### Deleted Files

| File | Reason |
|---|---|
| `OrderCreatedEventConsumer.java` | Merged into `OrderSagaEventConsumer` |
| `OrderCancelledEventConsumer.java` | Merged into `OrderSagaEventConsumer` |
| `OrderCompletedEventConsumer.java` | Merged into `OrderSagaEventConsumer` |
| `OrderRefundedEventConsumer.java` | Merged into `OrderSagaEventConsumer` |

### New Files

| File | Purpose |
|---|---|
| `OrderSagaEventConsumer.java` | Merged order saga consumer (order module) |
| `PaymentMetricsConsumer.java` | Migrated from `PaymentMetricsListener` (payment module) |

### Unchanged Files

| File | Reason |
|---|---|
| `DomainEventPublisher.java` | Interface unchanged |
| `EventIdempotencyChecker.java` | Still used by `OrderNotificationEventConsumer` |
| `ConfirmCallback.java` | Still used |
| `ReturnCallback.java` | Still used |
| `StockReservationEventConsumer.java` | Single-method, just update queue name |
| `PaymentInitiationEventConsumer.java` | Single-method, just update queue name |
| `ProductAuditEventConsumer.java` | Single-method, just update queue name |
| `ReportProcessedEventConsumer.java` | Single-method, just update queue name |
| `WebSocketEventConsumer.java` | Single-method, just update queue name |

## Migration Strategy

1. **Phase 1**: Update `BaseDomainEvent` and all 26 event classes (no runtime impact — events are still compatible)
2. **Phase 2**: Update `RoutingKeyResolver` to convention-based (routing keys change format — requires queue drain)
3. **Phase 3**: Update `RabbitMQConfig` with new queue topology (old queues must be drained first)
4. **Phase 4**: Update all consumer classes with new queue names and `@RabbitHandler` pattern
5. **Phase 5**: Merge order saga consumers, migrate `PaymentMetricsListener`
6. **Phase 6**: Add DLQ + retry configuration

**Deployment note**: Phases 2-3 require draining existing queues before deploying. During deployment, the old queues should be deleted and new queues created. This is best done during a maintenance window.

## Routing Key Mapping (Old → New)

| Old Routing Key | New Routing Key |
|---|---|
| `order.aggregate.created` | `order.created` |
| `order.aggregate.cancelled` | `order.cancelled` |
| `order.aggregate.paid` | `order.paid` |
| `order.aggregate.completed` | `order.completed` |
| `order.aggregate.refunded` | `order.refunded` |
| `order.aggregate.shipped` | `order.shipped` |
| `order.stock.reservation-requested` | `stock.reservation.requested` |
| `order.payment.initiation-requested` | `payment.initiation.requested` |
| `product.aggregate.created` | `product.created` |
| `product.aggregate.updated` | `product.updated` |
| `product.aggregate.deleted` | `product.deleted` |
| `product.aggregate.marked-sold` | `product.marked.sold` |
| `product.aggregate.submitted-for-review` | `product.submitted.for.review` |
| `product.audit.completed` | `product.audited` |
| `product.report.processed` | `report.processed` |
| `product.stock.decreased` | `stock.decreased` |
| `product.stock.restored` | `stock.restored` |
| `payment.transaction.created` | `payment.created` |
| `payment.transaction.succeeded` | `payment.succeeded` |
| `payment.transaction.failed` | `payment.failed` |
| `payment.transaction.refunded` | `payment.refunded` |
| `payment.transaction.closed` | `payment.closed` |
| `message.aggregate.sent` | `message.sent` |
| `message.aggregate.read` | `message.read` |
| `message.aggregate.deleted` | `message.deleted` |
| `message.aggregate.recalled` | `message.recalled` |