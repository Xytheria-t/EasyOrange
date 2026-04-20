package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.ddd.ValueObject;
import com.cartethyia.easyorange.product.enums.ConditionLevel;

import java.util.Objects;

public final class ConditionLevelVO implements ValueObject {

    private final ConditionLevel value;

    public ConditionLevelVO(ConditionLevel value) {
        this.value = Objects.requireNonNull(value, "新旧程度不能为空");
    }

    public ConditionLevel value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConditionLevelVO that = (ConditionLevelVO) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "ConditionLevelVO{" + value + '}';
    }
}
