package com.cartethyia.easyorange.favorite.domain.valueobject;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductDetailInfo(
    String id,
    String sellerId,
    String username,
    String userAvatar,
    String categoryId,
    String categoryName,
    String title,
    String description,
    BigDecimal price,
    BigDecimal originalPrice,
    Integer stock,
    Integer status,
    String statusDesc,
    Integer views,
    Integer condition,
    String conditionDesc,
    String location,
    String contactMethod,
    List<String> images,
    String mainImageUrl,
    LocalDateTime createTime,
    LocalDateTime updateTime
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String sellerId;
        private String username;
        private String userAvatar;
        private String categoryId;
        private String categoryName;
        private String title;
        private String description;
        private BigDecimal price;
        private BigDecimal originalPrice;
        private Integer stock;
        private Integer status;
        private String statusDesc;
        private Integer views;
        private Integer condition;
        private String conditionDesc;
        private String location;
        private String contactMethod;
        private List<String> images;
        private String mainImageUrl;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;

        public Builder id(String id) { this.id = id; return this; }
        public Builder sellerId(String sellerId) { this.sellerId = sellerId; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder userAvatar(String userAvatar) { this.userAvatar = userAvatar; return this; }
        public Builder categoryId(String categoryId) { this.categoryId = categoryId; return this; }
        public Builder categoryName(String categoryName) { this.categoryName = categoryName; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder price(BigDecimal price) { this.price = price; return this; }
        public Builder originalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; return this; }
        public Builder stock(Integer stock) { this.stock = stock; return this; }
        public Builder status(Integer status) { this.status = status; return this; }
        public Builder statusDesc(String statusDesc) { this.statusDesc = statusDesc; return this; }
        public Builder views(Integer views) { this.views = views; return this; }
        public Builder condition(Integer condition) { this.condition = condition; return this; }
        public Builder conditionDesc(String conditionDesc) { this.conditionDesc = conditionDesc; return this; }
        public Builder location(String location) { this.location = location; return this; }
        public Builder contactMethod(String contactMethod) { this.contactMethod = contactMethod; return this; }
        public Builder images(List<String> images) { this.images = images; return this; }
        public Builder mainImageUrl(String mainImageUrl) { this.mainImageUrl = mainImageUrl; return this; }
        public Builder createTime(LocalDateTime createTime) { this.createTime = createTime; return this; }
        public Builder updateTime(LocalDateTime updateTime) { this.updateTime = updateTime; return this; }

        public ProductDetailInfo build() {
            return new ProductDetailInfo(
                    id, sellerId, username, userAvatar, categoryId, categoryName,
                    title, description, price, originalPrice, stock, status,
                    statusDesc, views, condition, conditionDesc, location,
                    contactMethod, images, mainImageUrl, createTime, updateTime
            );
        }
    }
}
