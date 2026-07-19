package com.cartethyia.easyorange.product.domain.aggregate;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.product.domain.enums.AuditAction;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.event.ProductAuditedEvent;
import com.cartethyia.easyorange.common.event.DomainEvent;
import com.cartethyia.easyorange.product.domain.event.ProductCreatedEvent;
import com.cartethyia.easyorange.product.domain.event.ProductDeletedEvent;
import com.cartethyia.easyorange.product.domain.event.ProductMarkedSoldEvent;
import com.cartethyia.easyorange.product.domain.event.ProductPutOnlineEvent;
import com.cartethyia.easyorange.product.domain.event.ProductSubmittedForReviewEvent;
import com.cartethyia.easyorange.product.domain.event.ProductTakeOfflineEvent;
import com.cartethyia.easyorange.product.domain.event.ProductUpdatedEvent;
import com.cartethyia.easyorange.product.domain.event.StockDecreasedEvent;
import com.cartethyia.easyorange.product.domain.event.StockRestoredEvent;
import com.cartethyia.easyorange.product.domain.exception.InsufficientStockException;
import com.cartethyia.easyorange.product.domain.exception.InvalidProductStatusException;
import com.cartethyia.easyorange.product.domain.valueobject.CategoryId;
import com.cartethyia.easyorange.product.domain.valueobject.ContactMethod;
import com.cartethyia.easyorange.product.domain.valueobject.ImageSet;
import com.cartethyia.easyorange.product.domain.valueobject.ProductDescription;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import com.cartethyia.easyorange.product.domain.valueobject.ProductTitle;
import com.cartethyia.easyorange.product.domain.valueobject.SellerId;
import com.cartethyia.easyorange.product.domain.valueobject.StockQuantity;
import com.cartethyia.easyorange.product.domain.valueobject.TagSet;
import com.cartethyia.easyorange.product.domain.valueobject.TradeLocation;
import com.cartethyia.easyorange.product.domain.valueobject.Version;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder(toBuilder = true)
public class Product {

    private final ProductId id;
    private final SellerId sellerId;
    private final CategoryId categoryId;
    private final ProductTitle title;
    private final Money price;
    private final Money originalPrice;
    private final StockQuantity stock;
    private final Version version;
    private final ProductStatus status;
    @Builder.Default private final int viewCount = 0;
    private final ConditionLevel conditionLevel;
    private final TradeLocation location;
    private final ContactMethod contactMethod;
    private final ProductDescription description;
    private final ImageSet images;
    private final TagSet tags;
    private final String searchText;
    private final LocalDateTime priceUpdateTime;
    private final LocalDateTime createTime;
    private final LocalDateTime updateTime;

    // ==================== Factory Methods ====================

    public static ProductTransition create(
            SellerId sellerId, CategoryId categoryId, ProductTitle title,
            Money price, Money originalPrice, StockQuantity stock,
            ConditionLevel conditionLevel, TradeLocation location, ContactMethod contactMethod,
            ProductDescription description, ImageSet images
    ) {
        BizRequire.notNull(title, "资产名称不能为空");
        BizRequire.notNull(price, "资产价格不能为空");
        BizRequire.requireTrue(price.isGreaterThan(Money.ZERO), "资产价格必须大于0");
        BizRequire.requireTrue(images != null && !images.isEmpty(), "资产图片不能为空");

        var p = Product.builder()
                .sellerId(sellerId).categoryId(categoryId).title(title)
                .price(price).originalPrice(originalPrice)
                .stock(stock != null ? stock : StockQuantity.of(1))
                .version(Version.INITIAL).status(ProductStatus.DRAFT)
                .conditionLevel(conditionLevel).location(location).contactMethod(contactMethod)
                .description(description).tags(TagSet.empty())
                .priceUpdateTime(LocalDateTime.now())
                .createTime(LocalDateTime.now()).updateTime(LocalDateTime.now())
                .build();

        var event = new ProductCreatedEvent(
                null, sellerId.value(),
                categoryId != null ? categoryId.value() : null,
                title.value(), price.value(),
                originalPrice != null ? originalPrice.value() : null,
                p.stock.value(),
                conditionLevel != null ? conditionLevel.getCode() : null,
                location != null ? location.value() : null,
                contactMethod != null ? contactMethod.value() : null,
                description != null ? description.value() : null,
                images != null ? images.imageUrls() : null
        );
        return new ProductTransition(p, event);
    }

    // ==================== Review Workflow Methods ====================

    public ProductTransition submitForReview(String userId) {
        if (!this.sellerId.equals(SellerId.of(userId))) {
            throw new InvalidProductStatusException("只能提交自己的资产审核", id, status);
        }
        if (!status.canTransitionTo(ProductStatus.PENDING_REVIEW)) {
            throw new InvalidProductStatusException("不允许提交审核", id, status);
        }
        var updated = toBuilder()
                .status(ProductStatus.PENDING_REVIEW)
                .updateTime(LocalDateTime.now())
                .build();
        return new ProductTransition(
                updated, new ProductSubmittedForReviewEvent(
                id.value(), sellerId.value(), status.getCode(), ProductStatus.PENDING_REVIEW.getCode()));
    }

    /**
     * 审核通过 — 将资产状态变更为 ONLINE（上架）。
     * <p>
     * 只有处于 PENDING_REVIEW（待审核）状态的资产可以通过审核。
     * 返回 {@link ProductTransition}，包含更新后的聚合根和 {@link ProductAuditedEvent}。
     */
    public ProductTransition approve(String reason) {
        if (!status.canTransitionTo(ProductStatus.ONLINE)) {
            throw new InvalidProductStatusException("不允许审核通过", id, status);
        }
        var updated = toBuilder()
                .status(ProductStatus.ONLINE)
                .updateTime(LocalDateTime.now())
                .build();
        var event = new ProductAuditedEvent(
                id.value(), title.value(), sellerId.value(),
                AuditAction.APPROVED.getCode(), reason, LocalDateTime.now()
        );
        return new ProductTransition(updated, event);
    }

    public ProductTransition reject(String reason) {
        if (!status.canTransitionTo(ProductStatus.REJECTED)) {
            throw new InvalidProductStatusException("不允许审核拒绝", id, status);
        }
        var updated = toBuilder()
                .status(ProductStatus.REJECTED)
                .updateTime(LocalDateTime.now())
                .build();
        var event = new ProductAuditedEvent(
                id.value(), title.value(), sellerId.value(),
                AuditAction.REJECTED.getCode(), reason, LocalDateTime.now()
        );
        return new ProductTransition(updated, event);
    }

    // ==================== State Transition Methods ====================

    public ProductTransition putOnline() {
        if (!status.canTransitionTo(ProductStatus.ONLINE)) {
            throw new InvalidProductStatusException("不允许上架", id, status);
        }
        BizRequire.requireTrue(isComplete(), "资产信息不完整，无法上架");
        BizRequire.requireTrue(hasValidPrice(), "资产价格无效，无法上架");
        BizRequire.requireTrue(hasStock(), "资产库存不足，无法上架");
        var updated = toBuilder()
                .status(ProductStatus.ONLINE)
                .updateTime(LocalDateTime.now())
                .build();
        return new ProductTransition(updated, new ProductPutOnlineEvent(id.value(), sellerId.value()));
    }

    public ProductTransition takeOffline() {
        if (!status.canTransitionTo(ProductStatus.OFFLINE)) {
            throw new InvalidProductStatusException("不允许下架", id, status);
        }
        var updated = toBuilder()
                .status(ProductStatus.OFFLINE)
                .updateTime(LocalDateTime.now())
                .build();
        return new ProductTransition(updated, new ProductTakeOfflineEvent(id.value(), sellerId.value()));
    }

    public ProductTransition markAsSold() {
        if (!status.canTransitionTo(ProductStatus.SOLD)) {
            throw new InvalidProductStatusException("不允许标记已售", id, status);
        }
        var updated = toBuilder()
                .status(ProductStatus.SOLD)
                .updateTime(LocalDateTime.now())
                .build();
        return new ProductTransition(updated, new ProductMarkedSoldEvent(id.value(), sellerId.value()));
    }

    // ==================== Update ====================

    public ProductTransition update(
            CategoryId categoryId, ProductTitle title, Money price, Money originalPrice,
            StockQuantity stock, ConditionLevel conditionLevel, TradeLocation location,
            ContactMethod contactMethod, ProductDescription description, ImageSet images
    ) {
        var builder = toBuilder();
        if (categoryId != null) builder.categoryId(categoryId);
        if (title != null && !title.value().isBlank()) builder.title(title);
        if (price != null) {
            builder.price(price);
            builder.priceUpdateTime(LocalDateTime.now());
        }
        if (originalPrice != null) builder.originalPrice(originalPrice);
        if (stock != null) builder.stock(stock);
        if (conditionLevel != null) builder.conditionLevel(conditionLevel);
        if (location != null) builder.location(location);
        if (contactMethod != null && contactMethod.isNotBlank()) builder.contactMethod(contactMethod);
        if (description != null) builder.description(description);
        if (images != null) builder.images(images);

        var updated = builder.updateTime(LocalDateTime.now()).build();

        var event = new ProductUpdatedEvent(
                id.value(), sellerId.value(),
                updated.categoryId != null ? updated.categoryId.value() : null,
                updated.title.value(), updated.price.value(),
                updated.originalPrice != null ? updated.originalPrice.value() : null,
                updated.stock.value(),
                updated.conditionLevel != null ? updated.conditionLevel.getCode() : null,
                updated.location != null ? updated.location.value() : null,
                updated.contactMethod != null && updated.contactMethod.isNotBlank() ? updated.contactMethod.value() : null,
                updated.description != null ? updated.description.value() : null,
                updated.images != null ? updated.images.imageUrls() : null
        );
        return new ProductTransition(updated, event);
    }

    public Product assignId(String id) {
        if (this.id != null && this.id.value() != null) {
            return this;
        }
        return toBuilder()
                .id(ProductId.of(id))
                .build();
    }

    public ProductTransition delete(String userId) {
        if (!this.sellerId.equals(SellerId.of(userId))) {
            throw new InvalidProductStatusException("无权删除此资产", id, status);
        }
        if (!status.canDelete()) {
            throw new InvalidProductStatusException("不允许删除", id, status);
        }
        var updated = toBuilder()
                .updateTime(LocalDateTime.now())
                .build();
        return new ProductTransition(updated, new ProductDeletedEvent(id.value(), userId));
    }

    // ==================== Stock Operation Methods ====================

    public ProductTransition decrementStock() {
        return decrementStock(1);
    }

    public ProductTransition decrementStock(int quantity) {
        if (!hasStock()) {
            throw new InsufficientStockException("资产库存不足", id, stock);
        }
        var updated = toBuilder()
                .stock(stock.decrease(quantity))
                .updateTime(LocalDateTime.now())
                .build();
        return new ProductTransition(updated, StockDecreasedEvent.of(id.value()));
    }

    public ProductTransition restoreStock() {
        if (status == ProductStatus.SOLD || status == ProductStatus.OFFLINE) {
            throw new InvalidProductStatusException("不允许恢复库存", id, status);
        }
        var updated = toBuilder()
                .stock(stock.increase())
                .updateTime(LocalDateTime.now())
                .build();
        return new ProductTransition(updated, StockRestoredEvent.of(id.value()));
    }

    // ==================== Query Methods ====================

    public boolean isComplete() {
        return title != null && !title.value().isBlank()
                && price != null
                && conditionLevel != null;
    }

    public boolean hasValidPrice() {
        return price != null && price.isGreaterThan(Money.ZERO);
    }

    public boolean hasStock() {
        return stock != null && stock.isAvailable();
    }

    // ==================== Result Record ====================

    public record ProductTransition(Product product, DomainEvent event) {}
}
