package com.cartethyia.easyorange.product.domain.aggregate;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.product.application.command.CreateProductCommand;
import com.cartethyia.easyorange.product.application.command.UpdateProductCommand;
import com.cartethyia.easyorange.product.domain.event.ProductCreatedEvent;
import com.cartethyia.easyorange.product.domain.event.ProductDeletedEvent;
import com.cartethyia.easyorange.product.domain.event.ProductMarkedSoldEvent;
import com.cartethyia.easyorange.product.domain.event.ProductUpdatedEvent;
import com.cartethyia.easyorange.product.domain.event.StockDecreasedEvent;
import com.cartethyia.easyorange.product.domain.event.StockRestoredEvent;
import com.cartethyia.easyorange.product.entity.Product;
import com.cartethyia.easyorange.product.entity.ProductDetail;
import com.cartethyia.easyorange.product.entity.ProductImage;
import com.cartethyia.easyorange.product.enums.ProductStatus;

import java.util.ArrayList;
import java.util.List;

public class ProductAggregate {

    private Product product;
    private ProductDetail detail;
    private List<ProductImage> images;
    private List<BaseDomainEvent> domainEvents;

    public ProductAggregate() {
        this.images = new ArrayList<>();
        this.domainEvents = new ArrayList<>();
    }

    private ProductAggregate(Builder builder) {
        this.product = builder.product;
        this.detail = builder.detail;
        this.images = builder.images != null ? builder.images : new ArrayList<>();
        this.domainEvents = new ArrayList<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ProductAggregate create(CreateProductCommand command, Long userId) {
        BizRequire.notBlank(command.getName(), "商品名称不能为空");
        BizRequire.notNull(command.getPrice(), "商品价格不能为空");
        BizRequire.requireTrue(command.getPrice().compareTo(java.math.BigDecimal.ZERO) > 0, "商品价格必须大于0");

        Product product = Product.builder()
                .userId(userId)
                .categoryId(command.getCategoryId())
                .name(command.getName())
                .price(command.getPrice())
                .originalPrice(command.getOriginalPrice())
                .stock(command.getStock() != null ? command.getStock() : 1)
                .status(ProductStatus.ONLINE.getCode())
                .viewCount(0)
                .conditionLevel(command.getConditionLevel())
                .location(command.getLocation())
                .contactMethod(command.getContactMethod())
                .build();

        ProductDetail detail = null;
        if (command.getDescription() != null && !command.getDescription().isBlank()) {
            detail = ProductDetail.builder()
                    .description(command.getDescription())
                    .build();
        }

        List<ProductImage> images = new ArrayList<>();
        BizRequire.notEmpty(command.getImageUrls(), "商品图片不能为空");
        for (int i = 0; i < command.getImageUrls().size(); i++) {
            images.add(ProductImage.builder()
                    .imageUrl(command.getImageUrls().get(i))
                    .sortOrder(i)
                    .isMain(i == 0 ? 1 : 0)
                    .build());
        }

        ProductAggregate aggregate = ProductAggregate.builder()
                .product(product)
                .detail(detail)
                .images(images)
                .build();

        ProductCreatedEvent event = ProductCreatedEvent.builder()
                .productId(product.getId())
                .userId(userId)
                .categoryId(command.getCategoryId())
                .name(command.getName())
                .price(command.getPrice())
                .originalPrice(command.getOriginalPrice())
                .stock(product.getStock())
                .conditionLevel(command.getConditionLevel())
                .location(command.getLocation())
                .contactMethod(command.getContactMethod())
                .description(command.getDescription())
                .imageUrls(command.getImageUrls())
                .build();
        aggregate.addDomainEvent(event);

        return aggregate;
    }

    public static ProductAggregate load(Product product, ProductDetail detail, List<ProductImage> images) {
        return ProductAggregate.builder()
                .product(product)
                .detail(detail)
                .images(images != null ? images : new ArrayList<>())
                .build();
    }

    public void update(UpdateProductCommand command) {
        BizRequire.notNull(product, "商品不存在");

        if (command.getCategoryId() != null) {
            product.setCategoryId(command.getCategoryId());
        }
        if (command.getName() != null && !command.getName().isBlank()) {
            product.setName(command.getName());
        }
        if (command.getPrice() != null) {
            product.setPrice(command.getPrice());
        }
        if (command.getOriginalPrice() != null) {
            product.setOriginalPrice(command.getOriginalPrice());
        }
        if (command.getStock() != null) {
            product.setStock(command.getStock());
        }
        if (command.getConditionLevel() != null) {
            product.setConditionLevel(command.getConditionLevel());
        }
        if (command.getLocation() != null) {
            product.setLocation(command.getLocation());
        }
        if (command.getContactMethod() != null) {
            product.setContactMethod(command.getContactMethod());
        }

        if (command.getDescription() != null) {
            if (detail != null) {
                detail.setDescription(command.getDescription());
            } else {
                detail = ProductDetail.builder()
                        .productId(product.getId())
                        .description(command.getDescription())
                        .build();
            }
        }

        if (command.getImageUrls() != null) {
            images.clear();
            for (int i = 0; i < command.getImageUrls().size(); i++) {
                images.add(ProductImage.builder()
                        .productId(product.getId())
                        .imageUrl(command.getImageUrls().get(i))
                        .sortOrder(i)
                        .isMain(i == 0 ? 1 : 0)
                        .build());
            }
        }

        ProductUpdatedEvent event = ProductUpdatedEvent.builder()
                .productId(product.getId())
                .userId(product.getUserId())
                .categoryId(product.getCategoryId())
                .name(product.getName())
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .stock(product.getStock())
                .conditionLevel(product.getConditionLevel())
                .location(product.getLocation())
                .contactMethod(product.getContactMethod())
                .description(detail != null ? detail.getDescription() : null)
                .imageUrls(command.getImageUrls())
                .build();
        addDomainEvent(event);
    }

    public void markAsSold() {
        BizRequire.notNull(product, "商品不存在");
        BizRequire.requireTrue(ProductStatus.ONLINE.getCode().equals(product.getStatus()),
                "只有上架中的商品才能标记为已售");
        product.setStatus(ProductStatus.SOLD.getCode());
        addDomainEvent(new ProductMarkedSoldEvent(product.getId()));
    }

    public void delete(Long userId) {
        BizRequire.notNull(product, "商品不存在");
        addDomainEvent(new ProductDeletedEvent(product.getId(), userId));
    }

    public StockDecreasedEvent decrementStock() {
        BizRequire.notNull(product, "商品不存在");
        BizRequire.requireTrue(product.getStock() != null && product.getStock() > 0, "商品库存不足");
        product.setStock(product.getStock() - 1);
        return new StockDecreasedEvent(product.getId());
    }

    public StockRestoredEvent restoreStock() {
        BizRequire.notNull(product, "商品不存在");
        product.setStock(product.getStock() + 1);
        return new StockRestoredEvent(product.getId());
    }

    public void addDomainEvent(BaseDomainEvent event) {
        if (domainEvents == null) {
            domainEvents = new ArrayList<>();
        }
        domainEvents.add(event);
    }

    public List<BaseDomainEvent> releaseEvents() {
        List<BaseDomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }

    public boolean hasDetail() {
        return detail != null && detail.getDescription() != null && !detail.getDescription().isBlank();
    }

    public boolean hasImages() {
        return images != null && !images.isEmpty();
    }

    public Long getId() {
        return product != null ? product.getId() : null;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public ProductDetail getDetail() {
        return detail;
    }

    public void setDetail(ProductDetail detail) {
        this.detail = detail;
    }

    public List<ProductImage> getImages() {
        return images;
    }

    public void setImages(List<ProductImage> images) {
        this.images = images;
    }

    public List<BaseDomainEvent> getDomainEvents() {
        return domainEvents;
    }

    public void setDomainEvents(List<BaseDomainEvent> domainEvents) {
        this.domainEvents = domainEvents;
    }

    public static class Builder {
        private Product product;
        private ProductDetail detail;
        private List<ProductImage> images;
        private List<BaseDomainEvent> domainEvents;

        public Builder product(Product product) {
            this.product = product;
            return this;
        }

        public Builder detail(ProductDetail detail) {
            this.detail = detail;
            return this;
        }

        public Builder images(List<ProductImage> images) {
            this.images = images;
            return this;
        }

        public Builder domainEvents(List<BaseDomainEvent> domainEvents) {
            this.domainEvents = domainEvents;
            return this;
        }

        public ProductAggregate build() {
            return new ProductAggregate(this);
        }
    }
}