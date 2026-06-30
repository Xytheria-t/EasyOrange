package com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.framework.entity.BaseDO;

@TableName("eo_product_image")
public class ProductImageDO extends BaseDO {

    private String productId;
    private String imageUrl;
    private Integer sortOrder;
    private Integer isMain;

    public ProductImageDO() {
    }

    public ProductImageDO(String productId, String imageUrl, Integer sortOrder, Integer isMain) {
        this.productId = productId;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
        this.isMain = isMain;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getIsMain() {
        return isMain;
    }

    public void setIsMain(Integer isMain) {
        this.isMain = isMain;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String productId;
        private String imageUrl;
        private Integer sortOrder;
        private Integer isMain;

        public Builder productId(String productId) {
            this.productId = productId;
            return this;
        }

        public Builder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Builder sortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public Builder isMain(Integer isMain) {
            this.isMain = isMain;
            return this;
        }

        public ProductImageDO build() {
            return new ProductImageDO(productId, imageUrl, sortOrder, isMain);
        }
    }
}
