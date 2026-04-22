package com.cartethyia.easyorange.user.domain.valueobject;

import com.cartethyia.easyorange.common.ddd.ValueObject;
import com.cartethyia.easyorange.common.util.BizRequire;

import java.util.Objects;
import java.util.regex.Pattern;

public final class Email implements ValueObject {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    private final String value;

    public Email(String value) {
        BizRequire.notBlank(value, "邮箱地址不能为空");
        String trimmed = value.trim();
        BizRequire.isTrue(isValid(trimmed), "邮箱格式不正确");
        this.value = trimmed.toLowerCase();
    }

    public static Email of(String value) {
        return new Email(value);
    }

    public String value() {
        return value;
    }

    private static boolean isValid(String value) {
        return EMAIL_PATTERN.matcher(value).matches();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Email email = (Email) o;
        return Objects.equals(value, email.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}