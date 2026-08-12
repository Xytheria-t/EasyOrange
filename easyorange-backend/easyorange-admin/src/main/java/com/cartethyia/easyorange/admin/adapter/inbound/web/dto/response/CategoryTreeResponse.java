package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import java.util.List;

public record CategoryTreeResponse(
        String categoryId,
        String name,
        Integer level,
        Integer sortOrder,
        Integer status,
        List<CategoryTreeResponse> children) {}
