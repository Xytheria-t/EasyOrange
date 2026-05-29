package com.cartethyia.easyorange.ai.dto;

public record CopyGenerationRequest(
        String productName,
        String categoryName,
        Integer conditionLevel,
        String originalPrice,
        String style
) {}
