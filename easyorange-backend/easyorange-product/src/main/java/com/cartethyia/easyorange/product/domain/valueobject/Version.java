package com.cartethyia.easyorange.product.domain.valueobject;

public record Version(Integer value) {
    public Version {
        if (value == null) {
            throw new IllegalArgumentException("版本号不能为空");
        }
    }

    public static Version initial() {
        return new Version(0);
    }

    public Version next() {
        return new Version(value + 1);
    }
}