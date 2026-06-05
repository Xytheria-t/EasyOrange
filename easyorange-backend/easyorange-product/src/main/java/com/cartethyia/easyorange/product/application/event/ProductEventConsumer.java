package com.cartethyia.easyorange.product.application.event;

import com.cartethyia.easyorange.framework.bloom.RedisBitmapBloomFilter;
import com.cartethyia.easyorange.product.adapter.outbound.cache.ProductCacheConstant;
import com.cartethyia.easyorange.product.application.query.ProductQueryService;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.domain.event.*;
import com.cartethyia.easyorange.product.domain.port.ProductCachePort;
import com.cartethyia.easyorange.product.domain.port.ProductNotificationPort;
import com.cartethyia.easyorange.product.domain.port.ProductSearchIndexPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ProductEventConsumer {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final ProductCachePort<?> productCachePort;
    private final ProductQueryService productQueryService;
    private final Optional<ProductNotificationPort> notificationPort;
    private final Optional<ProductSearchIndexPort> searchIndexPort;
    private final RedisBitmapBloomFilter bloomFilter;

    @RabbitListener(
        queues = "eo.order.events",
        containerFactory = "domainEventContainerFactory"
    )
    public void onProductCreated(ProductCreatedEvent event) {
        Long productId = event.getProductId();
        log.info("event=ProductCreated productId={} userId={} name={} categoryId={}",
                productId, event.getUserId(), event.getName(), event.getCategoryId());

        bloomFilter.put(ProductCacheConstant.PRODUCT_BLOOM_KEY, productId.toString());
        evictListCache(event.getCategoryId());
        notificationPort.ifPresent(port -> safeCall(() -> port.notifyProductCreated(productId, event.getUserId()), "notifyProductCreated", productId));
        searchIndexPort.ifPresent(port -> safeCall(() -> port.indexProduct(productId), "indexProduct", productId));
    }

    @RabbitListener(
        queues = "eo.order.events",
        containerFactory = "domainEventContainerFactory"
    )
    public void onProductUpdated(ProductUpdatedEvent event) {
        Long productId = event.getProductId();
        log.info("event=ProductUpdated productId={} userId={} categoryId={}", productId, event.getUserId(), event.getCategoryId());
        evictListCache(event.getCategoryId());
        searchIndexPort.ifPresent(port -> safeCall(() -> port.updateProductIndex(productId), "updateProductIndex", productId));
    }

    @RabbitListener(
        queues = "eo.order.events",
        containerFactory = "domainEventContainerFactory"
    )
    public void onProductDeleted(ProductDeletedEvent event) {
        Long productId = event.getProductId();
        log.info("event=ProductDeleted productId={} userId={}", productId, event.getUserId());
        productCachePort.evictProductCache(productId);
        searchIndexPort.ifPresent(port -> safeCall(() -> port.removeProductIndex(productId), "removeProductIndex", productId));
    }

    @RabbitListener(
        queues = "eo.order.events",
        containerFactory = "domainEventContainerFactory"
    )
    public void onProductMarkedSold(ProductMarkedSoldEvent event) {
        Long productId = event.getProductId();
        log.info("event=ProductMarkedSold productId={} sellerId={}", productId, event.getSellerId());
        productCachePort.evictProductCache(productId);
        notificationPort.ifPresent(port -> safeCall(() -> port.notifyProductMarkedSold(productId, event.getSellerId()), "notifyProductMarkedSold", productId));
        searchIndexPort.ifPresent(port -> safeCall(() -> port.updateProductIndex(productId), "updateProductIndex", productId));
    }

    @RabbitListener(
        queues = "eo.order.events",
        containerFactory = "domainEventContainerFactory"
    )
    public void onStockDecreased(StockDecreasedEvent event) {
        Long productId = event.getProductId();
        log.info("event=StockDecreased productId={} quantity={}", productId, event.getQuantity());
        checkLowStock(productId);
    }

    @RabbitListener(
        queues = "eo.order.events",
        containerFactory = "domainEventContainerFactory"
    )
    public void onStockRestored(StockRestoredEvent event) {
        Long productId = event.getProductId();
        log.info("event=StockRestored productId={} quantity={}", productId, event.getQuantity());
        productCachePort.evictProductCache(productId);
        searchIndexPort.ifPresent(port -> safeCall(() -> port.updateProductIndex(productId), "updateProductIndex", productId));
    }

    private void evictListCache(Long categoryId) {
        if (categoryId != null) {
            productCachePort.evictProductListCache(categoryId);
        }
    }

    private void checkLowStock(Long productId) {
        try {
            ProductReadModel readModel = productQueryService.getProductReadModel(productId);
            if (readModel != null && readModel.stock() != null && readModel.stock() <= LOW_STOCK_THRESHOLD) {
                log.warn("event=LowStockWarning productId={} currentStock={} threshold={}", productId, readModel.stock(), LOW_STOCK_THRESHOLD);
                notificationPort.ifPresent(port -> safeCall(() -> port.notifyLowStock(productId, readModel.stock()), "notifyLowStock", productId));
            }
        } catch (Exception e) {
            log.error("event=checkLowStockFailed productId={}", productId, e);
        }
    }

    @FunctionalInterface
    private interface SafeAction {
        void run() throws Exception;
    }

    private static void safeCall(SafeAction action, String actionName, Long productId) {
        try {
            action.run();
            log.debug("action={} success productId={}", actionName, productId);
        } catch (Exception e) {
            log.error("action={} failed productId={}", actionName, productId, e);
        }
    }
}
