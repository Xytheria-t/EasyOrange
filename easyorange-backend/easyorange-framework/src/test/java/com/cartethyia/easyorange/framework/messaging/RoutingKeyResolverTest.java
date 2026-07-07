package com.cartethyia.easyorange.framework.messaging;

import com.cartethyia.easyorange.common.event.DomainEvent;
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
    @DisplayName("resolve unknown event type produces convention-based key (no exception)")
    void resolve_unknownEventType_producesConventionKey() {
        var unknownEvent = createTestEvent("UserRegistered");
        assertThat(resolver.resolve(unknownEvent)).isEqualTo("user.registered");
    }

    @Test
    @DisplayName("resolve single-word event type")
    void resolve_singleWordEventType() {
        var event = createTestEvent("Login");
        assertThat(resolver.resolve(event)).isEqualTo("login");
    }

    private static DomainEvent createTestEvent(String eventType) {
        return new DomainEvent() {
            @Override
            public String eventType() {
                return eventType;
            }
        };
    }
}
