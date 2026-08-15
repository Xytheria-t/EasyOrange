package com.cartethyia.easyorange.ai.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record AiReviewRequest(
        @NotBlank(message = "商品名称不能为空") String productName,
        String description,
        String categoryName,
        String conditionLevel,
        String price,
        String sellerName,
        List<String> imageUrls) {}
