# Event System Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the shared-queue anti-pattern, establish convention-based routing, simplify BaseDomainEvent, add DLQ/retry, and unify consumer patterns.

**Architecture:** Per-consumer queues with DLQ, convention-based routing key derivation from event class names, `@RabbitListener`+`@RabbitHandler` pattern for multi-method consumers, merged order saga consumer, migrated PaymentMetricsListener.

**Tech Stack:** Spring Boot 4.0.3, Spring AMQP 4.0.x, RabbitMQ 3.13, Java 25, JUnit 5, Mockito

---

## Phase 1: Simplify BaseDomainEvent (No Runtime Impact)

### Task 1: Simplify BaseDomainEvent base class

**Files:**
- Modify: `easyorange-backend/easyorange-common/src/main/java/com/cartethyia/easyorange/common/event/BaseDomainEvent.java`

- [ ] **Step 1: Write the failing test for eventType() convention**

Create test file: `easyorange-backend/easyorange-common/src/test/java/com/cartethyia/easyorange/common/event/BaseDomainEventTest.java`

```java
package com.cartethyia.easyorange.common.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BaseDomainEventTest {

    @Test
    @DisplayName("eventType strips Event suffix from class name")
    void eventType_stripsEventSuffix() {
        var event = new BaseDomainEvent() {
            // Anonymous class: BaseDomainEventTest$1 — no "Event" suffix
        };
        // Anonymous classes don't follow the convention, so test with named inner classes
    }

    @Test
    @DisplayName("eventType derives from class name for XxxEvent pattern")
    void eventType_derivesFromClassName() {
        var event = new ProductCreatedEvent();
        assertThat(event.eventType()).isEqualTo("ProductCreated");
    }

    @Test
    @DisplayName("eventType handles class without Event suffix")
    void eventType_handlesNoEventSuffix() {
        // For classes that don't end with "Event", return simple name as-is
        var event = new BaseDomainEvent() {};
        // Anonymous class name contains "$", so eventType returns the simple name
        assertThat(event.eventType()).isNotNull();
    }

    @Test
    @DisplayName("eventId is non-null UUID")
    void eventId_isNonNullUUID() {
        var event = new ProductCreatedEvent();
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getEventId()).matches("[0-9a-f-]{36}");
    }

    @Test
    @DisplayName("occurredOn is non-null")
    void occurredOn_isNonNull() {
        var event = new ProductCreatedEvent();
        assertThat(event.getOccurredOn()).isNotNull();
    }

    /** Minimal concrete event for testing */
    static class ProductCreatedEvent extends BaseDomainEvent {
        // No fields needed for base class testing
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd easyorange-backend && ./mvnw test -pl easyorange-common -Dtest=BaseDomainEventTest -DfailIfNoTests=false`
Expected: FAIL — `eventType()` is still abstract, anonymous class test may pass but `ProductCreatedEvent` inner class won't compile without `eventType()` override

- [ ] **Step 3: Implement the simplified BaseDomainEvent**

Replace `BaseDomainEvent.java` with:

```java
package com.cartethyia.easyorange.common.event;

import lombok.Getter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Getter
public abstract class BaseDomainEvent implements Serializable {

    private final String eventId;
    private final Instant occurredOn;

    protected BaseDomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = Instant.now();
    }

    public String eventType() {
        String simpleName = getClass().getSimpleName();
        if (simpleName.endsWith("Event")) {
            return simpleName.substring(0, simpleName.length() - 5);
        }
        return simpleName;
    }
}
```

Key changes:
- Removed `aggregateType` field (unused by consumers)
- Removed `version` field (always 1, no evolution strategy)
- Removed `eventType()` abstract method — now concrete with convention-based derivation
- Simplified constructor — no `Class<?>` parameter needed

- [ ] **Step 4: Run test to verify it passes**

Run: `cd easyorange-backend && ./mvnw test -pl easyorange-common -Dtest=BaseDomainEventTest -DfailIfNoTests=false`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add easyorange-backend/easyorange-common/src/main/java/com/cartethyia/easyorange/common/event/BaseDomainEvent.java
git add easyorange-backend/easyorange-common/src/test/java/com/cartethyia/easyorange/common/event/BaseDomainEventTest.java
git commit -m "refactor(event): simplify BaseDomainEvent — remove aggregateType, version, make eventType() concrete"
```

---

### Task 2: Update all 26 event classes to remove eventType() overrides and simplify constructors

**Files:**
- Modify: All 9 event files in `easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/event/`
- Modify: All 8 event files in `easyorange-order/src/main/java/com/cartethyia/easyorange/order/domain/event/`
- Modify: All 5 event files in `easyorange-payment/src/main/java/com/cartethyia/easyorange/payment/domain/event/`
- Modify: All 4 event files in `easyorange-message/src/main/java/com/cartethyia/easyorange/message/domain/event/`

For each event class, the changes are:
1. Remove the `eventType()` override method
2. Change `super(SomeClass.class)` → `super()` in constructor
3. Remove `@Override` annotation if it was the only override

**Example transformation for `ProductCreatedEvent`:**

Before:
```java
public class ProductCreatedEvent extends BaseDomainEvent {
    // fields...

    public ProductCreatedEvent(Long productId, Long userId, ...) {
        super(ProductCreatedEvent.class);  // ← remove class parameter
        // field assignments...
    }

    // getters...

    @Override
    public String eventType() {        // ← DELETE this entire method
        return "ProductCreated";
    }
}
```

After:
```java
public class ProductCreatedEvent extends BaseDomainEvent {
    // fields...

    public ProductCreatedEvent(Long productId, Long userId, ...) {
        super();                       // ← simplified
        // field assignments...
    }

    // getters...
    // eventType() removed — derived by BaseDomainEvent
}
```

- [ ] **Step 1: Update product module events (9 files)**

Files to modify:
- `ProductCreatedEvent.java` — `super(ProductCreatedEvent.class)` → `super()`, remove `eventType()`
- `ProductUpdatedEvent.java` — same pattern
- `ProductDeletedEvent.java` — same pattern
- `ProductMarkedSoldEvent.java` — same pattern
- `ProductSubmittedForReviewEvent.java` — same pattern
- `ProductAuditedEvent.java` — same pattern (has `@Getter`, remove `eventType()`)
- `ReportProcessedEvent.java` — same pattern (has `@Getter`, remove `eventType()`)
- `StockDecreasedEvent.java` — same pattern
- `StockRestoredEvent.java` — same pattern

- [ ] **Step 2: Update order module events (8 files)**

Files to modify:
- `OrderCreatedEvent.java` — `super(OrderCreatedEvent.class)` → `super()`, remove `eventType()`
- `OrderPaidEvent.java` — same pattern
- `OrderShippedEvent.java` — same pattern
- `OrderCompletedEvent.java` — same pattern
- `OrderCancelledEvent.java` — same pattern
- `OrderRefundedEvent.java` — same pattern
- `PaymentInitiationRequestedEvent.java` — same pattern
- `StockReservationRequestedEvent.java` — same pattern

- [ ] **Step 3: Update payment module events (5 files)**

Files to modify:
- `PaymentCreatedEvent.java` — remove `private static final String EVENT_TYPE = "PaymentCreated"`, remove `eventType()` override, change `super(PaymentCreatedEvent.class)` → `super()`
- `PaymentSucceededEvent.java` — same pattern
- `PaymentFailedEvent.java` — same pattern
- `PaymentRefundedEvent.java` — same pattern
- `PaymentClosedEvent.java` — same pattern

Note: Payment events use a `private static final String EVENT_TYPE` constant pattern. Remove the constant AND the `eventType()` override.

- [ ] **Step 4: Update message module events (4 files)**

Files to modify:
- `MessageSentEvent.java` — `super(MessageSentEvent.class)` → `super()`, remove `eventType()`
- `MessageReadEvent.java` — same pattern
- `MessageDeletedEvent.java` — same pattern
- `MessageRecalledEvent.java` — same pattern

- [ ] **Step 5: Run all tests to verify nothing is broken**

Run: `cd easyorange-backend && ./mvnw test -DexcludedGroups=integration`
Expected: ALL PASS

- [ ] **Step 6: Commit**

```bash
git add easyorange-backend/easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/event/
git add easyorange-backend/easyorange-order/src/main/java/com/cartethyia/easyorange/order/domain/event/
git add easyorange-backend/easyorange-payment/src/main/java/com/cartethyia/easyorange/payment/domain/event/
git add easyorange-backend/easyorange-message/src/main/java/com/cartethyia/easyorange/message/domain/event/
git commit -m "refactor(event): remove eventType() overrides and simplify constructors in all 26 event classes"
```

---

## Phase 2: Convention-Based RoutingKeyResolver

### Task 3: Replace RoutingKeyResolver with convention-based derivation

**Files:**
- Modify: `easyorange-backend/easyorange-framework/src/main/java/com/cartethyia/easyorange/framework/messaging/core/RoutingKeyResolver.java`
- Modify: `easyorange-backend/easyorange-framework/src/test/java/com/cartethyia/easyorange/framework/messaging/RoutingKeyResolverTest.java`

- [ ] **Step 1: Write the failing test for convention-based routing**

Replace `RoutingKeyResolverTest.java` with:

```java
package com.cartethyia.easyorange.framework.messaging;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.framework.messaging.core.RoutingKeyResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

class RoutingKeyResolverTest {

    private RoutingKeyResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new RoutingKeyResolver();
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @CsvSource({
        "OrderCreated, order.created",
        "OrderCancelled, order.cancelled",
        "OrderPaid, order.paid",
        "OrderCompleted, order.completed",
        "OrderRefunded, order.refunded",
        "OrderShipped, order.shipped",
        "StockReservationRequested, stock.reservation.requested",
        "PaymentInitiationRequested, payment.initiation.requested",
        "ProductCreated, product.created",
        "ProductUpdated, product.updated",
        "ProductDeleted, product.deleted",
        "ProductMarkedSold, product.marked.sold",
        "ProductSubmittedForReview, product.submitted.for.review",
        "ProductAudited, product.audited",
        "ReportProcessed, report.processed",
        "StockDecreased, stock.decreased",
        "StockRestored, stock.restored",
        "PaymentCreated, payment.created",
        "PaymentSucceeded, payment.succeeded",
        "PaymentFailed, payment.failed",
        "PaymentRefunded, payment.refunded",
        "PaymentClosed, payment.closed",
        "MessageSent, message.sent",
        "MessageRead, message.read",
        "MessageDeleted, message.deleted",
        "MessageRecalled, message.recalled"
    })
    @DisplayName("resolve known event types returns correct routing keys")
    void resolve_knownEventType_returnsCorrectRoutingKey(String eventType, String expectedRoutingKey) {
        var event = createTestEvent(eventType);
        assertThat(resolver.resolve(event)).isEqualTo(expectedRoutingKey);
    }

    @Test
    @DisplayName("resolve unknown event type still produces a routing key (convention-based)")
    void resolve_unknownEventType_producesConventionKey() {
        var unknownEvent = createTestEvent("UserRegistered");
        // Convention-based: unknown events still get a routing key
        assertThat(resolver.resolve(unknownEvent)).isEqualTo("user.registered");
    }

    @Test
    @DisplayName("resolve handles single-word event type")
    void resolve_singleWordEventType() {
        var event = createTestEvent("Login");
        assertThat(resolver.resolve(event)).isEqualTo("login");
    }

    private static BaseDomainEvent createTestEvent(String eventType) {
        return new BaseDomainEvent() {
            @Override
            public String eventType() {
                return eventType;
            }
        };
    }
}
```

Key test changes:
- All 26 routing key mappings updated to convention-based format
- Unknown event types no longer throw — they produce a convention-based key
- Removed `hasRoutingKey()` and `getRegisteredEventTypes()` tests (those methods are removed)

- [ ] **Step 2: Run test to verify it fails**

Run: `cd easyorange-backend && ./mvnw test -pl easyorange-framework -Dtest=RoutingKeyResolverTest -DfailIfNoTests=false`
Expected: FAIL — old routing key format doesn't match new convention

- [ ] **Step 3: Implement convention-based RoutingKeyResolver**

Replace `RoutingKeyResolver.java` with:

```java
package com.cartethyia.easyorange.framework.messaging.core;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import org.springframework.stereotype.Component;

@Component
public class RoutingKeyResolver {

    /**
     * Derives a routing key from the event type using convention:
     * Strip "Event" suffix (already done by BaseDomainEvent.eventType()),
     * then convert camelCase to dot.case lowercase.
     *
     * Examples:
     *   ProductCreated → product.created
     *   StockReservationRequested → stock.reservation.requested
     *   OrderPaid → order.paid
     */
    public String resolve(BaseDomainEvent event) {
        String typeName = event.eventType();
        return typeName.replaceAll("([a-z])([A-Z])", "$1.$2").toLowerCase();
    }
}
```

Key changes:
- Removed the 26-entry hardcoded `Map`
- Removed `hasRoutingKey()` and `getRegisteredEventTypes()` methods
- Convention-based derivation: camelCase → dot.case lowercase
- Unknown event types automatically get routing keys (no more `IllegalArgumentException`)

- [ ] **Step 4: Run test to verify it passes**

Run: `cd easyorange-backend && ./mvnw test -pl easyorange-framework -Dtest=RoutingKeyResolverTest -DfailIfNoTests=false`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add easyorange-backend/easyorange-framework/src/main/java/com/cartethyia/easyorange/framework/messaging/core/RoutingKeyResolver.java
git add easyorange-backend/easyorange-framework/src/test/java/com/cartethyia/easyorange/framework/messaging/RoutingKeyResolverTest.java
git commit -m "refactor(event): replace RoutingKeyResolver hardcoded map with convention-based derivation"
```

---

## Phase 3: Per-Consumer Queues + DLQ + @RabbitHandler Pattern

### Task 4: Rewrite RabbitMQConfig with per-consumer queues and DLQ

**Files:**
- Modify: `easyorange-backend/easyorange-framework/src/main/java/com/cartethyia/easyorange/framework/messaging/config/RabbitMQConfig.java`
- Modify: `easyorange-backend/easyorange-framework/src/main/java/com/cartethyia/easyorange/framework/messaging/config/RabbitMQProperties.java`

- [ ] **Step 1: Update RabbitMQProperties with DLQ/retry config**

Add DLQ and retry configuration to `RabbitMQProperties.java`:

```java
@Data
@Primary
@ConfigurationProperties(prefix = "easyorange.rabbitmq")
public class RabbitMQProperties {

    private boolean enabled = true;
    private String exchange = "eo.domain.events";
    private String routingKeyPrefix = "";

    private PublisherConfig publisher = new PublisherConfig();
    private ConsumerConfig consumer = new ConsumerConfig();
    private DlqConfig dlq = new DlqConfig();
    private List<QueueConfig> queues = List.of();

    // ... existing inner classes (PublisherConfig, ConsumerConfig, RetryConfig, ConcurrencyConfig, QueueConfig) ...

    @Data
    public static class DlqConfig {
        private boolean enabled = true;
        private String exchange = "eo.dlq";
    }
}
```

- [ ] **Step 2: Rewrite RabbitMQConfig with per-consumer queues and DLQ**

Replace `RabbitMQConfig.java` with the new topology. Key changes:
- Remove 4 shared queues (`eo.product.events`, `eo.order.events`, `eo.payment.events`, `eo.message.events`)
- Add 9 per-consumer queues with DLQ arguments
- Add 9 DLQ queues
- Add DLQ exchange
- Add bindings for each queue to only the routing keys its consumer needs
- Add retry interceptor to `domainEventContainerFactory`

```java
package com.cartethyia.easyorange.framework.messaging.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.NewMessageRecoverer;
import org.springframework.amqp.retry.InterceptingChunkMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(RabbitMQProperties.class)
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "eo.domain.events";
    public static final String DLQ_EXCHANGE_NAME = "eo.dlq";

    // Per-consumer queue names
    public static final String QUEUE_PRODUCT_CQRS = "eo.product.cqrs";
    public static final String QUEUE_ORDER_NOTIFICATION = "eo.order.notification";
    public static final String QUEUE_ORDER_SAGA = "eo.order.saga";
    public static final String QUEUE_STOCK_RESERVATION = "eo.stock.reservation";
    public static final String QUEUE_PAYMENT_INITIATION = "eo.payment.initiation";
    public static final String QUEUE_AUDIT_NOTIFICATION = "eo.audit.notification";
    public static final String QUEUE_REPORT_NOTIFICATION = "eo.report.notification";
    public static final String QUEUE_MESSAGE_WEBSOCKET = "eo.message.websocket";
    public static final String QUEUE_PAYMENT_METRICS = "eo.payment.metrics";

    private final RabbitMQProperties properties;

    // === Exchange ===

    @Bean
    public TopicExchange domainEventExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public TopicExchange dlqExchange() {
        return new TopicExchange(DLQ_EXCHANGE_NAME, true, false);
    }

    // === Queues (with DLQ arguments) ===

    @Bean
    public Queue productCqrsQueue() {
        return createQueue(QUEUE_PRODUCT_CQRS);
    }

    @Bean
    public Queue orderNotificationQueue() {
        return createQueue(QUEUE_ORDER_NOTIFICATION);
    }

    @Bean
    public Queue orderSagaQueue() {
        return createQueue(QUEUE_ORDER_SAGA);
    }

    @Bean
    public Queue stockReservationQueue() {
        return createQueue(QUEUE_STOCK_RESERVATION);
    }

    @Bean
    public Queue paymentInitiationQueue() {
        return createQueue(QUEUE_PAYMENT_INITIATION);
    }

    @Bean
    public Queue auditNotificationQueue() {
        return createQueue(QUEUE_AUDIT_NOTIFICATION);
    }

    @Bean
    public Queue reportNotificationQueue() {
        return createQueue(QUEUE_REPORT_NOTIFICATION);
    }

    @Bean
    public Queue messageWebSocketQueue() {
        return createQueue(QUEUE_MESSAGE_WEBSOCKET);
    }

    @Bean
    public Queue paymentMetricsQueue() {
        return createQueue(QUEUE_PAYMENT_METRICS);
    }

    // === DLQ Queues ===

    @Bean
    public Queue productCqrsDlq() {
        return createDlq(QUEUE_PRODUCT_CQRS);
    }

    @Bean
    public Queue orderNotificationDlq() {
        return createDlq(QUEUE_ORDER_NOTIFICATION);
    }

    @Bean
    public Queue orderSagaDlq() {
        return createDlq(QUEUE_ORDER_SAGA);
    }

    @Bean
    public Queue stockReservationDlq() {
        return createDlq(QUEUE_STOCK_RESERVATION);
    }

    @Bean
    public Queue paymentInitiationDlq() {
        return createDlq(QUEUE_PAYMENT_INITIATION);
    }

    @Bean
    public Queue auditNotificationDlq() {
        return createDlq(QUEUE_AUDIT_NOTIFICATION);
    }

    @Bean
    public Queue reportNotificationDlq() {
        return createDlq(QUEUE_REPORT_NOTIFICATION);
    }

    @Bean
    public Queue messageWebSocketDlq() {
        return createDlq(QUEUE_MESSAGE_WEBSOCKET);
    }

    @Bean
    public Queue paymentMetricsDlq() {
        return createDlq(QUEUE_PAYMENT_METRICS);
    }

    // === DLQ Bindings ===

    @Bean
    public Binding productCqrsDlqBinding() {
        return bindDlq(QUEUE_PRODUCT_CQRS);
    }

    @Bean
    public Binding orderNotificationDlqBinding() {
        return bindDlq(QUEUE_ORDER_NOTIFICATION);
    }

    @Bean
    public Binding orderSagaDlqBinding() {
        return bindDlq(QUEUE_ORDER_SAGA);
    }

    @Bean
    public Binding stockReservationDlqBinding() {
        return bindDlq(QUEUE_STOCK_RESERVATION);
    }

    @Bean
    public Binding paymentInitiationDlqBinding() {
        return bindDlq(QUEUE_PAYMENT_INITIATION);
    }

    @Bean
    public Binding auditNotificationDlqBinding() {
        return bindDlq(QUEUE_AUDIT_NOTIFICATION);
    }

    @Bean
    public Binding reportNotificationDlqBinding() {
        return bindDlq(QUEUE_REPORT_NOTIFICATION);
    }

    @Bean
    public Binding messageWebSocketDlqBinding() {
        return bindDlq(QUEUE_MESSAGE_WEBSOCKET);
    }

    @Bean
    public Binding paymentMetricsDlqBinding() {
        return bindDlq(QUEUE_PAYMENT_METRICS);
    }

    // === Event Bindings (queue → exchange → routing keys) ===

    // Product CQRS: product lifecycle + stock events
    @Bean
    public Binding productCqrsProductBinding(Queue productCqrsQueue, TopicExchange exchange) {
        return BindingBuilder.bind(productCqrsQueue).to(exchange).with("product.#");
    }

    @Bean
    public Binding productCqrsStockBinding(Queue productCqrsQueue, TopicExchange exchange) {
        return BindingBuilder.bind(productCqrsQueue).to(exchange).with("stock.#");
    }

    // Order notification: all order events
    @Bean
    public Binding orderNotificationBinding(Queue orderNotificationQueue, TopicExchange exchange) {
        return BindingBuilder.bind(orderNotificationQueue).to(exchange).with("order.#");
    }

    // Order saga: specific order lifecycle events
    @Bean
    public Binding orderSagaCreatedBinding(Queue orderSagaQueue, TopicExchange exchange) {
        return BindingBuilder.bind(orderSagaQueue).to(exchange).with("order.created");
    }

    @Bean
    public Binding orderSagaCancelledBinding(Queue orderSagaQueue, TopicExchange exchange) {
        return BindingBuilder.bind(orderSagaQueue).to(exchange).with("order.cancelled");
    }

    @Bean
    public Binding orderSagaCompletedBinding(Queue orderSagaQueue, TopicExchange exchange) {
        return BindingBuilder.bind(orderSagaQueue).to(exchange).with("order.completed");
    }

    @Bean
    public Binding orderSagaRefundedBinding(Queue orderSagaQueue, TopicExchange exchange) {
        return BindingBuilder.bind(orderSagaQueue).to(exchange).with("order.refunded");
    }

    // Stock reservation
    @Bean
    public Binding stockReservationBinding(Queue stockReservationQueue, TopicExchange exchange) {
        return BindingBuilder.bind(stockReservationQueue).to(exchange).with("stock.reservation.requested");
    }

    // Payment initiation
    @Bean
    public Binding paymentInitiationBinding(Queue paymentInitiationQueue, TopicExchange exchange) {
        return BindingBuilder.bind(paymentInitiationQueue).to(exchange).with("payment.initiation.requested");
    }

    // Audit notification
    @Bean
    public Binding auditNotificationBinding(Queue auditNotificationQueue, TopicExchange exchange) {
        return BindingBuilder.bind(auditNotificationQueue).to(exchange).with("product.audited");
    }

    // Report notification
    @Bean
    public Binding reportNotificationBinding(Queue reportNotificationQueue, TopicExchange exchange) {
        return BindingBuilder.bind(reportNotificationQueue).to(exchange).with("report.#");
    }

    // Message websocket
    @Bean
    public Binding messageWebSocketBinding(Queue messageWebSocketQueue, TopicExchange exchange) {
        return BindingBuilder.bind(messageWebSocketQueue).to(exchange).with("message.recalled");
    }

    // Payment metrics: all payment events
    @Bean
    public Binding paymentMetricsBinding(Queue paymentMetricsQueue, TopicExchange exchange) {
        return BindingBuilder.bind(paymentMetricsQueue).to(exchange).with("payment.#");
    }

    // === Infrastructure Beans ===

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setMandatory(true);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory domainEventContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setPrefetchCount(properties.getConsumer().getPrefetch());
        factory.setConcurrentConsumers(properties.getConsumer().getConcurrency().getMin());
        factory.setMaxConcurrentConsumers(properties.getConsumer().getConcurrency().getMax());
        factory.setDefaultRequeueRejected(properties.getConsumer().isDefaultRequeueRejected());
        factory.setAdviceChain(retryInterceptor());
        return factory;
    }

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate template = new RetryTemplate();
        ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
        backOff.setInitialInterval(properties.getPublisher().getRetry().getInitialInterval());
        backOff.setMultiplier(properties.getPublisher().getRetry().getMultiplier());
        backOff.setMaxInterval(10000L);
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(properties.getPublisher().getRetry().getMaxAttempts());
        template.setBackOffPolicy(backOff);
        template.setRetryPolicy(retryPolicy);
        return template;
    }

    @Bean
    public MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
        return new NewMessageRecoverer(rabbitTemplate);
    }

    // === Helper Methods ===

    private Queue createQueue(String name) {
        return QueueBuilder.durable(name)
            .quorum()
            .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE_NAME)
            .withArgument("x-dead-letter-routing-key", name + ".dlq")
            .build();
    }

    private Queue createDlq(String queueName) {
        return QueueBuilder.durable(queueName + ".dlq")
            .quorum()
            .build();
    }

    private Binding bindDlq(String queueName) {
        return BindingBuilder.bind(new Queue(queueName + ".dlq"))
            .to(new TopicExchange(DLQ_EXCHANGE_NAME))
            .with(queueName + ".dlq");
    }

    private org.aopalliance.intercept.MethodInterceptor retryInterceptor() {
        return new org.springframework.amqp.rabbit.retry.StatelessRetryInterceptorBuilder()
            .retryOperations(retryTemplate())
            .messageRecoverer(messageRecoverer(rabbitTemplate(null, new Jackson2JsonMessageConverter())))
            .build();
    }
}
```

Note: The `retryInterceptor()` method uses Spring AMQP's `StatelessRetryInterceptorBuilder` to add retry with exponential backoff to the consumer container factory. Failed messages after max retries are routed to the DLQ via the `x-dead-letter-exchange` argument.

- [ ] **Step 3: Commit**

```bash
git add easyorange-backend/easyorange-framework/src/main/java/com/cartethyia/easyorange/framework/messaging/config/
git commit -m "refactor(event): per-consumer queues with DLQ and retry configuration"
```

---

### Task 5: Update all consumer classes with new queue names and @RabbitHandler pattern

**Files:**
- Modify: `easyorange-product/src/main/java/com/cartethyia/easyorange/product/application/event/ProductEventConsumer.java`
- Modify: `easyorange-order/src/main/java/com/cartethyia/easyorange/order/adapter/inbound/mq/OrderNotificationEventConsumer.java`
- Modify: `easyorange-application/src/main/java/com/cartethyia/easyorange/adapter/event/StockReservationEventConsumer.java`
- Modify: `easyorange-application/src/main/java/com/cartethyia/easyorange/adapter/event/PaymentInitiationEventConsumer.java`
- Modify: `easyorange-application/src/main/java/com/cartethyia/easyorange/adapter/event/ProductAuditEventConsumer.java`
- Modify: `easyorange-application/src/main/java/com/cartethyia/easyorange/adapter/event/ReportProcessedEventConsumer.java`
- Modify: `easyorange-message/src/main/java/com/cartethyia/easyorange/message/websocket/WebSocketEventConsumer.java`

- [ ] **Step 1: Update ProductEventConsumer — @RabbitListener+@RabbitHandler pattern**

Change from method-level `@RabbitListener` to class-level `@RabbitListener` + method-level `@RabbitHandler`:

```java
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@RabbitListener(queues = RabbitMQConfig.QUEUE_PRODUCT_CQRS, containerFactory = "domainEventContainerFactory")
public class ProductEventConsumer {
    // ... existing fields ...

    @RabbitHandler
    public void onProductCreated(ProductCreatedEvent event) { ... }

    @RabbitHandler
    public void onProductUpdated(ProductUpdatedEvent event) { ... }

    @RabbitHandler
    public void onProductDeleted(ProductDeletedEvent event) { ... }

    @RabbitHandler
    public void onProductMarkedSold(ProductMarkedSoldEvent event) { ... }

    @RabbitHandler
    public void onStockDecreased(StockDecreasedEvent event) { ... }

    @RabbitHandler
    public void onStockRestored(StockRestoredEvent event) { ... }

    // ... existing private methods ...
}
```

- [ ] **Step 2: Update OrderNotificationEventConsumer — @RabbitListener+@RabbitHandler pattern**

```java
@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_NOTIFICATION, containerFactory = "domainEventContainerFactory")
public class OrderNotificationEventConsumer {
    // ... existing fields ...

    @RabbitHandler
    public void onOrderCreated(OrderCreatedEvent event) { ... }

    @RabbitHandler
    public void onOrderPaid(OrderPaidEvent event) { ... }

    @RabbitHandler
    public void onOrderShipped(OrderShippedEvent event) { ... }

    @RabbitHandler
    public void onOrderCompleted(OrderCompletedEvent event) { ... }

    @RabbitHandler
    public void onOrderCancelled(OrderCancelledEvent event) { ... }

    @RabbitHandler
    public void onOrderRefunded(OrderRefundedEvent event) { ... }

    // ... existing private methods ...
}
```

- [ ] **Step 3: Update single-method consumers — just update queue names**

For each single-method consumer, update the `queues` attribute:

| Consumer | Old Queue | New Queue |
|---|---|---|
| `StockReservationEventConsumer` | `eo.product.events` | `RabbitMQConfig.QUEUE_STOCK_RESERVATION` |
| `PaymentInitiationEventConsumer` | `eo.payment.events` | `RabbitMQConfig.QUEUE_PAYMENT_INITIATION` |
| `ProductAuditEventConsumer` | `eo.message.events` | `RabbitMQConfig.QUEUE_AUDIT_NOTIFICATION` |
| `ReportProcessedEventConsumer` | `eo.message.events` | `RabbitMQConfig.QUEUE_REPORT_NOTIFICATION` |
| `WebSocketEventConsumer` | `eo.message.events` | `RabbitMQConfig.QUEUE_MESSAGE_WEBSOCKET` |

- [ ] **Step 4: Run product and order module tests**

Run: `cd easyorange-backend && ./mvnw test -pl easyorange-product,easyorange-order -DexcludedGroups=integration`
Expected: PASS (unit tests don't depend on RabbitMQ)

- [ ] **Step 5: Commit**

```bash
git add easyorange-backend/easyorange-product/src/main/java/com/cartethyia/easyorange/product/application/event/ProductEventConsumer.java
git add easyorange-backend/easyorange-order/src/main/java/com/cartethyia/easyorange/order/adapter/inbound/mq/OrderNotificationEventConsumer.java
git add easyorange-backend/easyorange-application/src/main/java/com/cartethyia/easyorange/adapter/event/
git add easyorange-backend/easyorange-message/src/main/java/com/cartethyia/easyorange/message/websocket/WebSocketEventConsumer.java
git commit -m "refactor(event): update consumers with per-consumer queue names and @RabbitHandler pattern"
```

---

### Task 6: Merge 4 order saga consumers into OrderSagaEventConsumer

**Files:**
- Create: `easyorange-backend/easyorange-order/src/main/java/com/cartethyia/easyorange/order/adapter/inbound/mq/subscriber/OrderSagaEventConsumer.java`
- Delete: `easyorange-backend/easyorange-order/src/main/java/com/cartethyia/easyorange/order/adapter/inbound/mq/subscriber/OrderCreatedEventConsumer.java`
- Delete: `easyorange-backend/easyorange-order/src/main/java/com/cartethyia/easyorange/order/adapter/inbound/mq/subscriber/OrderCancelledEventConsumer.java`
- Delete: `easyorange-backend/easyorange-order/src/main/java/com/cartethyia/easyorange/order/adapter/inbound/mq/subscriber/OrderCompletedEventConsumer.java`
- Delete: `easyorange-backend/easyorange-order/src/main/java/com/cartethyia/easyorange/order/adapter/inbound/mq/subscriber/OrderRefundedEventConsumer.java`
- Modify: `easyorange-backend/easyorange-order/src/test/java/com/cartethyia/easyorange/order/adapter/inbound/mq/subscriber/OrderEventSubscribersTest.java`

- [ ] **Step 1: Create OrderSagaEventConsumer**

```java
package com.cartethyia.easyorange.order.adapter.inbound.mq.subscriber;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import com.cartethyia.easyorange.order.domain.event.*;
import com.cartethyia.easyorange.order.domain.port.ProductInventoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_SAGA, containerFactory = "domainEventContainerFactory")
public class OrderSagaEventConsumer {

    private final DomainEventPublisher domainEventPublisher;
    private final ProductInventoryPort productInventoryPort;

    @RabbitHandler
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("收到订单创建事件: orderId={}, items count={}", event.getOrderId(), event.getItems().size());
        try {
            for (OrderCreatedEvent.OrderItemPayload item : event.getItems()) {
                var stockEvent = new StockReservationRequestedEvent(
                        event.getOrderId(), item.getProductId(), item.getQuantity());
                domainEventPublisher.publish(stockEvent);
            }
            log.info("库存预留请求已发布: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("发布库存预留请求失败: orderId={}", event.getOrderId(), e);
            throw e;
        }
    }

    @RabbitHandler
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("收到订单取消事件: orderId={}, productCount={}", event.getOrderId(), event.getProductIds().size());
        try {
            for (Long productId : event.getProductIds()) {
                productInventoryPort.restoreStock(productId);
                log.info("库存恢复成功: productId={}", productId);
            }
            log.info("订单取消事件处理完成: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("订单取消事件处理失败: orderId={}", event.getOrderId(), e);
            throw e;
        }
    }

    @RabbitHandler
    public void onOrderCompleted(OrderCompletedEvent event) {
        log.info("收到订单完成事件: orderId={}, productCount={}", event.getOrderId(), event.getProductIds().size());
        try {
            for (Long productId : event.getProductIds()) {
                productInventoryPort.markAsSold(productId);
                log.info("商品标记已售成功: productId={}", productId);
            }
            log.info("订单完成事件处理完成: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("订单完成事件处理失败: orderId={}", event.getOrderId(), e);
            throw e;
        }
    }

    @RabbitHandler
    public void onOrderRefunded(OrderRefundedEvent event) {
        log.info("收到订单退款事件: orderId={}, productCount={}", event.getOrderId(), event.getProductIds().size());
        try {
            for (Long productId : event.getProductIds()) {
                productInventoryPort.restoreStock(productId);
                log.info("库存恢复成功: productId={}", productId);
            }
            log.info("订单退款事件处理完成: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("订单退款事件处理失败: orderId={}", event.getOrderId(), e);
            throw e;
        }
    }
}
```

- [ ] **Step 2: Delete the 4 separate consumer files**

```bash
rm easyorange-backend/easyorange-order/src/main/java/com/cartethyia/easyorange/order/adapter/inbound/mq/subscriber/OrderCreatedEventConsumer.java
rm easyorange-backend/easyorange-order/src/main/java/com/cartethyia/easyorange/order/adapter/inbound/mq/subscriber/OrderCancelledEventConsumer.java
rm easyorange-backend/easyorange-order/src/main/java/com/cartethyia/easyorange/order/adapter/inbound/mq/subscriber/OrderCompletedEventConsumer.java
rm easyorange-backend/easyorange-order/src/main/java/com/cartethyia/easyorange/order/adapter/inbound/mq/subscriber/OrderRefundedEventConsumer.java
```

- [ ] **Step 3: Rewrite OrderEventSubscribersTest for OrderSagaEventConsumer**

Replace the test file with tests for the merged consumer:

```java
package com.cartethyia.easyorange.order.adapter.inbound.mq.subscriber;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.order.domain.event.*;
import com.cartethyia.easyorange.order.domain.port.ProductInventoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderSagaEventConsumer 单元测试")
class OrderEventSubscribersTest {

    private static final Long ORDER_ID = 100L;
    private static final Long PRODUCT_ID = 200L;
    private static final Long BUYER_ID = 1L;
    private static final Long SELLER_ID = 2L;

    @Nested
    @DisplayName("OrderSagaEventConsumer")
    class OrderSagaEventConsumerTests {

        @Mock
        private DomainEventPublisher domainEventPublisher;

        @Mock
        private ProductInventoryPort productInventoryPort;

        private OrderSagaEventConsumer consumer;

        @Captor
        private ArgumentCaptor<StockReservationRequestedEvent> stockEventCaptor;

        @BeforeEach
        void setUp() {
            consumer = new OrderSagaEventConsumer(domainEventPublisher, productInventoryPort);
        }

        @Test
        @DisplayName("收到订单创建事件后发布库存预留请求")
        void onOrderCreated_shouldPublishStockReservationRequest() {
            OrderCreatedEvent event = new OrderCreatedEvent(ORDER_ID, BUYER_ID, SELLER_ID,
                    List.of(new OrderCreatedEvent.OrderItemPayload(PRODUCT_ID, 1, BigDecimal.valueOf(99.99), BigDecimal.valueOf(99.99))),
                    BigDecimal.valueOf(99.99));

            consumer.onOrderCreated(event);

            verify(domainEventPublisher).publish(stockEventCaptor.capture());
            StockReservationRequestedEvent captured = stockEventCaptor.getValue();
            assertThat(captured.getOrderId()).isEqualTo(ORDER_ID);
            assertThat(captured.getProductId()).isEqualTo(PRODUCT_ID);
            assertThat(captured.getQuantity()).isEqualTo(1);
        }

        @Test
        @DisplayName("收到订单取消事件后恢复库存")
        void onOrderCancelled_shouldRestoreStock() {
            OrderCancelledEvent event = new OrderCancelledEvent(ORDER_ID, List.of(PRODUCT_ID), "取消原因");

            consumer.onOrderCancelled(event);

            verify(productInventoryPort).restoreStock(PRODUCT_ID);
        }

        @Test
        @DisplayName("收到订单完成事件后标记商品已售")
        void onOrderCompleted_shouldMarkAsSold() {
            OrderCompletedEvent event = new OrderCompletedEvent(ORDER_ID, List.of(PRODUCT_ID));

            consumer.onOrderCompleted(event);

            verify(productInventoryPort).markAsSold(PRODUCT_ID);
        }

        @Test
        @DisplayName("收到订单退款事件后恢复库存")
        void onOrderRefunded_shouldRestoreStock() {
            OrderRefundedEvent event = new OrderRefundedEvent(ORDER_ID, List.of(PRODUCT_ID), "退款原因");

            consumer.onOrderRefunded(event);

            verify(productInventoryPort).restoreStock(PRODUCT_ID);
        }
    }
}
```

- [ ] **Step 4: Run order module tests**

Run: `cd easyorange-backend && ./mvnw test -pl easyorange-order -DexcludedGroups=integration`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add easyorange-backend/easyorange-order/src/main/java/com/cartethyia/easyorange/order/adapter/inbound/mq/subscriber/
git add easyorange-backend/easyorange-order/src/test/java/com/cartethyia/easyorange/order/adapter/inbound/mq/subscriber/OrderEventSubscribersTest.java
git commit -m "refactor(event): merge 4 order saga consumers into OrderSagaEventConsumer"
```

---

### Task 7: Migrate PaymentMetricsListener to PaymentMetricsConsumer

**Files:**
- Create: `easyorange-backend/easyorange-payment/src/main/java/com/cartethyia/easyorange/payment/application/metrics/PaymentMetricsConsumer.java`
- Delete: `easyorange-backend/easyorange-payment/src/main/java/com/cartethyia/easyorange/payment/application/metrics/PaymentMetricsListener.java`

- [ ] **Step 1: Create PaymentMetricsConsumer**

```java
package com.cartethyia.easyorange.payment.application.metrics;

import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import com.cartethyia.easyorange.payment.domain.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@RabbitListener(queues = RabbitMQConfig.QUEUE_PAYMENT_METRICS, containerFactory = "domainEventContainerFactory")
public class PaymentMetricsConsumer {

    private final PaymentMetricsService metricsService;

    @RabbitHandler
    public void onPaymentCreated(PaymentCreatedEvent event) {
        metricsService.recordPaymentCreated();
    }

    @RabbitHandler
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        metricsService.recordPaymentSuccess();
    }

    @RabbitHandler
    public void onPaymentFailed(PaymentFailedEvent event) {
        metricsService.recordPaymentFailed();
    }

    @RabbitHandler
    public void onPaymentRefunded(PaymentRefundedEvent event) {
        metricsService.recordRefund();
    }
}
```

- [ ] **Step 2: Delete PaymentMetricsListener**

```bash
rm easyorange-backend/easyorange-payment/src/main/java/com/cartethyia/easyorange/payment/application/metrics/PaymentMetricsListener.java
```

- [ ] **Step 3: Run payment module tests**

Run: `cd easyorange-backend && ./mvnw test -pl easyorange-payment -DexcludedGroups=integration`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add easyorange-backend/easyorange-payment/src/main/java/com/cartethyia/easyorange/payment/application/metrics/
git commit -m "refactor(event): migrate PaymentMetricsListener to RabbitMQ PaymentMetricsConsumer"
```

---

## Phase 4: Update Integration Test

### Task 8: Update RabbitMQDomainEventPublisherIT for new queue names

**Files:**
- Modify: `easyorange-backend/easyorange-framework/src/test/java/com/cartethyia/easyorange/framework/messaging/RabbitMQDomainEventPublisherIT.java`

- [ ] **Step 1: Update integration test queue names**

The integration test currently reads from `eo.product.events`. Update to use `eo.product.cqrs`:

```java
@Test
void publish_orderCreatedEvent_messageArrivesInProductCqrsQueue() {
    var event = createTestEvent("OrderCreated");
    publisher.publish(event);

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> {
            Message message = rabbitTemplate.receive(RabbitMQConfig.QUEUE_PRODUCT_CQRS, 1000);
            assertThat(message).isNotNull();
        });
}

@Test
void publish_multipleEvents_allArriveInQueues() {
    var event1 = createTestEvent("OrderCreated");
    var event2 = createTestEvent("OrderPaid");

    publisher.publish(event1);
    publisher.publish(event2);

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> {
            List<Message> messages = new ArrayList<>();
            Message msg;
            while ((msg = rabbitTemplate.receive(RabbitMQConfig.QUEUE_PRODUCT_CQRS, 100)) != null) {
                messages.add(msg);
            }
            assertThat(messages).hasSizeGreaterThanOrEqualTo(2);
        });
}
```

Also update `createTestEvent` to work with the simplified `BaseDomainEvent`:

```java
private static BaseDomainEvent createTestEvent(String eventType) {
    return new BaseDomainEvent() {
        @Override
        public String eventType() {
            return eventType;
        }
    };
}
```

- [ ] **Step 2: Commit**

```bash
git add easyorange-backend/easyorange-framework/src/test/java/com/cartethyia/easyorange/framework/messaging/RabbitMQDomainEventPublisherIT.java
git commit -m "test(event): update integration test for per-consumer queue names"
```

---

## Phase 5: Final Verification

### Task 9: Run full test suite and verify

- [ ] **Step 1: Run all unit tests**

Run: `cd easyorange-backend && ./mvnw test -DexcludedGroups=integration`
Expected: ALL PASS

- [ ] **Step 2: Run architecture tests**

Run: `cd easyorange-backend && ./mvnw test -pl easyorange-application -Dtest=ArchitectureRulesTest`
Expected: PASS

- [ ] **Step 3: Run integration tests (requires Docker)**

Run: `cd easyorange-backend && ./mvnw test -pl easyorange-framework -DexcludedGroups=""`
Expected: PASS (requires RabbitMQ Docker container)

- [ ] **Step 4: Final commit (if any fixes needed)**

```bash
git add -A
git commit -m "fix(event): address test failures from event system optimization"
```

---

## Summary of All Changes

| Phase | Task | Files Changed | Risk |
|-------|------|---------------|------|
| 1 | Simplify BaseDomainEvent | 1 file | Low — backward compatible |
| 1 | Update 26 event classes | 26 files | Low — remove overrides, simplify constructors |
| 2 | Convention-based RoutingKeyResolver | 2 files | Medium — routing key format changes |
| 3 | Per-consumer queues + DLQ | 2 files | High — queue topology changes |
| 3 | Update consumer classes | 7 files | High — queue name changes |
| 3 | Merge order saga consumers | 5 files (1 new, 4 deleted) | Medium — logic preserved |
| 3 | Migrate PaymentMetricsListener | 2 files (1 new, 1 deleted) | Low — was dead code |
| 4 | Update integration test | 1 file | Low |
| 5 | Full verification | 0 files | N/A |

**Deployment Note**: Phases 2-3 change routing key format and queue topology. Deploy during a maintenance window after draining existing queues. The old queues (`eo.product.events`, `eo.order.events`, `eo.payment.events`, `eo.message.events`) must be deleted before starting the new version.