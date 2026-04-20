package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.ddd.ValueObject;
import com.cartethyia.easyorange.common.util.BizRequire;

import java.util.Objects;
import java.util.regex.Pattern;

public final class OrderNo implements ValueObject {

    private static final Pattern ORDER_NO_PATTERN = Pattern.compile("^ORD\\d{13,}$");

    private final String value;

    public OrderNo(String value) {
        BizRequire.notBlank(value, "订单号不能为空");
        this.value = value;
    }

    public static OrderNo of(String value) {
        return new OrderNo(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderNo orderNo = (OrderNo) o;
        return Objects.equals(value, orderNo.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "OrderNo{" + value + '}';
    }
}
