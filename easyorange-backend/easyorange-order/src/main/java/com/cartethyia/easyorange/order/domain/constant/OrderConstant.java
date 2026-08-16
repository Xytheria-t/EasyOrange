package com.cartethyia.easyorange.order.domain.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OrderConstant {

    public static final String DEFAULT_PAYMENT_METHOD = "WECHAT";
    public static final String PAYMENT_BIZ_TYPE = "ORDER";
    public static final String PAYMENT_DESC = "订单支付";
    public static final String DEFAULT_ADDRESS = "未指定";
}
