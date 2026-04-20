package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.ddd.ValueObject;
import com.cartethyia.easyorange.common.util.BizRequire;

import java.util.Objects;

public final class BuyerId implements ValueObject {

    private final Long value;

    public BuyerId(Long value) {
        BizRequire.notNull(value, "买家ID不能为空");
        BizRequire.isTrue(value > 0, "买家ID必须为正数");
        this.value = value;
    }

    public Long value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BuyerId buyerId = (BuyerId) o;
        return Objects.equals(value, buyerId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "BuyerId{" + value + '}';
    }
}
