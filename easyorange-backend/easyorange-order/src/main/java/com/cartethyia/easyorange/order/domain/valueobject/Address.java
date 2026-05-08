package com.cartethyia.easyorange.order.domain.valueobject;

public record Address(String value) {
    public Address {
    }

    public static Address of(String value) {
        return new Address(value);
    }
}
