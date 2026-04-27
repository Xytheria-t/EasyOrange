package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.order.enums.OrderStatus;

public record OrderStatusVO(OrderStatus value) {
    public OrderStatusVO {
        if (value == null) {
            throw new IllegalArgumentException("订单状态不能为空");
        }
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
}