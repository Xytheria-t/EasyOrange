package com.cartethyia.easyorange.order.domain.valueobject;

public record Version(Integer value) {
    public Version {
        if (value == null) {
            throw new IllegalArgumentException("版本号不能为空");
        }
    }

    public Version next() {
        return new Version(value + 1);
    }
}