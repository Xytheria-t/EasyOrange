package com.cartethyia.easyorange.payment.constant;

/**
 * 支付常量
 *
 * @author cartethyia
 * @date 2026/03/06
 */
public class PaymentConstants {

    private PaymentConstants() {
    }

    public static final String PAYMENT_NO_PREFIX = "PAY";

    public static final long PAYMENT_TIMEOUT_MINUTES = 30;

    public static final String CALLBACK_SUCCESS = "SUCCESS";

    public static final String CALLBACK_FAIL = "FAIL";

    public static final String MOCK_PAYMENT_NO_PREFIX = "MOCK_";

    public static final String MOCK_TXN_PREFIX = "MOCK_TXN_";

    public static final int CONFIG_STATUS_ENABLED = 1;
}
