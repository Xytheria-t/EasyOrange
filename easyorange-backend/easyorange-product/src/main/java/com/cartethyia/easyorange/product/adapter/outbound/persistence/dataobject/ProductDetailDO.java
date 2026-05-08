package com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.framework.entity.BaseDO;

@TableName("eo_product_detail")
public class ProductDetailDO extends BaseDO {

    @TableField(exist = false)
    private Long id;

    @TableId(value = "product_id", type = IdType.INPUT)
    private Long productId;

    private String description;

    public ProductDetailDO() {
    }

    public ProductDetailDO(Long productId, String description) {
        this.productId = productId;
        this.description = description;
    }

    @Override
    public Long getId() {
        return this.id != null ? this.id : this.productId;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
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
