package com.cartethyia.easyorange.order.domain.valueobject;

public record CancellationReason(String value) {
    public CancellationReason {
        if (value != null) {
            value = value.trim();
        }
    }
}