package com.cartethyia.easyorange.framework.messaging.core;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class RoutingKeyResolver {

    private static final Map<String, String> EVENT_ROUTING_KEYS = Map.ofEntries(
        // Order aggregate events
        Map.entry("OrderCreated", "order.aggregate.created"),
        Map.entry("OrderCancelled", "order.aggregate.cancelled"),
        Map.entry("OrderPaid", "order.aggregate.paid"),
        Map.entry("OrderCompleted", "order.aggregate.completed"),
        Map.entry("OrderRefunded", "order.aggregate.refunded"),
        Map.entry("OrderShipped", "order.aggregate.shipped"),

        // Order domain events
        Map.entry("StockReservationRequested", "order.stock.reservation-requested"),
        Map.entry("PaymentInitiationRequested", "order.payment.initiation-requested"),

        // Product aggregate events
        Map.entry("ProductCreated", "product.aggregate.created"),
        Map.entry("ProductUpdated", "product.aggregate.updated"),
        Map.entry("ProductDeleted", "product.aggregate.deleted"),
        Map.entry("ProductMarkedSold", "product.aggregate.marked-sold"),
        Map.entry("ProductSubmittedForReview", "product.aggregate.submitted-for-review"),

        // Product stock events
        Map.entry("StockDecreased", "product.stock.decreased"),
        Map.entry("StockRestored", "product.stock.restored"),

        // Payment transaction events
        Map.entry("PaymentCreated", "payment.transaction.created"),
        Map.entry("PaymentSucceeded", "payment.transaction.succeeded"),
        Map.entry("PaymentFailed", "payment.transaction.failed"),
        Map.entry("PaymentRefunded", "payment.transaction.refunded"),
        Map.entry("PaymentClosed", "payment.transaction.closed"),

        // Message events
        Map.entry("MessageSent", "message.aggregate.sent"),
        Map.entry("MessageRead", "message.aggregate.read"),
        Map.entry("MessageDeleted", "message.aggregate.deleted"),
        Map.entry("MessageRecalled", "message.aggregate.recalled")
    );

    public String resolve(BaseDomainEvent event) {
        String routingKey = EVENT_ROUTING_KEYS.get(event.eventType());
        if (routingKey == null) {
            throw new IllegalArgumentException(
                "No routing key defined for event type: " + event.eventType()
            );
        }
        return routingKey;
    }

    public boolean hasRoutingKey(String eventType) {
        return EVENT_ROUTING_KEYS.containsKey(eventType);
    }

    public Set<String> getRegisteredEventTypes() {
        return Set.copyOf(EVENT_ROUTING_KEYS.keySet());
    }
}
