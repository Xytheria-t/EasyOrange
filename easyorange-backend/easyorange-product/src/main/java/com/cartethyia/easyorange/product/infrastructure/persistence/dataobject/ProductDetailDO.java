package com.cartethyia.easyorange.product.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;

@TableName("product_detail")
public class ProductDetailDO extends BaseDO {

    private Long productId;
    private String description;

    public ProductDetailDO() {
    }

    public ProductDetailDO(Long productId, String description) {
        this.productId = productId;
        this.description = description;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long productId;
        private String description;

        public Builder productId(Long productId) {
            this.productId = productId;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public ProductDetailDO build() {
            return new ProductDetailDO(productId, description);
        }
    }
}
