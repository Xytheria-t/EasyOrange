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
import com.cartethyia.easyorange.product.domain.valueobject.ConditionLevelVO;
import com.cartethyia.easyorange.product.domain.valueobject.ContactMethod;
import com.cartethyia.easyorange.product.domain.valueobject.ImageSet;
import com.cartethyia.easyorange.product.domain.valueobject.Money;
import com.cartethyia.easyorange.product.domain.valueobject.ProductDescription;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import com.cartethyia.easyorange.product.domain.valueobject.ProductStatusVO;
import com.cartethyia.easyorange.product.domain.valueobject.ProductTitle;
import com.cartethyia.easyorange.product.domain.valueobject.SellerId;
import com.cartethyia.easyorange.product.domain.valueobject.StockQuantity;
import com.cartethyia.easyorange.product.domain.valueobject.TagSet;
import com.cartethyia.easyorange.product.domain.valueobject.TradeLocation;
import com.cartethyia.easyorange.product.domain.valueobject.Version;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Product {

    private ProductId id;
    private final SellerId sellerId;
    private CategoryId categoryId;
    private ProductTitle title;
    private Money price;
    private Money originalPrice;
    private StockQuantity stock;
    private Version version;
    private ProductStatusVO status;
    private Integer viewCount;
    private ConditionLevelVO conditionLevel;
    private TradeLocation location;
    private ContactMethod contactMethod;
    private ProductDescription description;
    private ImageSet images;
    private TagSet tags;
    private String searchText;
    private LocalDateTime priceUpdateTime;
    private final LocalDateTime createTime;
    private LocalDateTime updateTime;
    private final List<BaseDomainEvent> domainEvents;

    private Product(SellerId sellerId) {
        this.sellerId = sellerId;
        this.domainEvents = new ArrayList<>();
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    public static Product create(
            SellerId sellerId,
            CategoryId categoryId,
            ProductTitle title,
            Money price,
            Money originalPrice,
            StockQuantity stock,
            ConditionLevelVO conditionLevel,
            TradeLocation location,
            ContactMethod contactMethod,
            ProductDescription description,
            ImageSet images
    ) {
        BizRequire.notNull(title, "商品名称不能为空");
        BizRequire.notNull(price, "商品价格不能为空");
        BizRequire.requireTrue(price.isGreaterThan(BigDecimal.ZERO), "商品价格必须大于0");
        BizRequire.requireTrue(images != null && !images.isEmpty(), "商品图片不能为空");

        Product p = new Product(sellerId);
        p.id = ProductId.of(null);
        p.categoryId = categoryId;
        p.title = title;
        p.price = price;
        p.originalPrice = originalPrice;
        p.stock = stock != null ? stock : StockQuantity.of(1);
        p.version = Version.initial();
        p.status = ProductStatusVO.of(ProductStatus.DRAFT);
        p.viewCount = 0;
        p.conditionLevel = conditionLevel;
        p.location = location;
        p.contactMethod = contactMethod;
        p.description = description;
        p.images = images != null ? images : ImageSet.empty();
        p.tags = TagSet.empty();
        p.searchText = null;
        p.priceUpdateTime = LocalDateTime.now();

        p.addDomainEvent(new ProductCreatedEvent(
                null, sellerId.value(), categoryId != null ? categoryId.value() : null,
                title.value(), price.value(),
                originalPrice != null ? originalPrice.value() : null,
                p.stock.value(), conditionLevel != null ? conditionLevel.code() : null,
                location != null ? location.value() : null,
                contactMethod != null ? contactMethod.value() : null,
                description != null ? description.value() : null,
                images != null ? images.imageUrls() : List.of()
        ));
        return p;
    }

    public static Product reconstitute(ProductId id, SellerId sellerId, CategoryId categoryId,
                                        ProductTitle title, Money price, Money originalPrice,
                                        StockQuantity stock, Version version, ProductStatusVO status,
                                        Integer viewCount, ConditionLevelVO conditionLevel,
                                        TradeLocation location, ContactMethod contactMethod,
                                        ProductDescription description, ImageSet images,
                                        TagSet tags, String searchText,
                                        LocalDateTime priceUpdateTime,
                                        LocalDateTime createTime, LocalDateTime updateTime) {
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
        p.updateTime = updateTime;
        return p;
    }

    public Product update(
            CategoryId categoryId,
            ProductTitle title,
            Money price,
            Money originalPrice,
            StockQuantity stock,
            ConditionLevelVO conditionLevel,
            TradeLocation location,
            ContactMethod contactMethod,
            ProductDescription description,
            ImageSet images
    ) {
        if (categoryId != null) {
            this.categoryId = categoryId;
        }
        if (title != null && !title.value().isBlank()) {
            this.title = title;
        }
        if (price != null) {
            this.price = price;
            this.priceUpdateTime = LocalDateTime.now();
        }
        if (originalPrice != null) {
            this.originalPrice = originalPrice;
        }
        if (stock != null) {
            this.stock = stock;
        }
        if (conditionLevel != null) {
            this.conditionLevel = conditionLevel;
        }
        if (location != null) {
            this.location = location;
        }
        if (contactMethod != null) {
            this.contactMethod = contactMethod;
        }
        if (description != null) {
            this.description = description;
        }
        if (images != null) {
            this.images = images;
        }
        this.updateTime = LocalDateTime.now();
        addDomainEvent(new ProductUpdatedEvent(
                id.value(), sellerId.value(),
                this.categoryId != null ? this.categoryId.value() : null,
                this.title.value(), this.price.value(),
                this.originalPrice != null ? this.originalPrice.value() : null,
                this.stock.value(),
                this.conditionLevel != null ? this.conditionLevel.code() : null,
                this.location != null ? this.location.value() : null,
                this.contactMethod != null ? this.contactMethod.value() : null,
                this.description != null ? this.description.value() : null,
                this.images != null ? this.images.imageUrls() : List.of()
        ));
        return this;
    }

    public void putOnline() {
        if (status.isOnLine()) {
            throw new InvalidProductStatusException("商品已上架，无需重复操作", id, status);
        }
        if (status.isSold()) {
            throw new InvalidProductStatusException("已售出商品不能上架", id, status);
        }
        BizRequire.requireTrue(isComplete(), "商品信息不完整，无法上架");
        BizRequire.requireTrue(hasValidPrice(), "商品价格无效，无法上架");
        this.status = ProductStatusVO.of(ProductStatus.ONLINE);
        this.updateTime = LocalDateTime.now();
    }

    public void takeOffline() {
        if (!status.isOnLine()) {
            throw new InvalidProductStatusException("只有上架中的商品才能下架", id, status);
        }
        this.status = ProductStatusVO.of(ProductStatus.OFFLINE);
        this.updateTime = LocalDateTime.now();
    }

    public void markAsSold() {
        if (!status.isOnLine()) {
            throw new InvalidProductStatusException("只有上架中的商品才能标记为已售", id, status);
        }
        this.status = ProductStatusVO.of(ProductStatus.SOLD);
        this.updateTime = LocalDateTime.now();
        addDomainEvent(new ProductMarkedSoldEvent(id.value()));
    }

    public void delete(Long userId) {
        if (!this.sellerId.equals(SellerId.of(userId))) {
            throw new IllegalStateException("无权删除此商品");
        }
        this.updateTime = LocalDateTime.now();
        addDomainEvent(new ProductDeletedEvent(id.value(), userId));
    }

    public void incrementViewCount() {
        this.viewCount = this.viewCount != null ? this.viewCount + 1 : 1;
        this.updateTime = LocalDateTime.now();
    }

    public void decrementStock() {
        if (!stock.isAvailable()) {
            throw new InsufficientStockException("商品库存不足", id, stock);
        }
        this.stock = stock.decrease();
        this.updateTime = LocalDateTime.now();
        addDomainEvent(new StockDecreasedEvent(id.value()));
    }

    public void restoreStock() {
        this.stock = stock.increase();
        this.updateTime = LocalDateTime.now();
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
        return price != null && price.isGreaterThan(BigDecimal.ZERO);
    }

    public boolean hasStock() {
        return stock != null && stock.isAvailable();
    }

    public ProductId getId() { return id; }
    public SellerId getSellerId() { return sellerId; }
    public CategoryId getCategoryId() { return categoryId; }
    public ProductTitle getTitle() { return title; }
    public Money getPrice() { return price; }
    public Money getOriginalPrice() { return originalPrice; }
    public StockQuantity getStock() { return stock; }
    public Version getVersion() { return version; }
    public ProductStatusVO getStatus() { return status; }
    public Integer getViewCount() { return viewCount; }
    public ConditionLevelVO getConditionLevel() { return conditionLevel; }
    public TradeLocation getLocation() { return location; }
    public ContactMethod getContactMethod() { return contactMethod; }
    public ProductDescription getDescription() { return description; }
    public ImageSet getImages() { return images; }
    public TagSet getTags() { return tags; }
    public String getSearchText() { return searchText; }
    public LocalDateTime getPriceUpdateTime() { return priceUpdateTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
}
