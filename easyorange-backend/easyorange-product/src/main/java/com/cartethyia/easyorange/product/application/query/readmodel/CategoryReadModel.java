package com.cartethyia.easyorange.product.application.query.readmodel;

import com.cartethyia.easyorange.product.domain.enums.CategoryStatus;
import java.io.Serializable;
import java.time.LocalDateTime;

public record CategoryReadModel(
        String id,
        String name,
        /** 一级分类为 null（与 eo_category.parent_id 同口径）。 */
        String parentId,
        Integer level,
        String icon,
        Integer sortOrder,
        CategoryStatus status,
        LocalDateTime createTime,
        Integer productCount)
        implements Serializable {}
