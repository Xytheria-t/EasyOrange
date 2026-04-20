package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.ddd.ValueObject;
import com.cartethyia.easyorange.order.enums.OrderStatus;

import java.util.Objects;

public final class OrderStatusVO implements ValueObject {

    private final OrderStatus value;

    public OrderStatusVO(OrderStatus value) {
        this.value = Objects.requireNonNull(value, "订单状态不能为空");
    }

    public OrderStatus value() {
        return value;
    }

    public boolean canCancel() {
        return OrderStatus.canCancel(value.getCode());
    }

    public boolean canPay() {
        return OrderStatus.canPay(value.getCode());
    }

    public boolean canShip() {
        return OrderStatus.canShip(value.getCode());
    }

    public boolean canConfirmReceipt() {
        return OrderStatus.canConfirmReceipt(value.getCode());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderStatusVO that = (OrderStatusVO) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "OrderStatusVO{" + value + '}';
    }
}
