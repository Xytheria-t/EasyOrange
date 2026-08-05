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
 * 由 {@code EventMetadataMessagePostProcessor} 在发布时注入，消费端 {@code EventConsumerHandler} 在消费时读取。
 * 事件幂等去重基于 {@code metadata.eventId()}（UUID v7，每事件实例唯一），而非事件的语义内容。
 */
public interface DomainEvent {

    /**
     * 事件类型名（去 "Event" 后缀）。
     * 示例：OrderCreatedEvent → "OrderCreated"，ProductCreatedEvent → "ProductCreated"
     */
    default String eventType() {
        String simpleName = getClass().getSimpleName();
        return simpleName.endsWith("Event")
                ? simpleName.substring(0, simpleName.length() - 5)
                : simpleName;
    }

    /**
     * 聚合根标识，用于事件追踪与路由。
     * <p>
     * 实现应返回事件所作用聚合根的主键，如 {@code orderId()} / {@code productId()}。
     */
    String aggregateId();
}
