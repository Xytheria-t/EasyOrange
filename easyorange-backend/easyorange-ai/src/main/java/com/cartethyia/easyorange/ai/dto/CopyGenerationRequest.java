package com.cartethyia.easyorange.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record CopyGenerationRequest(
        @NotBlank(message = "商品名称不能为空") String productName,
        String categoryName,
        String conditionLevel,
        String originalPrice,
        String style) {

    public CopyGenerationRequest {
        if (style == null) {
            style = "standard";
        }
    }
}
