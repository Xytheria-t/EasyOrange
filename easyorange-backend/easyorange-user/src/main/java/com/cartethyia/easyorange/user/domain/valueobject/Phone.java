package com.cartethyia.easyorange.user.domain.valueobject;

import com.cartethyia.easyorange.common.ddd.ValueObject;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.user.constant.UserConstants;

import java.util.Objects;
import java.util.regex.Pattern;

public final class Phone implements ValueObject {

    private static final Pattern PHONE_PATTERN = Pattern.compile(UserConstants.PHONE_REGEX);

    private final String value;

    public Phone(String value) {
        BizRequire.notBlank(value, "手机号不能为空");
        String trimmed = value.trim();
        BizRequire.isTrue(isValid(trimmed), "手机号格式不正确");
        this.value = trimmed;
    }

    public static Phone of(String value) {
        return new Phone(value);
    }

    public String value() {
        return value;
    }

    private static boolean isValid(String value) {
        return PHONE_PATTERN.matcher(value).matches();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Phone phone = (Phone) o;
        return Objects.equals(value, phone.value);
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