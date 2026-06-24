package com.cartethyia.easyorange.product.domain.aggregate;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.product.domain.enums.ConsignmentMode;
import com.cartethyia.easyorange.product.domain.event.PriceAdjustedEvent;
import com.cartethyia.easyorange.product.domain.event.ProductAuditedEvent;
import com.cartethyia.easyorange.product.domain.event.ProductCreatedEvent;
import com.cartethyia.easyorange.product.domain.event.ProductDeletedEvent;
import com.cartethyia.easyorange.product.domain.event.ProductMarkedSoldEvent;
import com.cartethyia.easyorange.product.domain.event.ProductSubmittedForReviewEvent;
import com.cartethyia.easyorange.product.domain.event.ProductUpdatedEvent;
import com.cartethyia.easyorange.product.domain.event.StockDecreasedEvent;
import com.cartethyia.easyorange.product.domain.event.StockRestoredEvent;
import com.cartethyia.easyorange.product.domain.exception.InsufficientStockException;
import com.cartethyia.easyorange.product.domain.exception.InvalidProductStatusException;
import com.cartethyia.easyorange.product.domain.valueobject.CategoryId;
import com.cartethyia.easyorange.product.domain.valueobject.ContactMethod;
import com.cartethyia.easyorange.product.domain.valueobject.ImageSet;
import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.product.domain.valueobject.ProductDescription;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import com.cartethyia.easyorange.product.domain.valueobject.ProductTitle;
import com.cartethyia.easyorange.product.domain.valueobject.SellerId;
import com.cartethyia.easyorange.product.domain.valueobject.StockQuantity;
import com.cartethyia.easyorange.product.domain.valueobject.TagSet;
import com.cartethyia.easyorange.product.domain.valueobject.TradeLocation;
import com.cartethyia.easyorange.product.domain.valueobject.Version;
import com.cartethyia.easyorange.product.domain.enums.AuditAction;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Getter
@Builder
public class Product {

    private final ProductId id;
    private final SellerId sellerId;
    private final CategoryId categoryId;
    private final ProductTitle title;
    private final Money price;
    private final Money originalPrice;
    private final Money floorPrice;
    private final ConsignmentMode consignmentMode;
    private final LocalDateTime listedAt;
    private final Integer currentPriceLevel;
    private final StockQuantity stock;
    private final Version version;
    private final ProductStatus status;
    @Builder.Default
    private final Integer viewCount = 0;
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

    public ProductBuilder toBuilder() {
        return Product.builder()
                .id(id)
                .sellerId(sellerId)
                .categoryId(categoryId)
                .title(title)
                .price(price)
                .originalPrice(originalPrice)
                .floorPrice(floorPrice)
                .consignmentMode(consignmentMode)
                .listedAt(listedAt)
                .currentPriceLevel(currentPriceLevel != null ? currentPriceLevel : 0)
                .stock(stock)
                .version(version)
                .status(status)
                .viewCount(viewCount != null ? viewCount : 0)
                .conditionLevel(conditionLevel)
                .location(location)
                .contactMethod(contactMethod)
                .description(description)
                .images(images)
                .tags(tags)
                .searchText(searchText)
                .priceUpdateTime(priceUpdateTime)
                .createTime(createTime)
                .updateTime(updateTime);
    }

    public static ProductCreatedResult create(
            SellerId sellerId,
            CategoryId categoryId,
            ProductTitle title,
            Money price,
            Money originalPrice,
            Money floorPrice,
            ConsignmentMode consignmentMode,
            StockQuantity stock,
            ConditionLevel conditionLevel,
            TradeLocation location,
            ContactMethod contactMethod,
            ProductDescription description,
            ImageSet images
    ) {
        BizRequire.notNull(title, "商品名称不能为空");
        BizRequire.notNull(price, "商品价格不能为空");
        BizRequire.requireTrue(price.isGreaterThan(Money.ZERO), "商品价格必须大于0");
        BizRequire.requireTrue(images != null && !images.isEmpty(), "商品图片不能为空");

        ConsignmentMode mode = consignmentMode != null ? consignmentMode : ConsignmentMode.MANUAL;
        if (mode == ConsignmentMode.AI_MANAGED) {
            BizRequire.notNull(floorPrice, "AI 托管模式必须设置底价");
            BizRequire.requireTrue(floorPrice.isGreaterThan(Money.ZERO), "底价必须大于 0");
        }

        Product p = Product.builder()
                .sellerId(sellerId)
                .categoryId(categoryId)
                .title(title)
                .price(price)
                .originalPrice(originalPrice)
                .floorPrice(floorPrice)
                .consignmentMode(consignmentMode != null ? consignmentMode : ConsignmentMode.MANUAL)
                .listedAt(null)
                .currentPriceLevel(0)
                .stock(stock != null ? stock : StockQuantity.of(1))
                .version(Version.INITIAL)
                .status(ProductStatus.DRAFT)
                .viewCount(0)
                .conditionLevel(conditionLevel)
                .location(location)
                .contactMethod(contactMethod)
                .description(description)
                .tags(TagSet.empty())
                .priceUpdateTime(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        ProductCreatedEvent event = new ProductCreatedEvent(
                null, sellerId.value(), valueOrNull(categoryId, CategoryId::value),
                title.value(), price.value(),
                valueOrNull(originalPrice, Money::value),
                p.stock.value(), valueOrNull(conditionLevel, ConditionLevel::getCode),
                valueOrNull(location, TradeLocation::value),
                valueOrNull(contactMethod, ContactMethod::value),
                valueOrNull(description, ProductDescription::value),
                images.imageUrls()
        );
        return new ProductCreatedResult(p, event);
    }

    public static Product reconstitute(
            ProductId id, SellerId sellerId, CategoryId categoryId,
            ProductTitle title, Money price, Money originalPrice,
            Money floorPrice, ConsignmentMode consignmentMode,
            LocalDateTime listedAt, Integer currentPriceLevel,
            StockQuantity stock, Version version, ProductStatus status,
            Integer viewCount, ConditionLevel conditionLevel,
            TradeLocation location, ContactMethod contactMethod,
            ProductDescription description, ImageSet images,
            TagSet tags, String searchText,
            LocalDateTime priceUpdateTime,
            LocalDateTime createTime, LocalDateTime updateTime
    ) {
        return Product.builder()
                .id(id)
                .sellerId(sellerId)
                .categoryId(categoryId)
                .title(title)
                .price(price)
                .originalPrice(originalPrice)
                .floorPrice(floorPrice)
                .consignmentMode(consignmentMode)
                .listedAt(listedAt)
                .currentPriceLevel(currentPriceLevel != null ? currentPriceLevel : 0)
                .stock(stock)
                .version(version)
                .status(status)
                .viewCount(viewCount != null ? viewCount : 0)
                .conditionLevel(conditionLevel)
                .location(location)
                .contactMethod(contactMethod)
                .description(description)
                .images(images)
                .tags(tags)
                .searchText(searchText)
                .priceUpdateTime(priceUpdateTime)
                .createTime(createTime)
                .updateTime(updateTime)
                .build();
    }

    public ProductUpdatedResult update(
            CategoryId categoryId,
            ProductTitle title,
            Money price,
            Money originalPrice,
            StockQuantity stock,
            ConditionLevel conditionLevel,
            ConsignmentMode consignmentMode,
            Money floorPrice,
            TradeLocation location,
            ContactMethod contactMethod,
            ProductDescription description,
            ImageSet images
    ) {
        ProductBuilder builder = toBuilder();
        updateIfPresent(categoryId, builder::categoryId);
        updateIfPresent(title, builder::title, t -> !t.value().isBlank());
        updateIfPresent(price, v -> {
            builder.price(v);
            builder.priceUpdateTime(LocalDateTime.now());
        });
        updateIfPresent(originalPrice, builder::originalPrice);
        updateIfPresent(stock, builder::stock);
        updateIfPresent(conditionLevel, builder::conditionLevel);
        if (consignmentMode != null) {
            builder.consignmentMode(consignmentMode);
        }
        if (floorPrice != null) {
            builder.floorPrice(floorPrice);
        }
        updateIfPresent(location, builder::location);
        updateIfPresent(contactMethod, builder::contactMethod, ContactMethod::isNotBlank);
        updateIfPresent(description, builder::description);
        updateIfPresent(images, builder::images);

        Product updated = builder.updateTime(LocalDateTime.now()).build();
        validateConsignmentMode(updated);

        ProductUpdatedEvent event = new ProductUpdatedEvent(
                id.value(), sellerId.value(),
                valueOrNull(updated.categoryId, CategoryId::value),
                updated.title.value(), updated.price.value(),
                valueOrNull(updated.originalPrice, Money::value),
                updated.stock.value(),
                valueOrNull(updated.conditionLevel, ConditionLevel::getCode),
                valueOrNull(updated.location, TradeLocation::value),
                valueOrNull(updated.contactMethod, ContactMethod::value),
                valueOrNull(updated.description, ProductDescription::value),
                valueOrNull(updated.images, ImageSet::imageUrls)
        );
        return new ProductUpdatedResult(updated, event);
    }

    private static void validateConsignmentMode(Product p) {
        if (p.consignmentMode == ConsignmentMode.AI_MANAGED) {
            BizRequire.notNull(p.floorPrice, "AI 托管模式必须设置底价");
            BizRequire.requireTrue(p.floorPrice.isGreaterThan(Money.ZERO), "底价必须大于 0");
        }
    }

    public Product putOnline() {
        if (status.isOnline()) {
            throw new InvalidProductStatusException("商品已上架，无需重复操作", id, status);
        }
        if (status.isSold()) {
            throw new InvalidProductStatusException("已售出商品不能上架", id, status);
        }
        BizRequire.requireTrue(isComplete(), "商品信息不完整，无法上架");
        BizRequire.requireTrue(hasValidPrice(), "商品价格无效，无法上架");
        BizRequire.requireTrue(hasStock(), "商品库存不足，无法上架");
        return toBuilder()
                .status(ProductStatus.ONLINE)
                .listedAt(LocalDateTime.now())
                .currentPriceLevel(0)
                .updateTime(LocalDateTime.now())
                .build();
    }

    public Product takeOffline() {
        if (!status.isOnline()) {
            throw new InvalidProductStatusException("只有上架中的商品才能下架", id, status);
        }
        return toBuilder()
                .status(ProductStatus.OFFLINE)
                .updateTime(LocalDateTime.now())
                .build();
    }

    public ProductMarkedSoldResult markAsSold() {
        if (!status.isOnline()) {
            throw new InvalidProductStatusException("只有上架中的商品才能标记为已售", id, status);
        }
        Product updated = toBuilder()
                .status(ProductStatus.SOLD)
                .updateTime(LocalDateTime.now())
                .build();
        return new ProductMarkedSoldResult(updated, new ProductMarkedSoldEvent(id.value(), sellerId.value()));
    }

    public ProductDeletedResult delete(Long userId) {
        if (!this.sellerId.equals(SellerId.of(userId))) {
            throw new InvalidProductStatusException("无权删除此商品", id, status);
        }
        if (!status.canDelete()) {
            throw new InvalidProductStatusException("已售商品不能删除", id, status);
        }
        Product updated = toBuilder()
                .updateTime(LocalDateTime.now())
                .build();
        return new ProductDeletedResult(updated, new ProductDeletedEvent(id.value(), userId));
    }

    public ProductSubmittedForReviewResult submitForReview(Long userId) {
        if (!this.sellerId.equals(SellerId.of(userId))) {
            throw new InvalidProductStatusException("只能提交自己的商品审核", id, status);
        }
        if (!status.canSubmitForReview()) {
            throw new InvalidProductStatusException("当前状态不支持提交审核", id, status);
        }
        Product updated = toBuilder()
                .status(ProductStatus.PENDING_REVIEW)
                .updateTime(LocalDateTime.now())
                .build();
        return new ProductSubmittedForReviewResult(
                updated, new ProductSubmittedForReviewEvent(
                id.value(), sellerId.value(), status.getCode(), ProductStatus.PENDING_REVIEW.getCode()));
    }

    /**
     * 审核通过 — 将商品状态变更为 ONLINE（上架）。
     * <p>
     * 只有处于 PENDING_REVIEW（待审核）状态的商品可以通过审核。
     * 返回 {@link ProductApprovedResult}，包含更新后的聚合根和 {@link ProductAuditedEvent}。
     */
    public ProductApprovedResult approve(String reason) {
        if (!status.canApprove()) {
            throw new InvalidProductStatusException("当前状态不允许审核通过", id, status);
        }
        Product updated = toBuilder()
                .status(ProductStatus.ONLINE)
                .listedAt(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        ProductAuditedEvent event = new ProductAuditedEvent(
                id.value(), title.value(), sellerId.value(),
                AuditAction.APPROVED.getCode(), reason, LocalDateTime.now()
        );
        return new ProductApprovedResult(updated, event);
    }

    /**
     * 审核拒绝 — 将商品状态变更为 REJECTED（已驳回）。
     * <p>
     * 只有处于 PENDING_REVIEW（待审核）状态的商品可以被拒绝。
     * 返回 {@link ProductRejectedResult}，包含更新后的聚合根和 {@link ProductAuditedEvent}。
     */
    public ProductRejectedResult reject(String reason) {
        if (!status.canReject()) {
            throw new InvalidProductStatusException("当前状态不允许审核拒绝", id, status);
        }
        Product updated = toBuilder()
                .status(ProductStatus.REJECTED)
                .updateTime(LocalDateTime.now())
                .build();
        ProductAuditedEvent event = new ProductAuditedEvent(
                id.value(), title.value(), sellerId.value(),
                AuditAction.REJECTED.getCode(), reason, LocalDateTime.now()
        );
        return new ProductRejectedResult(updated, event);
    }

    public Product incrementViewCount() {
        return toBuilder()
                .viewCount(viewCount != null ? viewCount + 1 : 1)
                .updateTime(LocalDateTime.now())
                .build();
    }

    public Product addViewCount(int count) {
        if (count <= 0) {
            return this;
        }
        return toBuilder()
                .viewCount(viewCount != null ? viewCount + count : count)
                .updateTime(LocalDateTime.now())
                .build();
    }

    public StockDecreasedResult decrementStock() {
        return decrementStock(1);
    }

    public StockDecreasedResult decrementStock(int quantity) {
        if (!hasStock()) {
            throw new InsufficientStockException("商品库存不足", id, stock);
        }
        Product updated = toBuilder()
                .stock(stock.decrease(quantity))
                .updateTime(LocalDateTime.now())
                .build();
        return new StockDecreasedResult(updated, new StockDecreasedEvent(id.value()));
    }

    public StockRestoredResult restoreStock() {
        if (status.isSold() || status.isOffline()) {
            throw new InvalidProductStatusException("已售或下架商品不能恢复库存", id, status);
        }
        Product updated = toBuilder()
                .stock(stock.increase())
                .updateTime(LocalDateTime.now())
                .build();
        return new StockRestoredResult(updated, new StockRestoredEvent(id.value()));
    }

    public Product assignId(Long id) {
        if (this.id != null && this.id.value() != null) {
            return this;
        }
        return toBuilder()
                .id(ProductId.of(id))
                .build();
    }

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

    // ==================== AI Consignment Methods ====================

    /**
     * 阶梯降价 — 将商品价格调整到指定阶梯等级。
     * <p>
     * 只有上架中且 AI 托管的商品可以自动调价。
     *
     * @param targetLevel 目标阶梯等级 (0-3)
     * @return PriceAdjustedResult 包含更新后的聚合根和 PriceAdjustedEvent
     * @throws InvalidProductStatusException 如果商品未上架或非 AI 托管模式
     */
    public PriceAdjustedResult adjustPrice(int targetLevel) {
        if (!status.isOnline()) {
            throw new InvalidProductStatusException("只有上架商品可以调价", id, status);
        }
        if (consignmentMode != ConsignmentMode.AI_MANAGED) {
            throw new InvalidProductStatusException("只有AI托管商品支持自动调价", id, status);
        }
        Money newPrice = calculatePriceForLevel(targetLevel);
        Product updated = toBuilder()
                .price(newPrice)
                .currentPriceLevel(targetLevel)
                .priceUpdateTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        return new PriceAdjustedResult(updated,
                new PriceAdjustedEvent(id.value(), sellerId.value(), newPrice.value(), targetLevel));
    }

    /**
     * 根据阶梯等级计算新价格。
     */
    private Money calculatePriceForLevel(int level) {
        return switch (level) {
            case 0 -> price;
            case 1 -> Money.of(price.value().multiply(new BigDecimal("0.95")));
            case 2 -> Money.of(price.value().multiply(new BigDecimal("0.90")));
            case 3 -> floorPrice;
            default -> price;
        };
    }

    /**
     * 计算当前应该处于的降价阶梯等级。
     * <p>
     * 根据上架时间计算：
     * <ul>
     *   <li>0-3 天 → 0 (原价)</li>
     *   <li>4-5 天 → 1 (降5%)</li>
     *   <li>6 天   → 2 (降10%)</li>
     *   <li>7 天+  → 3 (底价)</li>
     * </ul>
     *
     * @return 期望的阶梯等级 (0-3)
     */
    public int calculateExpectedPriceLevel() {
        if (listedAt == null || consignmentMode != ConsignmentMode.AI_MANAGED) {
            return 0;
        }
        long daysOnline = Duration.between(listedAt, LocalDateTime.now()).toDays();
        if (daysOnline >= 7) return 3;
        if (daysOnline >= 6) return 2;
        if (daysOnline >= 4) return 1;
        return 0;
    }

    private static <T, R> R valueOrNull(T obj, Function<T, R> extractor) {
        return obj != null ? extractor.apply(obj) : null;
    }

    private static <T> void updateIfPresent(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    private static <T> void updateIfPresent(T value, Consumer<T> setter, Predicate<T> condition) {
        if (value != null && condition.test(value)) {
            setter.accept(value);
        }
    }

    // ==================== Result Records ====================

    public record ProductCreatedResult(Product product, ProductCreatedEvent event) {}
    public record ProductUpdatedResult(Product product, ProductUpdatedEvent event) {}
    public record ProductMarkedSoldResult(Product product, ProductMarkedSoldEvent event) {}
    public record ProductDeletedResult(Product product, ProductDeletedEvent event) {}
    public record ProductSubmittedForReviewResult(Product product, ProductSubmittedForReviewEvent event) {}
    public record ProductApprovedResult(Product product, ProductAuditedEvent event) {}
    public record ProductRejectedResult(Product product, ProductAuditedEvent event) {}
    public record StockDecreasedResult(Product product, StockDecreasedEvent event) {}
    public record StockRestoredResult(Product product, StockRestoredEvent event) {}
    public record PriceAdjustedResult(Product product, PriceAdjustedEvent event) {}
}