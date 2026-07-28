package com.cartethyia.easyorange.product.adapter.outbound.persistence.product;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;

@TableName("eo_product_detail")
public class ProductDetailDO extends BaseDO {

    @TableField(exist = false)
    private String id;

    @TableId(value = "product_id", type = IdType.INPUT)
    private String productId;

    private String description;

    public ProductDetailDO() {
    }

    public ProductDetailDO(String productId, String description) {
        this.productId = productId;
        this.description = description;
    }

    @Override
    public String getId() {
        return this.id != null ? this.id : this.productId;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
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
        private String productId;
        private String description;

        public Builder productId(String productId) {
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
