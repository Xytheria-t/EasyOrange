package com.cartethyia.easyorange.product.adapter.outbound.persistence.product;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@TableName("eo_product_detail")
public class ProductDetailDO extends BaseDO {

    @TableField(exist = false)
    private String id;

    @TableId(value = "product_id", type = IdType.INPUT)
    private String productId;

    private String description;

    @Override
    public String getId() {
        return this.id != null ? this.id : this.productId;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }
}
