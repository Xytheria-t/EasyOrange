package com.cartethyia.easyorange.product.domain.aggregate;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.product.domain.event.ProductCreatedEvent;
import com.cartethyia.easyorange.product.domain.event.ProductDeletedEvent;
import com.cartethyia.easyorange.product.domain.event.ProductMarkedSoldEvent;
import com.cartethyia.easyorange.product.domain.event.ProductUpdatedEvent;
import com.cartethyia.easyorange.product.domain.event.StockDecreasedEvent;
import com.cartethyia.easyorange.product.domain.event.StockRestoredEvent;
import com.cartethyia.easyorange.product.domain.exception.InsufficientStockException;
import com.cartethyia.easyorange.product.domain.exception.InvalidProductStatusException;
import com.cartethyia.easyorange.product.domain.valueobject.CategoryId;
import com.cartethyia.easyorange.product.domain.valueobject.ContactMethod;
import com.cartethyia.easyorange.product.domain.valueobject.ImageSet;
import com.cartethyia.easyorange.product.domain.valueobject.Money;
import com.cartethyia.easyorange.product.domain.valueobject.ProductDescription;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import com.cartethyia.easyorange.product.domain.valueobject.ProductTitle;
import com.cartethyia.easyorange.product.domain.valueobject.SellerId;
import com.cartethyia.easyorange.product.domain.valueobject.StockQuantity;
import com.cartethyia.easyorange.product.domain.valueobject.TagSet;
import com.cartethyia.easyorange.product.domain.valueobject.TradeLocation;
import com.cartethyia.easyorange.product.domain.valueobject.Version;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Getter
public class Product {

    private ProductId id;
    private final SellerId sellerId;
    private CategoryId categoryId;
    private ProductTitle title;
    private Money price;
    private Money originalPrice;
    private StockQuantity stock;
    private Version version;
    private ProductStatus status;
    private Integer viewCount;
    private ConditionLevel conditionLevel;
    private TradeLocation location;
    private ContactMethod contactMethod;
    private ProductDescription description;
    private ImageSet images;
    private TagSet tags;
    private String searchText;
    private LocalDateTime priceUpdateTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private final List<BaseDomainEvent> domainEvents = new ArrayList<>();

    private Product(SellerId sellerId) {
        this.sellerId = sellerId;
        this.createTime = LocalDateTime.now();
        touch();
    }

    public static Product create(
            SellerId sellerId,
            CategoryId categoryId,
            ProductTitle title,
            Money price,
            Money originalPrice,
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

        Product p = new Product(sellerId);
        p.id = ProductId.of(null);
        p.categoryId = categoryId;
        p.title = title;
        p.price = price;
        p.originalPrice = originalPrice;
        p.stock = stock != null ? stock : StockQuantity.of(1);
        p.version = Version.INITIAL;
        p.status = ProductStatus.DRAFT;
        p.viewCount = 0;
        p.conditionLevel = conditionLevel;
        p.location = location;
        p.contactMethod = contactMethod;
        p.description = description;
        p.images = images;
        p.tags = TagSet.empty();
        p.searchText = null;
        p.priceUpdateTime = LocalDateTime.now();

        p.addDomainEvent(new ProductCreatedEvent(
                null, sellerId.value(), valueOrNull(categoryId, CategoryId::value),
                title.value(), price.value(),
                valueOrNull(originalPrice, Money::value),
                p.stock.value(), valueOrNull(conditionLevel, ConditionLevel::getCode),
                valueOrNull(location, TradeLocation::value),
                valueOrNull(contactMethod, ContactMethod::value),
                valueOrNull(description, ProductDescription::value),
                images.imageUrls()
        ));
        return p;
    }

    public static Product reconstitute(
            ProductId id, SellerId sellerId, CategoryId categoryId,
            ProductTitle title, Money price, Money originalPrice,
            StockQuantity stock, Version version, ProductStatus status,
            Integer viewCount, ConditionLevel conditionLevel,
            TradeLocation location, ContactMethod contactMethod,
            ProductDescription description, ImageSet images,
            TagSet tags, String searchText,
            LocalDateTime priceUpdateTime,
            LocalDateTime createTime, LocalDateTime updateTime
    ) {
        Product p = new Product(sellerId);
        p.id = id;
        p.categoryId = categoryId;
        p.title = title;
        p.price = price;
        p.originalPrice = originalPrice;
        p.stock = stock;
        p.version = version;
        p.status = status;
        p.viewCount = viewCount;
        p.conditionLevel = conditionLevel;
        p.location = location;
        p.contactMethod = contactMethod;
        p.description = description;
        p.images = images;
        p.tags = tags;
        p.searchText = searchText;
        p.priceUpdateTime = priceUpdateTime;
        p.createTime = createTime;
        p.updateTime = updateTime;
        return p;
    }

    public void update(
            CategoryId categoryId,
            ProductTitle title,
            Money price,
            Money originalPrice,
            StockQuantity stock,
            ConditionLevel conditionLevel,
            TradeLocation location,
            ContactMethod contactMethod,
            ProductDescription description,
            ImageSet images
    ) {
        updateIfPresent(categoryId, v -> this.categoryId = v);
        updateIfPresent(title, v -> this.title = v, t -> !t.value().isBlank());
        updateIfPresent(price, v -> {
            this.price = v;
            this.priceUpdateTime = LocalDateTime.now();
        });
        updateIfPresent(originalPrice, v -> this.originalPrice = v);
        updateIfPresent(stock, v -> this.stock = v);
        updateIfPresent(conditionLevel, v -> this.conditionLevel = v);
        updateIfPresent(location, v -> this.location = v);
        updateIfPresent(contactMethod, v -> this.contactMethod = v, ContactMethod::isNotBlank);
        updateIfPresent(description, v -> this.description = v);
        updateIfPresent(images, v -> this.images = v);

        touch();
        addDomainEvent(new ProductUpdatedEvent(
                id.value(), sellerId.value(),
                valueOrNull(this.categoryId, CategoryId::value),
                this.title.value(), this.price.value(),
                valueOrNull(this.originalPrice, Money::value),
                this.stock.value(),
                valueOrNull(this.conditionLevel, ConditionLevel::getCode),
                valueOrNull(this.location, TradeLocation::value),
                valueOrNull(this.contactMethod, ContactMethod::value),
                valueOrNull(this.description, ProductDescription::value),
                this.images.imageUrls()
        ));
    }

    public void putOnline() {
        if (status.isOnline()) {
            throw new InvalidProductStatusException("商品已上架，无需重复操作", id, status);
        }
        if (status.isSold()) {
            throw new InvalidProductStatusException("已售出商品不能上架", id, status);
        }
        BizRequire.requireTrue(isComplete(), "商品信息不完整，无法上架");
        BizRequire.requireTrue(hasValidPrice(), "商品价格无效，无法上架");
        BizRequire.requireTrue(hasStock(), "商品库存不足，无法上架");
        this.status = ProductStatus.ONLINE;
        touch();
    }

    public void takeOffline() {
        if (!status.isOnline()) {
            throw new InvalidProductStatusException("只有上架中的商品才能下架", id, status);
        }
        this.status = ProductStatus.OFFLINE;
        touch();
    }

    public void markAsSold() {
        if (!status.isOnline()) {
            throw new InvalidProductStatusException("只有上架中的商品才能标记为已售", id, status);
        }
        this.status = ProductStatus.SOLD;
        touch();
        addDomainEvent(new ProductMarkedSoldEvent(id.value()));
    }

    public void delete(Long userId) {
        if (!this.sellerId.equals(SellerId.of(userId))) {
            throw new InvalidProductStatusException("无权删除此商品", id, status);
        }
        if (!status.canDelete()) {
            throw new InvalidProductStatusException("已售商品不能删除", id, status);
        }
        touch();
        addDomainEvent(new ProductDeletedEvent(id.value(), userId));
    }

    public void incrementViewCount() {
        this.viewCount = this.viewCount != null ? this.viewCount + 1 : 1;
        touch();
    }

    public void addViewCount(int count) {
        if (count > 0) {
            this.viewCount = this.viewCount != null ? this.viewCount + count : count;
            touch();
        }
    }

    public void decrementStock() {
        if (!hasStock()) {
            throw new InsufficientStockException("商品库存不足", id, stock);
        }
        this.stock = stock.decrease();
        touch();
        addDomainEvent(new StockDecreasedEvent(id.value()));
    }

    public void restoreStock() {
        if (status.isSold() || status.isOffline()) {
            throw new InvalidProductStatusException("已售或下架商品不能恢复库存", id, status);
        }
        this.stock = stock.increase();
        touch();
        addDomainEvent(new StockRestoredEvent(id.value()));
    }

    public void assignId(Long id) {
        if (this.id == null || this.id.value() == null) {
            this.id = ProductId.of(id);
        }
    }

    public void addDomainEvent(BaseDomainEvent event) {
        this.domainEvents.add(event);
    }

    public List<BaseDomainEvent> releaseEvents() {
        List<BaseDomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
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

    private void touch() {
        this.updateTime = LocalDateTime.now();
    }

    private static <T, R> R valueOrNull(T obj, Function<T, R> extractor) {
        return obj != null ? extractor.apply(obj) : null;
    }

    private <T> void updateIfPresent(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    private <T> void updateIfPresent(T value, Consumer<T> setter, Predicate<T> condition) {
        if (value != null && condition.test(value)) {
            setter.accept(value);
        }
    }
}
