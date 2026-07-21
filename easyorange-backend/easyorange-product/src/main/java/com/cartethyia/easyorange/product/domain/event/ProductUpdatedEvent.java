package com.cartethyia.easyorange.product.domain.event;

import java.math.BigDecimal;
import java.util.List;

public record ProductUpdatedEvent(String productId, String userId, String categoryId, String name,
                                  BigDecimal price, BigDecimal originalPrice, Integer stock,
                                  Integer conditionLevel, String location, String contactMethod,
                                  String description, List<String> imageUrls) implements ProductEvent {

    public ProductUpdatedEvent {
        imageUrls = imageUrls != null ? List.copyOf(imageUrls) : List.of();
    }
}
