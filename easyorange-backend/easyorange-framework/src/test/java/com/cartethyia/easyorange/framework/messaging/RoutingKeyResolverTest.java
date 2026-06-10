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
        "OrderCreated, order.aggregate.created",
        "OrderCancelled, order.aggregate.cancelled",
        "OrderPaid, order.aggregate.paid",
        "OrderCompleted, order.aggregate.completed",
        "OrderRefunded, order.aggregate.refunded",
        "OrderShipped, order.aggregate.shipped",
        "StockReservationRequested, order.stock.reservation-requested",
        "PaymentInitiationRequested, order.payment.initiation-requested",
        "ProductCreated, product.aggregate.created",
        "ProductUpdated, product.aggregate.updated",
        "ProductDeleted, product.aggregate.deleted",
        "ProductMarkedSold, product.aggregate.marked-sold",
        "ProductSubmittedForReview, product.aggregate.submitted-for-review",
        "ProductAudited, product.audit.completed",
        "ReportProcessed, product.report.processed",
        "StockDecreased, product.stock.decreased",
        "StockRestored, product.stock.restored",
        "PaymentCreated, payment.transaction.created",
        "PaymentSucceeded, payment.transaction.succeeded",
        "PaymentFailed, payment.transaction.failed",
        "PaymentRefunded, payment.transaction.refunded",
        "PaymentClosed, payment.transaction.closed",
        "MessageSent, message.aggregate.sent",
        "MessageRead, message.aggregate.read",
        "MessageDeleted, message.aggregate.deleted",
        "MessageRecalled, message.aggregate.recalled"
    })
    @DisplayName("resolve known event types returns correct routing keys")
    void resolve_knownEventType_returnsCorrectRoutingKey(String eventType, String expectedRoutingKey) {
        var event = createTestEvent(eventType);
        assertThat(resolver.resolve(event)).isEqualTo(expectedRoutingKey);
    }

    @Test
    @DisplayName("resolve unknown event type throws IllegalArgumentException")
    void resolve_unknownEventType_throwsException() {
        var unknownEvent = createTestEvent("UnknownEvent");
        assertThatThrownBy(() -> resolver.resolve(unknownEvent))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No routing key defined for event type: UnknownEvent");
    }

    @Test
    @DisplayName("hasRoutingKey returns true for known event type")
    void hasRoutingKey_knownEventType_returnsTrue() {
        assertThat(resolver.hasRoutingKey("OrderCreated")).isTrue();
        assertThat(resolver.hasRoutingKey("PaymentFailed")).isTrue();
        assertThat(resolver.hasRoutingKey("MessageRecalled")).isTrue();
    }

    @Test
    @DisplayName("hasRoutingKey returns false for unknown event type")
    void hasRoutingKey_unknownEventType_returnsFalse() {
        assertThat(resolver.hasRoutingKey("UnknownEvent")).isFalse();
        assertThat(resolver.hasRoutingKey("")).isFalse();
    }

    @Test
    @DisplayName("getRegisteredEventTypes returns all 26 event types")
    void getRegisteredEventTypes_returnsAllEventTypes() {
        var eventTypes = resolver.getRegisteredEventTypes();
        assertThat(eventTypes).hasSize(26);
        assertThat(eventTypes).containsExactlyInAnyOrder(
            "OrderCreated", "OrderCancelled", "OrderPaid", "OrderCompleted",
            "OrderRefunded", "OrderShipped", "StockReservationRequested",
            "PaymentInitiationRequested", "ProductCreated", "ProductUpdated",
            "ProductDeleted", "ProductMarkedSold", "ProductSubmittedForReview",
            "ProductAudited", "ReportProcessed",
            "StockDecreased", "StockRestored", "PaymentCreated", "PaymentSucceeded",
            "PaymentFailed", "PaymentRefunded", "PaymentClosed", "MessageSent",
            "MessageRead", "MessageDeleted", "MessageRecalled"
        );
    }

    private static BaseDomainEvent createTestEvent(String eventType) {
        return new BaseDomainEvent(RoutingKeyResolverTest.class) {
            @Override
            public String eventType() {
                return eventType;
            }
        };
    }
}
