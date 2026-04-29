package com.cartethyia.easyorange.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.cartethyia.easyorange.common.entity.BaseDO;

import java.math.BigDecimal;

@TableName("product")
public class Product extends BaseDO {

    private Long userId;
    private Long categoryId;
    private String name;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    @Version
    private Integer version;
    private Integer status;
    private Integer viewCount;
    private Integer conditionLevel;
    private String location;
    private String contactMethod;
    private String tags;
    private String searchText;
    private java.time.LocalDateTime priceUpdateTime;

    public Product() {
    }

    public Product(Long userId, Long categoryId, String name, BigDecimal price, BigDecimal originalPrice,
                   Integer stock, Integer version, Integer status, Integer viewCount,
                   Integer conditionLevel, String location, String contactMethod,
                   String tags, String searchText, java.time.LocalDateTime priceUpdateTime) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.name = name;
        this.price = price;
        this.originalPrice = originalPrice;
        this.stock = stock;
        this.version = version;
        this.status = status;
        this.viewCount = viewCount;
        this.conditionLevel = conditionLevel;
        this.location = location;
        this.contactMethod = contactMethod;
        this.tags = tags;
        this.searchText = searchText;
        this.priceUpdateTime = priceUpdateTime;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
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

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public java.time.LocalDateTime getPriceUpdateTime() {
        return priceUpdateTime;
    }

    public void setPriceUpdateTime(java.time.LocalDateTime priceUpdateTime) {
        this.priceUpdateTime = priceUpdateTime;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long userId;
        private Long categoryId;
        private String name;
        private BigDecimal price;
        private BigDecimal originalPrice;
        private Integer stock;
        private Integer version;
        private Integer status;
        private Integer viewCount;
        private Integer conditionLevel;
        private String location;
        private String contactMethod;
        private String tags;
        private String searchText;
        private java.time.LocalDateTime priceUpdateTime;

        public Builder id(Long id) {
            this.id = id;
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

        public Builder version(Integer version) {
            this.version = version;
            return this;
        }

        public Builder status(Integer status) {
            this.status = status;
            return this;
        }

        public Builder viewCount(Integer viewCount) {
            this.viewCount = viewCount;
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

        public Builder tags(String tags) {
            this.tags = tags;
            return this;
        }

        public Builder searchText(String searchText) {
            this.searchText = searchText;
            return this;
        }

        public Builder priceUpdateTime(java.time.LocalDateTime priceUpdateTime) {
            this.priceUpdateTime = priceUpdateTime;
            return this;
        }

        public Product build() {
            Product product = new Product(userId, categoryId, name, price, originalPrice, stock,
                    version, status, viewCount, conditionLevel, location, contactMethod, tags, searchText, priceUpdateTime);
            if (id != null) {
                product.setId(id);
            }
            return product;
        }
    }
}