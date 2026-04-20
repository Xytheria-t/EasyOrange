package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.ddd.ValueObject;

import java.util.Objects;

public final class CancellationReason implements ValueObject {

    private final String value;

    public CancellationReason(String value) {
        this.value = value != null ? value.trim() : null;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CancellationReason that = (CancellationReason) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "CancellationReason{value='" + value + "'}";
    }
}
