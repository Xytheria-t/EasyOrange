package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.ddd.ValueObject;

import java.util.Objects;

public final class OrderRemark implements ValueObject {

    private final String value;

    public OrderRemark(String value) {
        this.value = value != null ? value.trim() : null;
    }

    public String value() {
        return value;
    }

    public boolean isBlank() {
        return value == null || value.isBlank();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderRemark that = (OrderRemark) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "OrderRemark{value='" + value + "'}";
    }
}
