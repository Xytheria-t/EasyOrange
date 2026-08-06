package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record ContactMethod(String value) {

    public static final int MAX_LENGTH = 100;

    public ContactMethod {
        if (value != null) {
            value = value.trim();
            BizRequire.requireTrue(value.length() <= MAX_LENGTH, "联系方式长度不能超过 " + MAX_LENGTH + " 个字符");
        }
    }

    public static ContactMethod of(String value) {
        return new ContactMethod(value);
    }

    public boolean isBlank() {
        return value == null || value.isBlank();
    }

    public boolean isNotBlank() {
        return !isBlank();
    }
}
