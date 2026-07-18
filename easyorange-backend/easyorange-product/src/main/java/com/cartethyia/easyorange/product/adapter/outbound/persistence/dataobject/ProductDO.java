package com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.cartethyia.easyorange.common.entity.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@TableName("eo_product")
public class ProductDO extends BaseDO {

    private String userId;
    private String categoryId;
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
    private LocalDateTime priceUpdateTime;
}
