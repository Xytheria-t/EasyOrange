package com.cartethyia.easyorange.user.domain.valueobject;

import com.cartethyia.easyorange.common.ddd.ValueObject;
import com.cartethyia.easyorange.common.util.BizRequire;

import java.util.Objects;

public final class Nickname implements ValueObject {

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 30;

    private final String value;

    public Nickname(String value) {
        BizRequire.notBlank(value, "昵称不能为空");
        String trimmed = value.trim();
        BizRequire.isTrue(trimmed.length() >= MIN_LENGTH, "昵称长度不能少于" + MIN_LENGTH + "个字符");
        BizRequire.isTrue(trimmed.length() <= MAX_LENGTH, "昵称长度不能超过" + MAX_LENGTH + "个字符");
        this.value = trimmed;
    }

    public static Nickname of(String value) {
        return new Nickname(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Nickname nickname = (Nickname) o;
        return Objects.equals(value, nickname.value);
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