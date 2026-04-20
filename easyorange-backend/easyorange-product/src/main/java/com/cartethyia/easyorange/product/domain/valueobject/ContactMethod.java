package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.ddd.ValueObject;

import java.util.Objects;

public final class ContactMethod implements ValueObject {

    private final String value;

    public ContactMethod(String value) {
        this.value = value != null ? value.trim() : null;
    }

    public String value() {
        return value;
    }

    public String trimmed() {
        return value != null ? value.trim() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContactMethod that = (ContactMethod) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "ContactMethod{" + value + '}';
    }
}
