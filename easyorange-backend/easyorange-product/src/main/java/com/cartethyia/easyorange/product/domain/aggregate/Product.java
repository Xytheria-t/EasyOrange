package com.cartethyia.easyorange.product.domain.aggregate;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.common.event.Transition;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.product.domain.enums.AuditAction;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.event.ProductAuditedEvent;
import com.cartethyia.easyorange.product.domain.event.ProductCreatedEvent;
import com.cartethyia.easyorange.product.domain.event.ProductDeletedEvent;
import com.cartethyia.easyorange.product.domain.event.ProductEvent;
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
import com.cartethyia.easyorange.product.domain.valueobject.ProductTitle;
import com.cartethyia.easyorange.product.domain.valueobject.SellerId;
import com.cartethyia.easyorange.product.domain.valueobject.StockQuantity;
import com.cartethyia.easyorange.product.domain.valueobject.TagSet;
import com.cartethyia.easyorange.product.domain.valueobject.TradeLocation;
import com.cartethyia.easyorange.product.domain.valueobject.Version;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.Builder;
import lombok.Getter;

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

    @Builder.Default
    private final int viewCount = 0;

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

    public static Transition<Product, ProductCreatedEvent> create(ProductCreateSpec spec) {
        BizRequire.notNull(spec.title(), "资产名称不能为空");
        BizRequire.notNull(spec.price(), "资产价格不能为空");
        BizRequire.requireTrue(spec.price().isGreaterThan(Money.ZERO), "资产价格必须大于0");
        BizRequire.requireTrue(spec.images() != null && !spec.images().isEmpty(), "资产图片不能为空");

        Product p = Product.builder()
                .sellerId(spec.sellerId())
                .categoryId(spec.categoryId())
                .title(spec.title())
                .price(spec.price())
                .originalPrice(spec.originalPrice())
                .stock(spec.stock() != null ? spec.stock() : StockQuantity.of(1))
                .version(Version.INITIAL)
                .status(ProductStatus.DRAFT)
                .conditionLevel(spec.conditionLevel())
                .location(spec.location())
                .contactMethod(spec.contactMethod())
                .description(spec.description())
                .tags(TagSet.empty())
                .priceUpdateTime(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        return new Transition<>(p, new ProductCreatedEvent(ProductEvent.Data.from(p)));
    }

    // ==================== State Transitions ====================
    // Lifecycle: DRAFT → PENDING_REVIEW → ONLINE ⇄ OFFLINE → SOLD（终端），REJECTED 可循环提交审核。
    // 所有转换的合法性统一由 transitionTo(target) 通过 ProductStatus 状态机表裁决。

    public Transition<Product, ProductSubmittedForReviewEvent> submitForReview(String userId) {
        if (!this.sellerId.equals(SellerId.of(userId))) {
            throw new InvalidProductStatusException("只能提交自己的资产审核", id, status);
        }
        return new Transition<>(
                transitionTo(ProductStatus.PENDING_REVIEW),
                new ProductSubmittedForReviewEvent(
                        id.value(), userId, sellerId.value(), status, ProductStatus.PENDING_REVIEW));
    }

    public Transition<Product, ProductAuditedEvent> approve(String reason) {
        var updated = transitionTo(ProductStatus.ONLINE);
        validateOnline();
        return new Transition<>(
                updated,
                new ProductAuditedEvent(
                        id.value(),
                        title.value(),
                        sellerId.value(),
                        AuditAction.APPROVED.getCode(),
                        reason,
                        LocalDateTime.now()));
    }

    public Transition<Product, ProductAuditedEvent> reject(String reason) {
        return new Transition<>(
                transitionTo(ProductStatus.REJECTED),
                new ProductAuditedEvent(
                        id.value(),
                        title.value(),
                        sellerId.value(),
                        AuditAction.REJECTED.getCode(),
                        reason,
                        LocalDateTime.now()));
    }

    public Transition<Product, ProductPutOnlineEvent> putOnline() {
        var updated = transitionTo(ProductStatus.ONLINE);
        validateOnline();
        return new Transition<>(updated, new ProductPutOnlineEvent(id.value(), sellerId.value()));
    }

    public Transition<Product, ProductTakeOfflineEvent> takeOffline() {
        return new Transition<>(
                transitionTo(ProductStatus.OFFLINE), new ProductTakeOfflineEvent(id.value(), sellerId.value()));
    }

    public Transition<Product, ProductTakeOfflineEvent> takeOffline(String userId) {
        if (!this.sellerId.equals(SellerId.of(userId))) {
            throw new InvalidProductStatusException("只能下架自己的资产", id, status);
        }
        return takeOffline();
    }

    public Optional<Transition<Product, ProductMarkedSoldEvent>> markAsSold() {
        if (status == ProductStatus.SOLD) {
            return Optional.empty(); // 幂等：订单完成链路重复触发时忽略
        }
        return Optional.of(new Transition<>(
                transitionTo(ProductStatus.SOLD), new ProductMarkedSoldEvent(id.value(), sellerId.value())));
    }

    /** 状态机守卫：目标状态非法时抛出 {@link InvalidProductStatusException}，否则返回新状态。 */
    private Product transitionTo(ProductStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidProductStatusException(
                    "不允许从 " + status.getDesc() + " 转换到 " + target.getDesc(), id, status);
        }
        return toBuilder().status(target).updateTime(LocalDateTime.now()).build();
    }

    /** 上架不变量：无论审核通过（approve）还是管理员直接上架（putOnline），进入 ONLINE 前必须通过同一组校验。 */
    private void validateOnline() {
        BizRequire.requireTrue(isComplete(), "资产信息不完整，无法上架");
        BizRequire.requireTrue(hasValidPrice(), "资产价格无效，无法上架");
        BizRequire.requireTrue(hasStock(), "资产库存不足，无法上架");
    }

    // ==================== Mutations ====================

    public Transition<Product, ProductUpdatedEvent> update(ProductUpdateSpec spec) {
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
        if (spec.contactMethod() != null && spec.contactMethod().isNotBlank())
            builder.contactMethod(spec.contactMethod());
        if (spec.description() != null) builder.description(spec.description());
        if (spec.images() != null) builder.images(spec.images());
        var updated = builder.updateTime(LocalDateTime.now()).build();
        return new Transition<>(updated, new ProductUpdatedEvent(ProductEvent.Data.from(updated)));
    }

    public Transition<Product, ProductDeletedEvent> delete(String userId) {
        if (!this.sellerId.equals(SellerId.of(userId))) {
            throw new InvalidProductStatusException("无权删除此资产", id, status);
        }
        if (!status.canDelete()) {
            throw new InvalidProductStatusException("不允许删除", id, status);
        }
        return new Transition<>(
                toBuilder().updateTime(LocalDateTime.now()).build(), new ProductDeletedEvent(id.value(), userId));
    }

    // ==================== Utility ====================

    public Product assignId(String id) {
        if (this.id != null && this.id.value() != null) {
            return this;
        }
        return toBuilder().id(ProductId.of(id)).build();
    }

    // ==================== Stock Operations ====================

    public Transition<Product, StockDecreasedEvent> decrementStock() {
        return decrementStock(1);
    }

    public Transition<Product, StockDecreasedEvent> decrementStock(int quantity) {
        if (!hasStock()) {
            throw new InsufficientStockException("资产库存不足", id, stock);
        }
        return new Transition<>(
                toBuilder()
                        .stock(stock.decrease(quantity))
                        .updateTime(LocalDateTime.now())
                        .build(),
                StockDecreasedEvent.of(id.value(), quantity));
    }

    public Transition<Product, StockRestoredEvent> restoreStock() {
        return restoreStock(1);
    }

    public Transition<Product, StockRestoredEvent> restoreStock(int quantity) {
        if (!status.canRestoreStock()) {
            throw new InvalidProductStatusException("不允许恢复库存", id, status);
        }
        return new Transition<>(
                toBuilder()
                        .stock(stock.increase(quantity))
                        .updateTime(LocalDateTime.now())
                        .build(),
                StockRestoredEvent.of(id.value(), quantity));
    }

    // ==================== Query / Predicate ====================

    public boolean isComplete() {
        return title != null && !title.value().isBlank() && price != null && conditionLevel != null;
    }

    public boolean hasValidPrice() {
        return price != null && price.isGreaterThan(Money.ZERO);
    }

    public boolean hasStock() {
        return stock != null && stock.isAvailable();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Product{id=" + id + ", title=" + title + ", status=" + status + ", price=" + price + "}";
    }
}
