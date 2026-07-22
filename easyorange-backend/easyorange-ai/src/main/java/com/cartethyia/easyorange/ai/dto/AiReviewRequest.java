package com.cartethyia.easyorange.ai.dto;

import java.util.List;

public record AiReviewRequest(
        String productName,
        String description,
        String categoryName,
        String conditionLevel,
        String price,
        String sellerName,
        List<String> imageUrls
) {}
