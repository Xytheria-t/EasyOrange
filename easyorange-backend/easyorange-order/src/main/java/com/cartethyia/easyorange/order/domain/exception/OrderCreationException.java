package com.cartethyia.easyorange.order.domain.exception;

import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.order.domain.constant.OrderResultCode;

/**
 * 订单创建异常 — 默认码订单域 B3009（见 {@link OrderResultCode#ORDER_ERROR}）。
 */
public class OrderCreationException extends BaseBusinessException {

    public OrderCreationException(String message) {
        super(message);
    }

    public OrderCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    protected String defaultCode() {
        return OrderResultCode.ORDER_ERROR.getCode();
    }
}
