package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.aggregate.ProductCreateSpec;
import com.cartethyia.easyorange.product.domain.valueobject.StockQuantity;

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
                String conditionLevel, String location, String contactMethod,
                String description, List<String> imageUrls) {

        public Data {
            imageUrls = imageUrls != null ? List.copyOf(imageUrls) : List.of();
        }

        /** 创建场景 — 从 ProductCreateSpec 与已解析库存构造事件数据载体。 */
        public static Data fromCreate(ProductCreateSpec spec, StockQuantity stock) {
            return new Data(
                    null, spec.sellerId().value(),
                    spec.categoryId() != null ? spec.categoryId().value() : null,
                    spec.title().value(), spec.price().value(),
                    spec.originalPrice() != null ? spec.originalPrice().value() : null,
                    stock.value(),
                    spec.conditionLevel() != null ? spec.conditionLevel().getCode() : null,
                    spec.location() != null ? spec.location().value() : null,
                    spec.contactMethod() != null ? spec.contactMethod().value() : null,
                    spec.description() != null ? spec.description().value() : null,
                    spec.images() != null ? spec.images().imageUrls() : null
            );
        }

        /** 更新场景 — 从更新后的 Product 聚合根构造事件数据载体。 */
        public static Data fromUpdated(Product updated) {
            return new Data(
                    updated.getId().value(), updated.getSellerId().value(),
                    updated.getCategoryId() != null ? updated.getCategoryId().value() : null,
                    updated.getTitle().value(), updated.getPrice().value(),
                    updated.getOriginalPrice() != null ? updated.getOriginalPrice().value() : null,
                    updated.getStock().value(),
                    updated.getConditionLevel() != null ? updated.getConditionLevel().getCode() : null,
                    updated.getLocation() != null ? updated.getLocation().value() : null,
                    updated.getContactMethod() != null && updated.getContactMethod().isNotBlank()
                            ? updated.getContactMethod().value() : null,
                    updated.getDescription() != null ? updated.getDescription().value() : null,
                    updated.getImages() != null ? updated.getImages().imageUrls() : null
            );
        }
    }
}
