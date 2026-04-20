package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.ddd.ValueObject;

import java.util.Objects;

public final class Version implements ValueObject {

    private final Integer value;

    public Version(Integer value) {
        this.value = Objects.requireNonNull(value, "版本号不能为空");
    }

    public Version(int value) {
        this.value = value;
    }

    public static Version initial() {
        return new Version(0);
    }

    public Integer value() {
        return value;
    }

    public Version next() {
        return new Version(value + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Version version = (Version) o;
        return Objects.equals(value, version.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "Version{" + value + '}';
    }
}
