package com.cartethyia.easyorange.product.application.query.readmodel;

import java.io.Serializable;
import java.time.LocalDateTime;

public record CategoryReadModel(
        Long id,
        String name,
        Long parentId,
        Integer level,
        String icon,
        Integer sortOrder,
        Integer status,
        LocalDateTime createTime,
        Integer productCount
) implements Serializable {
}
