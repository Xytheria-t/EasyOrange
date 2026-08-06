package com.cartethyia.easyorange.product.application.query.readmodel;

import java.io.Serializable;
import java.time.LocalDateTime;

public record CategoryReadModel(
        String id,
        String name,
        String parentId,
        Integer level,
        String icon,
        Integer sortOrder,
        Integer status,
        LocalDateTime createTime,
        Integer productCount)
        implements Serializable {}
