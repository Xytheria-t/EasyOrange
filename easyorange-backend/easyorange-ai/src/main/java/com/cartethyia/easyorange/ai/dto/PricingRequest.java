package com.cartethyia.easyorange.ai.dto;

import java.math.BigDecimal;

public record PricingRequest(
        String productName,
        String description,
        String categoryName,
        String conditionLevel,
        BigDecimal originalPrice
) {}
