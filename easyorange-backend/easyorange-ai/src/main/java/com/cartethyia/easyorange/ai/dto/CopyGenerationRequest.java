package com.cartethyia.easyorange.ai.dto;

public record CopyGenerationRequest(
        String productName,
        String categoryName,
        String conditionLevel,
        String originalPrice,
        String style
) {}
