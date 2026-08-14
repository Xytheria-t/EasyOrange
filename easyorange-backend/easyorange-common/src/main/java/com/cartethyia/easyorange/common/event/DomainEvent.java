package com.cartethyia.easyorange.common.event;

/**
 * 领域事件标记接口。
 * <p>
 * 事件类应为 record 实现，Jackson 反序列化由 {@code ParameterNamesModule} + {@code -parameters} 编译选项处理，
 * 无需 {@code @JsonCreator}。
 * <p>
 * 类型判别（RabbitMQ 路由）使用事件类 simple name 去掉 "Event" 后缀，而非 {@code @JsonTypeInfo}。
 * <p>
 * 事件元数据：eventId 由事件实例自身携带（UUID v7，事件创建时生成），随消息体传输——
 * outbox 重投 / DLQ 重投时 ID 保持不变，消费端 {@code EventConsumerHandler} 基于
 * {@link #eventId()} 幂等去重，而非事件的语义内容。traceId / occurredOn 通过 message headers 传输。
 */
public interface DomainEvent {

    /**
     * 事件唯一 ID（UUID v7，事件实例创建时生成），用于跨服务追踪与消费端幂等去重。
     * 实现应为 record 的第一个组件 {@code String eventId}，构造时经 {@code UuidV7.generateId()} 生成。
     */
    String eventId();

    /**
     * 事件类型名（去 "Event" 后缀）。
     * 示例：OrderCreatedEvent → "OrderCreated"，ProductCreatedEvent → "ProductCreated"
     */
    default String eventType() {
        String simpleName = getClass().getSimpleName();
        return simpleName.endsWith("Event") ? simpleName.substring(0, simpleName.length() - 5) : simpleName;
    }

    /**
     * 聚合根标识，用于事件追踪与路由。
     * <p>
     * 实现应返回事件所作用聚合根的主键，如 {@code orderId()} / {@code productId()}。
     */
    String aggregateId();
}
