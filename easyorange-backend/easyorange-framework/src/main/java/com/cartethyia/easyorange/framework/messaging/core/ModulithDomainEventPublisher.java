package com.cartethyia.easyorange.framework.messaging.core;

import com.cartethyia.easyorange.common.event.DomainEvent;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.framework.messaging.config.EventExternalizationConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Spring Modulith 领域事件发布器 — Outbox 同事务双写的入口。
 * <p>
 * 底层委托给 {@link ApplicationEventPublisher} 完成发布，发布行为会被
 * Spring Modulith 的事件发布注册表拦截。注册表会在调用方同一个数据库事务内
 * 将事件写入 EVENT_PUBLICATION 数据表，事务提交后再异步将事件投递到 RabbitMQ。
 * <p>
 * 该方案保证消息至少投递一次：如果数据库事务提交完成，但消息还未发送至 RabbitMQ 时应用宕机，
 * 应用重启后会自动重试未发送成功的事件（由配置项
 * {@code spring.modulith.events.republish-outstanding-events-on-restart} 控制）。
 * <p>
 * 本类是 {@link DomainEventPublisher} 接口唯一标注 {@code @Primary} 的主实现类，
 * 通过 {@link EventExternalizationConfig} 完成事件向外中间件的转发配置。
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ModulithDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publish(DomainEvent event) {
        log.debug("通过Spring Modulith发布领域事件：{}", event.getClass().getSimpleName());
        eventPublisher.publishEvent(event);
    }
}
