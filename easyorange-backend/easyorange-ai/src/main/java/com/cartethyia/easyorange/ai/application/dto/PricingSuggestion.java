package com.cartethyia.easyorange.ai.application.dto;

import java.math.BigDecimal;

public record PricingSuggestion(
        BigDecimal suggestedPrice, BigDecimal minPrice, BigDecimal maxPrice, String reasoning, String marketContext) {}
