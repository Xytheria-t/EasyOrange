package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

import java.math.BigDecimal;
import java.util.List;

/**
 * 产品领域事件密封接口 — 消除所有子类重复的 {@link #aggregateId()} 模板。
 *
 * <p>所有产品领域事件只需 {@code implements ProductEvent} 并提供 {@code String productId()} 组件即可。
 */
public sealed interface ProductEvent extends DomainEvent
    permits ProductCreatedEvent, ProductUpdatedEvent, ProductDeletedEvent,
            ProductSubmittedForReviewEvent, ProductPutOnlineEvent,
            ProductTakeOfflineEvent, ProductMarkedSoldEvent,
            ProductAuditedEvent,
            StockDecreasedEvent, StockRestoredEvent,
            ReportProcessedEvent {

    /**
     * 聚合根标识 — 所有产品事件共享 productId 作为聚合根主键。
     */
    String productId();

    @Override
    default String aggregateId() {
        return productId();
    }

    /**
     * 商品创建/更新事件的共享数据载体。
     * <p>
     * {@link ProductCreatedEvent} 和 {@link ProductUpdatedEvent} 携带完全相同的字段集，
     * 通过此类型作为单一数据源，确保两个事件在字段演进时不会不同步。
     */
    record Data(String productId, String userId, String categoryId, String name,
                BigDecimal price, BigDecimal originalPrice, Integer stock,
                Integer conditionLevel, String location, String contactMethod,
                String description, List<String> imageUrls) {

        public Data {
            imageUrls = imageUrls != null ? List.copyOf(imageUrls) : List.of();
        }
    }
}
