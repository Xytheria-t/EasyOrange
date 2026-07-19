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
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.domain.event.ProductCreatedEvent;
import com.cartethyia.easyorange.product.domain.event.ProductDeletedEvent;
import com.cartethyia.easyorange.product.domain.event.ProductMarkedSoldEvent;
import com.cartethyia.easyorange.product.domain.event.ProductPutOnlineEvent;
import com.cartethyia.easyorange.product.domain.event.ProductSubmittedForReviewEvent;
import com.cartethyia.easyorange.product.domain.event.ProductTakeOfflineEvent;
import com.cartethyia.easyorange.product.domain.event.ProductUpdatedEvent;
import com.cartethyia.easyorange.product.domain.event.StockDecreasedEvent;
import com.cartethyia.easyorange.product.domain.event.StockRestoredEvent;
import com.cartethyia.easyorange.product.domain.port.ProductCachePort;
import com.cartethyia.easyorange.product.domain.port.ProductNotificationPort;
import com.cartethyia.easyorange.product.domain.port.ProductSearchIndexPort;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 商品 CQRS 投影消费者 — 将商品领域事件投影到读模型（缓存 / 搜索索引 / 布隆过滤器）。
 * <p>
 * 关闭幂等检查：投影操作本身是幂等的（重复写入缓存/索引结果一致）。
 * 子操作（ES 索引、通知端口）失败由 {@link #safeCall} 容忍，不影响主流程。
 */
@Component
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@RabbitListener(queues = RabbitMQConfig.QUEUE_PRODUCT_CQRS, containerFactory = "domainEventContainerFactory")
public class ProductEventConsumer extends AbstractDomainEventConsumer {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final ProductCachePort<?> productCachePort;
    private final ProductQueryService productQueryService;
    private final Optional<ProductNotificationPort> notificationPort;
    private final Optional<ProductSearchIndexPort> searchIndexPort;
    private final BloomFilter bloomFilter;

    public ProductEventConsumer(EventIdempotencyChecker idempotencyChecker,
                                 EventMetricsService metricsService,
                                 ProductCachePort<?> productCachePort,
                                 ProductQueryService productQueryService,
                                 Optional<ProductNotificationPort> notificationPort,
                                 Optional<ProductSearchIndexPort> searchIndexPort,
                                 BloomFilter bloomFilter) {
        super(idempotencyChecker, metricsService, false);
        this.productCachePort = productCachePort;
        this.productQueryService = productQueryService;
        this.notificationPort = notificationPort;
        this.searchIndexPort = searchIndexPort;
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
        bloomFilter.put(ProductCacheConstant.PRODUCT_BLOOM_KEY, productId);
        evictListCache(e.categoryId());
        notificationPort.ifPresent(p -> safeCall(() -> p.notifyProductCreated(productId, e.userId()), "notifyProductCreated", productId));
        searchIndexPort.ifPresent(p -> safeCall(() -> p.indexProduct(productId), "indexProduct", productId));
    }

    private void handleUpdated(ProductUpdatedEvent e) {
        var productId = e.productId();
        evictListCache(e.categoryId());
        searchIndexPort.ifPresent(p -> safeCall(() -> p.updateProductIndex(productId), "updateProductIndex", productId));
    }

    private void handleDeleted(ProductDeletedEvent e) {
        var productId = e.productId();
        productCachePort.evictProductCache(productId);
        searchIndexPort.ifPresent(p -> safeCall(() -> p.removeProductIndex(productId), "removeProductIndex", productId));
    }

    private void handleMarkedSold(ProductMarkedSoldEvent e) {
        var productId = e.productId();
        productCachePort.evictProductCache(productId);
        notificationPort.ifPresent(p -> safeCall(() -> p.notifyProductMarkedSold(productId, e.sellerId()), "notifyProductMarkedSold", productId));
        searchIndexPort.ifPresent(p -> safeCall(() -> p.updateProductIndex(productId), "updateProductIndex", productId));
    }

    private void handleStockDecreased(StockDecreasedEvent e) {
        checkLowStock(e.productId());
    }

    private void handleStockRestored(StockRestoredEvent e) {
        var productId = e.productId();
        productCachePort.evictProductCache(productId);
        searchIndexPort.ifPresent(p -> safeCall(() -> p.updateProductIndex(productId), "updateProductIndex", productId));
    }

    private void handleSubmittedForReview(ProductSubmittedForReviewEvent e) {
        productCachePort.evictProductCache(e.productId());
    }

    private void handlePutOnline(ProductPutOnlineEvent e) {
        var productId = e.productId();
        productCachePort.evictProductCache(productId);
        searchIndexPort.ifPresent(p -> safeCall(() -> p.indexProduct(productId), "indexProduct", productId));
    }

    private void handleTakeOffline(ProductTakeOfflineEvent e) {
        var productId = e.productId();
        productCachePort.evictProductCache(productId);
        searchIndexPort.ifPresent(p -> safeCall(() -> p.removeProductIndex(productId), "removeProductIndex", productId));
    }

    private void evictListCache(String categoryId) {
        if (categoryId != null) {
            productCachePort.evictProductListCache(categoryId);
        }
    }

    private void checkLowStock(String productId) {
        try {
            ProductReadModel readModel = productQueryService.getProductReadModel(productId);
            if (readModel != null && readModel.stock() != null && readModel.stock() <= LOW_STOCK_THRESHOLD) {
                log.warn("event=LowStockWarning productId={} currentStock={} threshold={}",
                        productId, readModel.stock(), LOW_STOCK_THRESHOLD);
                notificationPort.ifPresent(p -> safeCall(
                        () -> p.notifyLowStock(productId, readModel.sellerId(), readModel.stock()),
                        "notifyLowStock", productId));
            }
        } catch (Exception e) {
            log.error("event=checkLowStockFailed productId={}", productId, e);
        }
    }

    @FunctionalInterface
    private interface SafeAction {
        void run() throws Exception;
    }

    private void safeCall(SafeAction action, String actionName, String productId) {
        try {
            action.run();
            log.debug("action={} success productId={}", actionName, productId);
        } catch (Exception e) {
            log.error("action={} failed productId={}", actionName, productId, e);
        }
    }
}
