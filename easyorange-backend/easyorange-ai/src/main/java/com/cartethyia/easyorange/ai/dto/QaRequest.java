package com.cartethyia.easyorange.ai.dto;

public record QaRequest(
        String productId,
        String question,
        String productName,
        String productDescription,
        String categoryName,
        String price,
        String conditionLevel,
        String sellerName,
        String sellerCreditLevel
) {
}