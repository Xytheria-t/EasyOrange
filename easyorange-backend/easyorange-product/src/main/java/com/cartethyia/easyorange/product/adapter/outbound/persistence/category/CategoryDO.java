package com.cartethyia.easyorange.product.adapter.outbound.persistence.category;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.cartethyia.easyorange.common.entity.BaseDO;
import com.cartethyia.easyorange.product.domain.enums.CategoryStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@TableName("eo_category")
public class CategoryDO extends BaseDO {

    private String name;
    private String parentId;
    private Integer level;
    private String icon;
    private Integer sortOrder;
    private CategoryStatus status;

    @Version
    private Integer version;
}
