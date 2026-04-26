package com.cartethyia.easyorange.common.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 基于 Spring 的领域事件发布器实现
 * <p>
 * 使用 Spring ApplicationEventPublisher 实现领域事件的发布。
 * 支持同步和异步发布模式（通过 Spring 配置）。
 * </p>
 *
 * @author cartethyia
 */
@Component
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher publisher;

    public SpringDomainEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(BaseDomainEvent event) {
        Objects.requireNonNull(event, "Event cannot be null");
        publisher.publishEvent(event);
    }

    @Override
    public void publishAll(List<BaseDomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        events.forEach(this::publish);
    }
}
