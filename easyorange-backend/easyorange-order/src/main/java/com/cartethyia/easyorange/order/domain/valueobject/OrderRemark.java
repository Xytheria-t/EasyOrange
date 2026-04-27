package com.cartethyia.easyorange.order.domain.valueobject;

public record OrderRemark(String value) {
    public OrderRemark {
        if (value != null) {
            value = value.trim();
        }
    }

    public boolean isBlank() {
        return value == null || value.isBlank();
    }
}