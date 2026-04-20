package com.cartethyia.easyorange.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.cartethyia.easyorange.common.entity.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
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

    /**
     * 商品状态：0-草稿 1-上架 2-已售 3-下架
     */
    private Integer status;

    private Integer viewCount;

    private Integer conditionLevel;

    private String location;

    private String contactMethod;
}
