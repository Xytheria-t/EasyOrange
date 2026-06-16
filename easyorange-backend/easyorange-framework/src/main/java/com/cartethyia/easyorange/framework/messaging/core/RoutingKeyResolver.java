package com.cartethyia.easyorange.framework.messaging.core;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import org.springframework.stereotype.Component;

@Component
public class RoutingKeyResolver {

    /**
     * Derives a routing key from the event type using convention:
     * camelCase → dot.case lowercase.
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
