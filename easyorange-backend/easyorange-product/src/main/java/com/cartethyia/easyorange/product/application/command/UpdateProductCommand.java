package com.cartethyia.easyorange.product.application.command;

import java.math.BigDecimal;
import java.util.List;

public class UpdateProductCommand {

    private Long id;
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

    public UpdateProductCommand() {
    }

    public UpdateProductCommand(Long id, Long categoryId, String name, BigDecimal price, BigDecimal originalPrice,
                                Integer stock, Integer conditionLevel, String location,
                                String contactMethod, String description, List<String> imageUrls) {
        this.id = id;
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getConditionLevel() {
        return conditionLevel;
    }

    public void setConditionLevel(Integer conditionLevel) {
        this.conditionLevel = conditionLevel;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getContactMethod() {
        return contactMethod;
    }

    public void setContactMethod(String contactMethod) {
        this.contactMethod = contactMethod;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
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

        public Builder id(Long id) {
            this.id = id;
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

        public UpdateProductCommand build() {
            return new UpdateProductCommand(id, categoryId, name, price, originalPrice, stock,
                    conditionLevel, location, contactMethod, description, imageUrls);
        }
    }
}