package com.cartethyia.easyorange.admin.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record CategoryTreeVO(
    Long categoryId,
    String name,
    Integer level,
    Integer sortOrder,
    Integer status,
    List<CategoryTreeVO> children
) {
    public static CategoryTreeVO from(CategoryVO vo, List<CategoryTreeVO> children) {
        return new CategoryTreeVO(vo.getCategoryId(), vo.getName(), vo.getLevel(),
            vo.getSortOrder(), vo.getStatus(), children != null ? children : List.of());
    }
}
