package com.cartethyia.easyorange.product.adapter.inbound.messaging;

import com.cartethyia.easyorange.framework.event.core.EventConsumerHandler;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import com.cartethyia.easyorange.product.application.query.ProductQueryHandler;
import com.cartethyia.easyorange.product.domain.event.*;
import com.cartethyia.easyorange.product.domain.port.ProductCacheEvictionPort;
import com.cartethyia.easyorange.product.domain.port.ProductNotificationPort;
import com.cartethyia.easyorange.product.domain.port.ProductSearchIndexPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@RabbitListener(queues = RabbitMQConfig.QUEUE_PRODUCT_CQRS, containerFactory = "domainEventContainerFactory")
public class ProductEventConsumer {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final EventConsumerHandler handler;
    private final ProductCacheEvictionPort productCachePort;
    private final ProductQueryHandler productQueryHandler;
    private final ProductNotificationPort notificationPort;
    private final ProductSearchIndexPort searchIndexPort;

    public ProductEventConsumer(
            EventIdempotencyChecker idempotencyChecker,
            EventMetricsService metricsService,
            ProductCacheEvictionPort productCachePort,
            ProductQueryHandler productQueryHandler,
            @Autowired(required = false) ProductNotificationPort notificationPort,
            @Autowired(required = false) ProductSearchIndexPort searchIndexPort) {
        this.handler = new EventConsumerHandler(getClass().getSimpleName(), idempotencyChecker, metricsService, false);
        this.productCachePort = productCachePort;
        this.productQueryHandler = productQueryHandler;
        this.notificationPort = notificationPort;
        this.searchIndexPort = searchIndexPort;
    }

    @RabbitHandler
    void handle(ProductEvent event, Message message) {
        handler.handle(event, message, _ -> {
            switch (event) {
                case ProductCreatedEvent e -> handleCreated(e);
                case ProductUpdatedEvent e -> handleUpdated(e);
                case ProductDeletedEvent e -> handleDeleted(e);
                case ProductMarkedSoldEvent e -> handleMarkedSold(e);
                case StockDecreasedEvent e -> checkLowStock(e.productId());
                case StockRestoredEvent e -> handleStockRestored(e);
                case ProductSubmittedForReviewEvent e -> productCachePort.evictProductCache(e.productId());
                case ProductPutOnlineEvent e -> handlePutOnline(e);
                case ProductTakeOfflineEvent e -> handleTakeOffline(e);
                default ->
                    log.debug("No handler for event: {}", event.getClass().getSimpleName());
            }
        });
    }

    private void handleCreated(ProductCreatedEvent e) {
        var productId = e.productId();
        if (e.data().categoryId() != null) {
            productCachePort.evictProductListCache(e.data().categoryId());
        }
        if (notificationPort != null)
            tryRun(() ->
                    notificationPort.notifyProductCreated(productId, e.data().userId()));
        if (searchIndexPort != null) tryRun(() -> searchIndexPort.indexProduct(productId));
    }

    private void handleUpdated(ProductUpdatedEvent e) {
        if (e.data().categoryId() != null) {
            productCachePort.evictProductListCache(e.data().categoryId());
        }
        if (searchIndexPort != null) tryRun(() -> searchIndexPort.updateProductIndex(e.productId()));
    }

    private void handleDeleted(ProductDeletedEvent e) {
        productCachePort.evictProductCache(e.productId());
        if (searchIndexPort != null) tryRun(() -> searchIndexPort.removeProductIndex(e.productId()));
    }

    private void handleMarkedSold(ProductMarkedSoldEvent e) {
        var productId = e.productId();
        productCachePort.evictProductCache(productId);
        if (notificationPort != null) tryRun(() -> notificationPort.notifyProductMarkedSold(productId, e.sellerId()));
        if (searchIndexPort != null) tryRun(() -> searchIndexPort.updateProductIndex(productId));
    }

    private void handleStockRestored(StockRestoredEvent e) {
        productCachePort.evictProductCache(e.productId());
        if (searchIndexPort != null) tryRun(() -> searchIndexPort.updateProductIndex(e.productId()));
    }

    private void handlePutOnline(ProductPutOnlineEvent e) {
        productCachePort.evictProductCache(e.productId());
        if (searchIndexPort != null) tryRun(() -> searchIndexPort.indexProduct(e.productId()));
    }

    private void handleTakeOffline(ProductTakeOfflineEvent e) {
        productCachePort.evictProductCache(e.productId());
        if (searchIndexPort != null) tryRun(() -> searchIndexPort.removeProductIndex(e.productId()));
    }

    private void checkLowStock(String productId) {
        try {
            var readModel = productQueryHandler.getProductReadModel(productId);
            if (readModel != null && readModel.stock() != null && readModel.stock() <= LOW_STOCK_THRESHOLD) {
                log.warn(
                        "event=LowStockWarning productId={} currentStock={} threshold={}",
                        productId,
                        readModel.stock(),
                        LOW_STOCK_THRESHOLD);
                if (notificationPort != null) {
                    notificationPort.notifyLowStock(productId, readModel.sellerId(), readModel.stock());
                }
            }
        } catch (Exception e) {
            log.error("event=checkLowStockFailed productId={}", productId, e);
        }
    }

    private static void tryRun(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            log.error("Port call failed", e);
        }
    }
}
