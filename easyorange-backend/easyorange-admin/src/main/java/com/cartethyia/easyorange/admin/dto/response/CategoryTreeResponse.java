package com.cartethyia.easyorange.admin.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record CategoryTreeResponse(
    Long categoryId,
    String name,
    Integer level,
    Integer sortOrder,
    Integer status,
    List<CategoryTreeResponse> children
) {
    public static CategoryTreeResponse from(CategoryResponse vo, List<CategoryTreeResponse> children) {
        return new CategoryTreeResponse(vo.getCategoryId(), vo.getName(), vo.getLevel(),
            vo.getSortOrder(), vo.getStatus(), children != null ? children : List.of());
    }
}