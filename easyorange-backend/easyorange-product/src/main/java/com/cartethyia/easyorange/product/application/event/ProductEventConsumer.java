package com.cartethyia.easyorange.product.application.event;

import com.cartethyia.easyorange.framework.bloom.BloomFilter;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import com.cartethyia.easyorange.product.adapter.outbound.cache.ProductCacheConstant;
import com.cartethyia.easyorange.product.application.query.ProductQueryService;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.domain.event.*;
import com.cartethyia.easyorange.product.domain.port.ProductCachePort;
import com.cartethyia.easyorange.product.domain.port.ProductNotificationPort;
import com.cartethyia.easyorange.product.domain.port.ProductSearchIndexPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@RabbitListener(
    queues = RabbitMQConfig.QUEUE_PRODUCT_CQRS,
    containerFactory = "domainEventContainerFactory"
)
public class ProductEventConsumer {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final ProductCachePort<?> productCachePort;
    private final ProductQueryService productQueryService;
    private final Optional<ProductNotificationPort> notificationPort;
    private final Optional<ProductSearchIndexPort> searchIndexPort;
    private final BloomFilter bloomFilter;

    @RabbitHandler
    public void onProductCreated(ProductCreatedEvent event) {
        String productId = event.productId();
        log.info("event=ProductCreated productId={} userId={} name={} categoryId={}",
                productId, event.userId(), event.name(), event.categoryId());

        bloomFilter.put(ProductCacheConstant.PRODUCT_BLOOM_KEY, productId);
        evictListCache(event.categoryId());
        notificationPort.ifPresent(port -> safeCall(() -> port.notifyProductCreated(productId, event.userId()), "notifyProductCreated", productId));
        searchIndexPort.ifPresent(port -> safeCall(() -> port.indexProduct(productId), "indexProduct", productId));
    }

    @RabbitHandler
    public void onProductUpdated(ProductUpdatedEvent event) {
        String productId = event.productId();
        log.info("event=ProductUpdated productId={} userId={} categoryId={}", productId, event.userId(), event.categoryId());
        evictListCache(event.categoryId());
        searchIndexPort.ifPresent(port -> safeCall(() -> port.updateProductIndex(productId), "updateProductIndex", productId));
    }

    @RabbitHandler
    public void onProductDeleted(ProductDeletedEvent event) {
        String productId = event.productId();
        log.info("event=ProductDeleted productId={} userId={}", productId, event.userId());
        productCachePort.evictProductCache(productId);
        searchIndexPort.ifPresent(port -> safeCall(() -> port.removeProductIndex(productId), "removeProductIndex", productId));
    }

    @RabbitHandler
    public void onProductMarkedSold(ProductMarkedSoldEvent event) {
        String productId = event.productId();
        log.info("event=ProductMarkedSold productId={} sellerId={}", productId, event.sellerId());
        productCachePort.evictProductCache(productId);
        notificationPort.ifPresent(port -> safeCall(() -> port.notifyProductMarkedSold(productId, event.sellerId()), "notifyProductMarkedSold", productId));
        searchIndexPort.ifPresent(port -> safeCall(() -> port.updateProductIndex(productId), "updateProductIndex", productId));
    }

    @RabbitHandler
    public void onStockDecreased(StockDecreasedEvent event) {
        String productId = event.productId();
        log.info("event=StockDecreased productId={} quantity={}", productId, event.quantity());
        checkLowStock(productId);
    }

    @RabbitHandler
    public void onStockRestored(StockRestoredEvent event) {
        String productId = event.productId();
        log.info("event=StockRestored productId={} quantity={}", productId, event.quantity());
        productCachePort.evictProductCache(productId);
        searchIndexPort.ifPresent(port -> safeCall(() -> port.updateProductIndex(productId), "updateProductIndex", productId));
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
                log.warn("event=LowStockWarning productId={} currentStock={} threshold={}", productId, readModel.stock(), LOW_STOCK_THRESHOLD);
                notificationPort.ifPresent(port -> safeCall(() -> port.notifyLowStock(productId, readModel.sellerId(), readModel.stock()), "notifyLowStock", productId));
            }
        } catch (Exception e) {
            log.error("event=checkLowStockFailed productId={}", productId, e);
        }
    }

    @FunctionalInterface
    private interface SafeAction {
        void run() throws Exception;
    }

    private static void safeCall(SafeAction action, String actionName, String productId) {
        try {
            action.run();
            log.debug("action={} success productId={}", actionName, productId);
        } catch (Exception e) {
            log.error("action={} failed productId={}", actionName, productId, e);
        }
    }
}
