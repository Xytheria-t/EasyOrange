package com.cartethyia.easyorange.product.dto.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductVO {

    private Long id;
    private Long sellerId;
    private String username;
    private String userAvatar;
    private Long categoryId;
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

    public ProductVO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserAvatar() {
        return userAvatar;
    }

    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getStatusDesc() {
        return statusDesc;
    }

    public void setStatusDesc(String statusDesc) {
        this.statusDesc = statusDesc;
    }

    public Integer getViews() {
        return views;
    }

    public void setViews(Integer views) {
        this.views = views;
    }

    public Integer getCondition() {
        return condition;
    }

    public void setCondition(Integer condition) {
        this.condition = condition;
    }

    public String getConditionDesc() {
        return conditionDesc;
    }

    public void setConditionDesc(String conditionDesc) {
        this.conditionDesc = conditionDesc;
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

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public String getMainImageUrl() {
        return mainImageUrl;
    }

    public void setMainImageUrl(String mainImageUrl) {
        this.mainImageUrl = mainImageUrl;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public static ProductVOBuilder builder() {
        return new ProductVOBuilder();
    }

    public static class ProductVOBuilder {
        private Long id;
        private Long sellerId;
        private String username;
        private String userAvatar;
        private Long categoryId;
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

        public ProductVOBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ProductVOBuilder sellerId(Long sellerId) {
            this.sellerId = sellerId;
            return this;
        }

        public ProductVOBuilder username(String username) {
            this.username = username;
            return this;
        }

        public ProductVOBuilder userAvatar(String userAvatar) {
            this.userAvatar = userAvatar;
            return this;
        }

        public ProductVOBuilder categoryId(Long categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public ProductVOBuilder categoryName(String categoryName) {
            this.categoryName = categoryName;
            return this;
        }

        public ProductVOBuilder title(String title) {
            this.title = title;
            return this;
        }

        public ProductVOBuilder description(String description) {
            this.description = description;
            return this;
        }

        public ProductVOBuilder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public ProductVOBuilder originalPrice(BigDecimal originalPrice) {
            this.originalPrice = originalPrice;
            return this;
        }

        public ProductVOBuilder stock(Integer stock) {
            this.stock = stock;
            return this;
        }

        public ProductVOBuilder status(Integer status) {
            this.status = status;
            return this;
        }

        public ProductVOBuilder statusDesc(String statusDesc) {
            this.statusDesc = statusDesc;
            return this;
        }

        public ProductVOBuilder views(Integer views) {
            this.views = views;
            return this;
        }

        public ProductVOBuilder condition(Integer condition) {
            this.condition = condition;
            return this;
        }

        public ProductVOBuilder conditionDesc(String conditionDesc) {
            this.conditionDesc = conditionDesc;
            return this;
        }

        public ProductVOBuilder location(String location) {
            this.location = location;
            return this;
        }

        public ProductVOBuilder contactMethod(String contactMethod) {
            this.contactMethod = contactMethod;
            return this;
        }

        public ProductVOBuilder images(List<String> images) {
            this.images = images;
            return this;
        }

        public ProductVOBuilder mainImageUrl(String mainImageUrl) {
            this.mainImageUrl = mainImageUrl;
            return this;
        }

        public ProductVOBuilder createTime(LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        public ProductVOBuilder updateTime(LocalDateTime updateTime) {
            this.updateTime = updateTime;
            return this;
        }

        public ProductVO build() {
            ProductVO vo = new ProductVO();
            vo.id = this.id;
            vo.sellerId = this.sellerId;
            vo.username = this.username;
            vo.userAvatar = this.userAvatar;
            vo.categoryId = this.categoryId;
            vo.categoryName = this.categoryName;
            vo.title = this.title;
            vo.description = this.description;
            vo.price = this.price;
            vo.originalPrice = this.originalPrice;
            vo.stock = this.stock;
            vo.status = this.status;
            vo.statusDesc = this.statusDesc;
            vo.views = this.views;
            vo.condition = this.condition;
            vo.conditionDesc = this.conditionDesc;
            vo.location = this.location;
            vo.contactMethod = this.contactMethod;
            vo.images = this.images;
            vo.mainImageUrl = this.mainImageUrl;
            vo.createTime = this.createTime;
            vo.updateTime = this.updateTime;
            return vo;
        }
    }
}