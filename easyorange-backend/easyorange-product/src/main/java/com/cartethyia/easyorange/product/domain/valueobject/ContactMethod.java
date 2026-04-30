package com.cartethyia.easyorange.product.domain.valueobject;

public record ContactMethod(String value) {
    public ContactMethod {
        if (value != null) {
            value = value.trim();
        }
    }

    public static ContactMethod of(String value) {
        return new ContactMethod(value);
    }

    public String trimmed() {
        return value != null ? value.trim() : null;
    }
}