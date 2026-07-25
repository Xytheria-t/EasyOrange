package com.cartethyia.easyorange.product.application.command;

import java.math.BigDecimal;
import java.util.List;

public record UpdateProductCommand(
        String id, String categoryId, String name, BigDecimal price,
        BigDecimal originalPrice, Integer stock, String conditionLevel,
        String location, String contactMethod, String description,
        List<String> imageUrls
) implements ProductCommand {}
