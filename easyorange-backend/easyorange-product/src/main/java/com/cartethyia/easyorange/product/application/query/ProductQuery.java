package com.cartethyia.easyorange.product.application.query;

import java.math.BigDecimal;

public class ProductQuery {

    private Long id;
    private Long userId;
    private Long categoryId;
    private Integer status;
    private String keyword;
    private Integer conditionLevel;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String sortBy;
    private String sortOrder;
    private Integer pageNum;
    private Integer pageSize;

    public ProductQuery() {
    }

    public ProductQuery(Long id, Long userId, Long categoryId, Integer status, String keyword,
                       Integer conditionLevel, BigDecimal minPrice, BigDecimal maxPrice,
                       String sortBy, String sortOrder, Integer pageNum, Integer pageSize) {
        this.id = id;
        this.userId = userId;
        this.categoryId = categoryId;
        this.status = status;
        this.keyword = keyword;
        this.conditionLevel = conditionLevel;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.sortBy = sortBy;
        this.sortOrder = sortOrder;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getConditionLevel() {
        return conditionLevel;
    }

    public void setConditionLevel(Integer conditionLevel) {
        this.conditionLevel = conditionLevel;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long userId;
        private Long categoryId;
        private Integer status;
        private String keyword;
        private Integer conditionLevel;
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private String sortBy;
        private String sortOrder;
        private Integer pageNum;
        private Integer pageSize;

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

        public Builder status(Integer status) {
            this.status = status;
            return this;
        }

        public Builder keyword(String keyword) {
            this.keyword = keyword;
            return this;
        }

        public Builder conditionLevel(Integer conditionLevel) {
            this.conditionLevel = conditionLevel;
            return this;
        }

        public Builder minPrice(BigDecimal minPrice) {
            this.minPrice = minPrice;
            return this;
        }

        public Builder maxPrice(BigDecimal maxPrice) {
            this.maxPrice = maxPrice;
            return this;
        }

        public Builder sortBy(String sortBy) {
            this.sortBy = sortBy;
            return this;
        }

        public Builder sortOrder(String sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public Builder pageNum(Integer pageNum) {
            this.pageNum = pageNum;
            return this;
        }

        public Builder pageSize(Integer pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public ProductQuery build() {
            return new ProductQuery(id, userId, categoryId, status, keyword, conditionLevel,
                    minPrice, maxPrice, sortBy, sortOrder, pageNum, pageSize);
        }
    }
}