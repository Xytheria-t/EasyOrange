package com.cartethyia.easyorange.order.domain.valueobject;

public record OrderNo(String value) {
    public OrderNo {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("订单号不能为空");
        }
    }

    public static OrderNo of(String value) {
        return new OrderNo(value);
    }
}