package com.cartethyia.easyorange.framework.messaging.config;

import com.cartethyia.easyorange.common.event.DomainEvent;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.modulith.events.EventExternalizationConfiguration;
import org.springframework.modulith.events.RoutingTarget;

/**
 * 配置 Spring Modulith 事件外部化到 RabbitMQ。
 * <p>
 * 所有实现 {@link DomainEvent} 的领域事件都会外部化到
 * {@value RabbitMQConfig#EXCHANGE_NAME} Topic Exchange。路由键由事件类名按约定推导：
 * CamelCase → 全小写点分（如 {@code OrderCreatedEvent} → {@code order.created}）。
 * <p>
 * 本配置在 RabbitMQ 启用时生效（默认）。禁用时事件仍持久化到 {@code EVENT_PUBLICATION} 表，
 * 但不会外部化——便于本地开发时无需启动 RabbitMQ。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EventExternalizationConfig {

    @Bean
    EventExternalizationConfiguration eventExternalizationConfiguration() {
        return EventExternalizationConfiguration.externalizing()
                .selectByType(DomainEvent.class)
                .route(
                        DomainEvent.class,
                        event -> RoutingTarget.forTarget(RabbitMQConfig.EXCHANGE_NAME)
                                .andKey(toRoutingKey(event)))
                .build();
    }

    /**
     * 将领域事件类型名转换为 RabbitMQ 路由键。
     * 约定：CamelCase → 全小写点分。
     * <p>
     * "Event" 后缀已由 {@link DomainEvent#eventType()} 剥离。
     * <p>
     * 示例：
     * <pre>
     *   OrderCreated        → "order.created"
     *   ProductMarkedSold   → "product.marked.sold"
     * </pre>
     */
    private static String toRoutingKey(DomainEvent event) {
        String typeName = event.eventType();
        return typeName.replaceAll("([a-z])([A-Z0-9])", "$1.$2")
                .replaceAll("([0-9])([A-Z])", "$1.$2")
                .toLowerCase();
    }
}
