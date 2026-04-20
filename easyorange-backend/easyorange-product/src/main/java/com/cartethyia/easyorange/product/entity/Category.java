package com.cartethyia.easyorange.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 分类实体
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("category")
public class Category extends BaseDO {

    private String name;

    private Long parentId;

    private Integer level;

    private String icon;

    private Integer sortOrder;

    private Integer status;
}
