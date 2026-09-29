package com.cartethyia.easyorange.common.event;

/**
 * 领域事件标记接口。实现应为 record，Jackson 反序列化由 {@code ParameterNamesModule} + {@code -parameters}
 * 编译选项处理，无需 {@code @JsonCreator}。
 * <p>
 * eventId 由事件实例自身携带（UUID v7，创建时生成），随消息体传输——outbox 重投 / DLQ 重投时 ID 保持不变，
 * 消费端 {@code EventConsumerHandler} 据此幂等去重；traceId / occurredOn 走 message headers。
 */
public interface DomainEvent {

    /**
     * 事件唯一 ID（UUID v7，创建时生成）— 消费端幂等键；实现须为 record 的首个组件 {@code String eventId}。
     */
    String eventId();

    /** 事件类型名（去 "Event" 后缀）— RabbitMQ 路由的判别依据，不用 {@code @JsonTypeInfo}。 */
    default String eventType() {
        String simpleName = getClass().getSimpleName();
        return simpleName.endsWith("Event") ? simpleName.substring(0, simpleName.length() - 5) : simpleName;
    }

    /** 聚合根主键，如 {@code orderId()} / {@code productId()}。 */
    String aggregateId();
}
