package com.cartethyia.easyorange.product.domain.aggregate;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.product.domain.enums.AuditAction;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.event.ProductAuditedEvent;
import com.cartethyia.easyorange.common.event.DomainEvent;
import com.cartethyia.easyorange.product.domain.event.ProductCreatedEvent;
import com.cartethyia.easyorange.product.domain.event.ProductEvent;
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

    // ==================== Static Factory ====================

    public static ProductTransition create(ProductCreateSpec spec) {
        BizRequire.notNull(spec.title(), "资产名称不能为空");
        BizRequire.notNull(spec.price(), "资产价格不能为空");
        BizRequire.requireTrue(spec.price().isGreaterThan(Money.ZERO), "资产价格必须大于0");
        BizRequire.requireTrue(spec.images() != null && !spec.images().isEmpty(), "资产图片不能为空");

        var p = Product.builder()
                .sellerId(spec.sellerId()).categoryId(spec.categoryId()).title(spec.title())
                .price(spec.price()).originalPrice(spec.originalPrice())
                .stock(spec.stock() != null ? spec.stock() : StockQuantity.of(1))
                .version(Version.INITIAL).status(ProductStatus.DRAFT)
                .conditionLevel(spec.conditionLevel()).location(spec.location()).contactMethod(spec.contactMethod())
                .description(spec.description()).tags(TagSet.empty())
                .priceUpdateTime(LocalDateTime.now())
                .createTime(LocalDateTime.now()).updateTime(LocalDateTime.now())
                .build();

        var event = new ProductCreatedEvent(new ProductEvent.Data(
                null, spec.sellerId().value(),
                spec.categoryId() != null ? spec.categoryId().value() : null,
                spec.title().value(), spec.price().value(),
                spec.originalPrice() != null ? spec.originalPrice().value() : null,
                p.stock.value(),
                spec.conditionLevel() != null ? spec.conditionLevel().getCode() : null,
                spec.location() != null ? spec.location().value() : null,
                spec.contactMethod() != null ? spec.contactMethod().value() : null,
                spec.description() != null ? spec.description().value() : null,
                spec.images() != null ? spec.images().imageUrls() : null
        ));
        return new ProductTransition(p, event);
    }

    // ==================== State Transitions ====================
    // Ordered by state machine lifecycle: DRAFT → PENDING_REVIEW → ONLINE → OFFLINE → SOLD

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
                id.value(), userId, sellerId.value(), status, ProductStatus.PENDING_REVIEW));
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

    // ==================== Mutations ====================

    public ProductTransition update(ProductUpdateSpec spec) {
        var builder = toBuilder();
        if (spec.categoryId() != null) builder.categoryId(spec.categoryId());
        if (spec.title() != null && !spec.title().value().isBlank()) builder.title(spec.title());
        if (spec.price() != null) {
            builder.price(spec.price());
            builder.priceUpdateTime(LocalDateTime.now());
        }
        if (spec.originalPrice() != null) builder.originalPrice(spec.originalPrice());
        if (spec.stock() != null) builder.stock(spec.stock());
        if (spec.conditionLevel() != null) builder.conditionLevel(spec.conditionLevel());
        if (spec.location() != null) builder.location(spec.location());
        if (spec.contactMethod() != null && spec.contactMethod().isNotBlank()) builder.contactMethod(spec.contactMethod());
        if (spec.description() != null) builder.description(spec.description());
        if (spec.images() != null) builder.images(spec.images());

        var updated = builder.updateTime(LocalDateTime.now()).build();

        var event = new ProductUpdatedEvent(new ProductEvent.Data(
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
        ));
        return new ProductTransition(updated, event);
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

    // ==================== Utility ====================

    public Product assignId(String id) {
        if (this.id != null && this.id.value() != null) {
            return this;
        }
        return toBuilder()
                .id(ProductId.of(id))
                .build();
    }

    // ==================== Stock Operations ====================

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

    // ==================== Query / Predicate ====================

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

    // ==================== Inner Types ====================

    public record ProductTransition(Product product, DomainEvent event) {}
}
