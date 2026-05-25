package com.cartethyia.easyorange.order.domain.valueobject;

import lombok.Builder;

@Builder
public record ProductSnapshot(
    Long productId,
    String name,
    String image,
    String description,
    Money price,
    String conditionLevel
) {}
