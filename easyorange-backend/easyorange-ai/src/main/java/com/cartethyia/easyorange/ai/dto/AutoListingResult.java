package com.cartethyia.easyorange.ai.dto;

import java.math.BigDecimal;
import java.util.List;

public record AutoListingResult(
        String title,
        String description,
        BigDecimal price,
        String categoryName,
        String categoryId,
        String conditionLevel,
        String location,
        List<String> tags,
        List<String> imageDescriptions) {}
