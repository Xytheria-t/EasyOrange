package com.cartethyia.easyorange.adapter.event;

import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import com.cartethyia.easyorange.message.websocket.WebSocketNotifier;
import com.cartethyia.easyorange.product.domain.event.OfferAcceptedEvent;
import com.cartethyia.easyorange.product.domain.event.OfferCounteredEvent;
import com.cartethyia.easyorange.product.domain.event.OfferRejectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@RabbitListener(
    queues = RabbitMQConfig.QUEUE_OFFER_EVENTS,
    containerFactory = "domainEventContainerFactory"
)
public class OfferEventConsumer {

    private final WebSocketNotifier webSocketNotifier;

    @RabbitHandler
    public void onOfferAccepted(OfferAcceptedEvent event) {
        log.info("收到出价接受事件: productId={}, buyerId={}, acceptedPrice={}",
                event.getProductId(), event.getBuyerId(), event.getAcceptedPrice());
        try {
            webSocketNotifier.notifyOfferAccepted(
                    event.getBuyerId(), event.getProductId(), event.getAcceptedPrice());
            log.info("出价接受通知已推送: buyerId={}, productId={}", event.getBuyerId(), event.getProductId());
        } catch (Exception e) {
            log.error("出价接受通知推送失败: buyerId={}, productId={}", event.getBuyerId(), event.getProductId(), e);
            throw e;
        }
    }

    @RabbitHandler
    public void onOfferRejected(OfferRejectedEvent event) {
        log.info("收到出价拒绝事件: productId={}, buyerId={}, offerPrice={}",
                event.getProductId(), event.getBuyerId(), event.getOfferPrice());
        try {
            webSocketNotifier.notifyOfferRejected(
                    event.getBuyerId(), event.getProductId(), event.getOfferPrice());
            log.info("出价拒绝通知已推送: buyerId={}, productId={}", event.getBuyerId(), event.getProductId());
        } catch (Exception e) {
            log.error("出价拒绝通知推送失败: buyerId={}, productId={}", event.getBuyerId(), event.getProductId(), e);
            throw e;
        }
    }

    @RabbitHandler
    public void onOfferCountered(OfferCounteredEvent event) {
        log.info("收到还价事件: productId={}, buyerId={}, counterPrice={}",
                event.getProductId(), event.getBuyerId(), event.getCounterPrice());
        try {
            webSocketNotifier.notifyCounterOffer(
                    event.getBuyerId(), event.getProductId(), event.getCounterPrice());
            log.info("还价通知已推送: buyerId={}, productId={}", event.getBuyerId(), event.getProductId());
        } catch (Exception e) {
            log.error("还价通知推送失败: buyerId={}, productId={}", event.getBuyerId(), event.getProductId(), e);
            throw e;
        }
    }
}
