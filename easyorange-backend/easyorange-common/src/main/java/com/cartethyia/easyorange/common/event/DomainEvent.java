package com.cartethyia.easyorange.common.event;

/**
 * Marker interface for all domain events.
 * <p>
 * Subclasses should be Java records implementing this interface.
 * Jackson deserialization is handled by {@code ParameterNamesModule} +
 * {@code -parameters} compiler flag — no {@code @JsonCreator} needed.
 * <p>
 * Type discrimination for RabbitMQ routing is done via the event class
 * simple name (stripped of "Event" suffix), not via {@code @JsonTypeInfo}.
 */
public interface DomainEvent {

    /**
     * Returns the event type name derived from the class simple name.
     * Strips the trailing "Event" suffix if present.
     * <p>
     * Examples:
     *   OrderCreatedEvent → "OrderCreated"
     *   ProductCreated  → "ProductCreated"
     */
    default String eventType() {
        String simpleName = getClass().getSimpleName();
        if (simpleName.endsWith("Event")) {
            return simpleName.substring(0, simpleName.length() - 5);
        }
        return simpleName;
    }
}
