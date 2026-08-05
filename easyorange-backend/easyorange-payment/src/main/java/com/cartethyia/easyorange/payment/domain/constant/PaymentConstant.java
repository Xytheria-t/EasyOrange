package com.cartethyia.easyorange.payment.domain.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PaymentConstant {

    // ==================== 支付单号前缀 ====================
    public static final String MOCK_PAYMENT_NO_PREFIX = "MOCK_";
    public static final String MOCK_TXN_PREFIX = "TXN_";
    public static final String MOCK_REFUND_NO_PREFIX = "REF_";

    // ==================== 配置状态 ====================
    public static final Integer CONFIG_STATUS_ENABLED = 1;
    public static final Integer CONFIG_STATUS_DISABLED = 0;
}
