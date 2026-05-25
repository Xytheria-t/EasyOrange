package com.cartethyia.easyorange.admin.dto.response;

import java.util.List;

public record CategoryTreeResponse(
    Long categoryId,
    String name,
    Integer level,
    Integer sortOrder,
    Integer status,
    List<CategoryTreeResponse> children
) {
    public static CategoryTreeResponse from(CategoryResponse vo, List<CategoryTreeResponse> children) {
        return new CategoryTreeResponse(vo.categoryId(), vo.name(), vo.level(),
            vo.sortOrder(), vo.status(), children != null ? children : List.of());
    }
}