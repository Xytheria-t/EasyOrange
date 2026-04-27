package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;

import java.math.BigDecimal;
import java.util.List;

public class ProductCreatedEvent extends BaseDomainEvent {

    private Long productId;
    private Long userId;
    private Long categoryId;
    private String name;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private Integer conditionLevel;
    private String location;
    private String contactMethod;
    private String description;
    private List<String> imageUrls;

    public ProductCreatedEvent(Long productId, Long userId, Long categoryId, String name,
                              BigDecimal price, BigDecimal originalPrice, Integer stock,
                              Integer conditionLevel, String location, String contactMethod,
                              String description, List<String> imageUrls) {
        super(ProductCreatedEvent.class);
        this.productId = productId;
        this.userId = userId;
        this.categoryId = categoryId;
        this.name = name;
        this.price = price;
        this.originalPrice = originalPrice;
        this.stock = stock;
        this.conditionLevel = conditionLevel;
        this.location = location;
        this.contactMethod = contactMethod;
        this.description = description;
        this.imageUrls = imageUrls;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getProductId() {
        return productId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public Integer getStock() {
        return stock;
    }

    public Integer getConditionLevel() {
        return conditionLevel;
    }

    public String getLocation() {
        return location;
    }

    public String getContactMethod() {
        return contactMethod;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    @Override
    public String eventType() {
        return "ProductCreated";
    }

    public static class Builder {
        private Long productId;
        private Long userId;
        private Long categoryId;
        private String name;
        private BigDecimal price;
        private BigDecimal originalPrice;
        private Integer stock;
        private Integer conditionLevel;
        private String location;
        private String contactMethod;
        private String description;
        private List<String> imageUrls;

        public Builder productId(Long productId) {
            this.productId = productId;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder categoryId(Long categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public Builder originalPrice(BigDecimal originalPrice) {
            this.originalPrice = originalPrice;
            return this;
        }

        public Builder stock(Integer stock) {
            this.stock = stock;
            return this;
        }

        public Builder conditionLevel(Integer conditionLevel) {
            this.conditionLevel = conditionLevel;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder contactMethod(String contactMethod) {
            this.contactMethod = contactMethod;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder imageUrls(List<String> imageUrls) {
            this.imageUrls = imageUrls;
            return this;
        }

        public ProductCreatedEvent build() {
            return new ProductCreatedEvent(productId, userId, categoryId, name, price,
                    originalPrice, stock, conditionLevel, location, contactMethod,
                    description, imageUrls);
        }
    }
}