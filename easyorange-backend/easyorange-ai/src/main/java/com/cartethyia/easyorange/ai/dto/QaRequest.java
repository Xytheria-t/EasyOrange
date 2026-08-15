package com.cartethyia.easyorange.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record QaRequest(
        String productId,
        @NotBlank(message = "问题不能为空") String question,
        String productName,
        String productDescription,
        String categoryName,
        String price,
        String conditionLevel,
        String sellerName,
        String sellerCreditLevel) {}
