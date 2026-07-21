package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

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
}
