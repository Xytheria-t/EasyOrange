package com.cartethyia.easyorange.common.event;

/**
 * 领域事件标记接口。
 * <p>
 * 事件类应为 record 实现，Jackson 反序列化由 {@code ParameterNamesModule} + {@code -parameters} 编译选项处理，
 * 无需 {@code @JsonCreator}。
 * <p>
 * 类型判别（RabbitMQ 路由）使用事件类 simple name 去掉 "Event" 后缀，而非 {@code @JsonTypeInfo}。
 * <p>
 * 事件元数据（eventId / occurredOn / traceId）通过 RabbitMQ message headers 传输，
 * 由 {@code EventMetadataMessagePostProcessor} 在发布时注入，{@code AbstractDomainEventConsumer} 在消费时读取。
 */
public interface DomainEvent {

    /**
     * 事件类型名（去 "Event" 后缀）。
     * 示例：OrderCreatedEvent → "OrderCreated"，ProductCreated → "ProductCreated"
     */
    default String eventType() {
        String simpleName = getClass().getSimpleName();
        return simpleName.endsWith("Event")
                ? simpleName.substring(0, simpleName.length() - 5)
                : simpleName;
    }

    /**
     * 聚合根标识，用于幂等键生成与分区路由。
     * <p>
     * 实现应返回事件所作用聚合根的主键，如 {@code orderId()} / {@code productId()}。
     */
    String aggregateId();

    /**
     * 事件 schema 版本，默认 1。schema 演进时递增以保持向后兼容。
     */
    default int version() {
        return 1;
    }

    /**
     * 幂等键，默认由 eventType + aggregateId + version 拼接。
     * <p>
     * 复合事件（如 {@code StockReservationRequestedEvent} 涉及 order×product）可重写返回更精确的复合键。
     */
    default String idempotencyKey() {
        return eventType() + ":" + aggregateId() + ":v" + version();
    }
}
