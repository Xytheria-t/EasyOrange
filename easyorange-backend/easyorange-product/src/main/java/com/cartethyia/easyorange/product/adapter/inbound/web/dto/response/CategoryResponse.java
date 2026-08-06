package com.cartethyia.easyorange.product.adapter.inbound.web.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record CategoryResponse(
        String id,
        String name,
        String parentId,
        Integer level,
        String icon,
        Integer sortOrder,
        Integer status,
        LocalDateTime createTime,
        Integer productCount,
        List<CategoryResponse> children) {}
