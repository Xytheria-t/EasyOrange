package com.cartethyia.easyorange.ai.dto;

import java.math.BigDecimal;

public record PricingRequest(
        String productName,
        String description,
        String categoryName,
        Integer conditionLevel,
        BigDecimal originalPrice
) {}
