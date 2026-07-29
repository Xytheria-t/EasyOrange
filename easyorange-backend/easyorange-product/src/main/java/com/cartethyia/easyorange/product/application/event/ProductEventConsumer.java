package com.cartethyia.easyorange.product.application.event;

import com.cartethyia.easyorange.common.event.DomainEvent;
import com.cartethyia.easyorange.framework.bloom.BloomFilter;
import com.cartethyia.easyorange.framework.event.core.AbstractDomainEventConsumer;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.framework.event.metadata.EventMetadata;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import com.cartethyia.easyorange.product.adapter.outbound.cache.ProductCacheConstant;
import com.cartethyia.easyorange.product.application.query.ProductQueryService;
import com.cartethyia.easyorange.product.domain.event.*;
import com.cartethyia.easyorange.product.domain.port.ProductCacheEvictionPort;
import com.cartethyia.easyorange.product.domain.port.ProductNotificationPort;
import com.cartethyia.easyorange.product.domain.port.ProductSearchIndexPort;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@RabbitListener(queues = RabbitMQConfig.QUEUE_PRODUCT_CQRS, containerFactory = "domainEventContainerFactory")
public class ProductEventConsumer extends AbstractDomainEventConsumer {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final ProductCacheEvictionPort productCachePort;
    private final ProductQueryService productQueryService;
    private final ProductNotificationPort notificationPort;
    private final ProductSearchIndexPort searchIndexPort;
    private final BloomFilter bloomFilter;

    public ProductEventConsumer(EventIdempotencyChecker idempotencyChecker,
                                 EventMetricsService metricsService,
                                 ProductCacheEvictionPort productCachePort,
                                 ProductQueryService productQueryService,
                                 Optional<ProductNotificationPort> notificationPort,
                                 Optional<ProductSearchIndexPort> searchIndexPort,
                                 BloomFilter bloomFilter) {
        super(idempotencyChecker, metricsService, false);
        this.productCachePort = productCachePort;
        this.productQueryService = productQueryService;
        this.notificationPort = notificationPort.orElse(null);
        this.searchIndexPort = searchIndexPort.orElse(null);
        this.bloomFilter = bloomFilter;
    }

    @RabbitHandler
    public void onProductCreated(ProductCreatedEvent event, Message message) { handle(event, message); }

    @RabbitHandler
    public void onProductUpdated(ProductUpdatedEvent event, Message message) { handle(event, message); }

    @RabbitHandler
    public void onProductDeleted(ProductDeletedEvent event, Message message) { handle(event, message); }

    @RabbitHandler
    public void onProductMarkedSold(ProductMarkedSoldEvent event, Message message) { handle(event, message); }

    @RabbitHandler
    public void onStockDecreased(StockDecreasedEvent event, Message message) { handle(event, message); }

    @RabbitHandler
    public void onStockRestored(StockRestoredEvent event, Message message) { handle(event, message); }

    @RabbitHandler
    public void onProductSubmittedForReview(ProductSubmittedForReviewEvent event, Message message) { handle(event, message); }

    @RabbitHandler
    public void onProductPutOnline(ProductPutOnlineEvent event, Message message) { handle(event, message); }

    @RabbitHandler
    public void onProductTakeOffline(ProductTakeOfflineEvent event, Message message) { handle(event, message); }

    @Override
    protected void doHandle(DomainEvent event, EventMetadata metadata) {
        switch (event) {
            case ProductCreatedEvent e -> handleCreated(e);
            case ProductUpdatedEvent e -> handleUpdated(e);
            case ProductDeletedEvent e -> handleDeleted(e);
            case ProductMarkedSoldEvent e -> handleMarkedSold(e);
            case StockDecreasedEvent e -> handleStockDecreased(e);
            case StockRestoredEvent e -> handleStockRestored(e);
            case ProductSubmittedForReviewEvent e -> handleSubmittedForReview(e);
            case ProductPutOnlineEvent e -> handlePutOnline(e);
            case ProductTakeOfflineEvent e -> handleTakeOffline(e);
            default -> throw new IllegalStateException("不支持的事件: " + event.getClass());
        }
    }

    private void handleCreated(ProductCreatedEvent e) {
        var productId = e.productId();
        var data = e.data();
        bloomFilter.put(ProductCacheConstant.PRODUCT_BLOOM_KEY, productId);
        evictListCache(data.categoryId());
        if (notificationPort != null) safeCall(() -> notificationPort.notifyProductCreated(productId, data.userId()), "notifyProductCreated", productId);
        if (searchIndexPort != null) safeCall(() -> searchIndexPort.indexProduct(productId), "indexProduct", productId);
    }

    private void handleUpdated(ProductUpdatedEvent e) {
        var productId = e.productId();
        evictListCache(e.data().categoryId());
        if (searchIndexPort != null) safeCall(() -> searchIndexPort.updateProductIndex(productId), "updateProductIndex", productId);
    }

    private void handleDeleted(ProductDeletedEvent e) {
        var productId = e.productId();
        productCachePort.evictProductCache(productId);
        if (searchIndexPort != null) safeCall(() -> searchIndexPort.removeProductIndex(productId), "removeProductIndex", productId);
    }

    private void handleMarkedSold(ProductMarkedSoldEvent e) {
        var productId = e.productId();
        productCachePort.evictProductCache(productId);
        if (notificationPort != null) safeCall(() -> notificationPort.notifyProductMarkedSold(productId, e.sellerId()), "notifyProductMarkedSold", productId);
        if (searchIndexPort != null) safeCall(() -> searchIndexPort.updateProductIndex(productId), "updateProductIndex", productId);
    }

    private void handleStockDecreased(StockDecreasedEvent e) {
        checkLowStock(e.productId());
    }

    private void handleStockRestored(StockRestoredEvent e) {
        var productId = e.productId();
        productCachePort.evictProductCache(productId);
        if (searchIndexPort != null) safeCall(() -> searchIndexPort.updateProductIndex(productId), "updateProductIndex", productId);
    }

    private void handleSubmittedForReview(ProductSubmittedForReviewEvent e) {
        productCachePort.evictProductCache(e.productId());
    }

    private void handlePutOnline(ProductPutOnlineEvent e) {
        var productId = e.productId();
        productCachePort.evictProductCache(productId);
        if (searchIndexPort != null) safeCall(() -> searchIndexPort.indexProduct(productId), "indexProduct", productId);
    }

    private void handleTakeOffline(ProductTakeOfflineEvent e) {
        var productId = e.productId();
        productCachePort.evictProductCache(productId);
        if (searchIndexPort != null) safeCall(() -> searchIndexPort.removeProductIndex(productId), "removeProductIndex", productId);
    }

    private void evictListCache(String categoryId) {
        if (categoryId != null) productCachePort.evictProductListCache(categoryId);
    }

    private void checkLowStock(String productId) {
        try {
            var readModel = productQueryService.getProductReadModel(productId);
            if (readModel != null && readModel.stock() != null && readModel.stock() <= LOW_STOCK_THRESHOLD) {
                log.warn("event=LowStockWarning productId={} currentStock={} threshold={}",
                        productId, readModel.stock(), LOW_STOCK_THRESHOLD);
                if (notificationPort != null) safeCall(
                        () -> notificationPort.notifyLowStock(productId, readModel.sellerId(), readModel.stock()),
                        "notifyLowStock", productId);
            }
        } catch (Exception e) {
            log.error("event=checkLowStockFailed productId={}", productId, e);
        }
    }

    private void safeCall(Runnable action, String actionName, String productId) {
        try {
            action.run();
            log.debug("action={} success productId={}", actionName, productId);
        } catch (Exception e) {
            log.error("action={} failed productId={}", actionName, productId, e);
        }
    }
}
