package com.cartethyia.easyorange.ai.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record PricingRequest(
        @NotBlank(message = "商品名称不能为空") String productName,
        String description,
        String categoryName,
        String conditionLevel,
        BigDecimal originalPrice) {}
