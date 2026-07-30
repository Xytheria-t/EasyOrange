package com.cartethyia.easyorange.framework.event.core;

/**
 * @deprecated 使用 {@link EventConsumerHandler} 组合代替继承。
 * 消费者改用 {@code private final EventConsumerHandler handler}
 * 并在 {@code @RabbitHandler} 方法中调用 {@code handler.handle(event, message, metadata -> ...)}。
 */
@Deprecated(forRemoval = true)
public abstract class AbstractDomainEventConsumer {
}
