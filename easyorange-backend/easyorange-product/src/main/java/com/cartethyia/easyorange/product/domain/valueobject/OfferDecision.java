package com.cartethyia.easyorange.product.domain.valueobject;

import java.math.BigDecimal;

public record OfferDecision(
        DecisionType type,
        BigDecimal acceptedPrice,
        BigDecimal counterPrice,
        String reason
) {
    public enum DecisionType { ACCEPT, COUNTER, REJECT }
}
