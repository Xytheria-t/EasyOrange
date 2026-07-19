package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

import java.math.BigDecimal;
import java.util.List;

public record ProductUpdatedEvent(String productId, String userId, String categoryId, String name,
                                  BigDecimal price, BigDecimal originalPrice, Integer stock,
                                  Integer conditionLevel, String location, String contactMethod,
                                  String description, List<String> imageUrls) implements DomainEvent {

    public ProductUpdatedEvent {
        imageUrls = imageUrls != null ? List.copyOf(imageUrls) : List.of();
    }

    @Override
    public String aggregateId() {
        return productId;
    }
}
