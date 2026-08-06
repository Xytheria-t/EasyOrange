package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.domain.Money;
import lombok.Builder;

@Builder
public record ProductSnapshot(
        String productId, String name, String image, String description, Money price, String conditionLevel) {}
