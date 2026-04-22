package com.cartethyia.easyorange.user.domain.valueobject;

import com.cartethyia.easyorange.common.ddd.ValueObject;
import com.cartethyia.easyorange.common.util.BizRequire;

import java.util.Objects;

public final class UserId implements ValueObject {

    private final Long value;

    public UserId(Long value) {
        BizRequire.notNull(value, "用户ID不能为空");
        BizRequire.isTrue(value > 0, "用户ID必须为正数");
        this.value = value;
    }

    public static UserId of(Long value) {
        return new UserId(value);
    }

    public Long value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserId userId = (UserId) o;
        return Objects.equals(value, userId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "UserId{" + value + '}';
    }
}