package com.cartethyia.easyorange.adapter.event;

import com.cartethyia.easyorange.favorite.application.service.FavoriteService;
import com.cartethyia.easyorange.framework.event.core.EventConsumerHandler;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import com.cartethyia.easyorange.product.domain.event.ProductUpdatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 收藏降价提醒消费者 — 监听商品更新事件，比对收藏价格快照，降价时通知收藏者。
 * <p>
 * 事件载荷自包含新价格（{@link ProductUpdatedEvent#data()}），无需回查商品；
 * 幂等由框架 {@link EventConsumerHandler} 去重 + 收藏快照 CAS 更新双保险。
 * 消费者位于 application 组装层（跨业务模块事件消费的既定模式，见 OrderNotificationEventConsumer）。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@RabbitListener(queues = RabbitMQConfig.QUEUE_FAVORITE_PRICE_DROP)
public class FavoritePriceDropEventConsumer {

    private final EventConsumerHandler handler;
    private final FavoriteService favoriteService;

    public FavoritePriceDropEventConsumer(
            EventIdempotencyChecker idempotencyChecker,
            EventMetricsService metricsService,
            FavoriteService favoriteService) {
        this.handler = new EventConsumerHandler(getClass().getSimpleName(), idempotencyChecker, metricsService);
        this.favoriteService = favoriteService;
    }

    @RabbitHandler
    public void onProductUpdated(ProductUpdatedEvent event, Message message) {
        handler.handle(
                event,
                message,
                () -> favoriteService.processPriceDrop(
                        event.productId(), event.data().name(), event.data().price()));
    }
}
