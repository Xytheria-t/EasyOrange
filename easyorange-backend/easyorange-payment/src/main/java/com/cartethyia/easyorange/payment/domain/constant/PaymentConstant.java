package com.cartethyia.easyorange.payment.domain.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PaymentConstant {

    // ==================== 支付单号前缀 ====================
    /** 正式支付单号前缀（paymentNo = PAY + 支付 ID 去横线，见 {@code PaymentNo#generate}）。 */
    public static final String PAYMENT_NO_PREFIX = "PAY";

    public static final String MOCK_TXN_PREFIX = "TXN_";
    public static final String MOCK_REFUND_NO_PREFIX = "REF_";
}
