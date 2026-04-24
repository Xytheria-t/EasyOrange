package com.cartethyia.easyorange.product.domain.valueobject;

public record TradeLocation(String value) {
    public TradeLocation {
        if (value != null) {
            value = value.trim();
        }
    }

    public String trimmed() {
        return value != null ? value.trim() : null;
    }
}