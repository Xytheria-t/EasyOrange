package com.cartethyia.easyorange.product.domain.valueobject;

public record TradeLocation(String value) {
    public TradeLocation {
        if (value != null) {
            value = value.trim();
        }
    }

    public static TradeLocation of(String value) {
        return new TradeLocation(value);
    }

    public String trimmed() {
        return value != null ? value.trim() : null;
    }
}