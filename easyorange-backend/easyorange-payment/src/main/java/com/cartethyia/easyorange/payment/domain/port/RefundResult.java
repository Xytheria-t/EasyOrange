package com.cartethyia.easyorange.payment.domain.port;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RefundResult {

    private final boolean success;
    private final String refundNo;
    private final String errorMessage;

    public static RefundResult success(String refundNo) {
        return new RefundResult(true, refundNo, null);
    }

    public static RefundResult failure(String errorMessage) {
        return new RefundResult(false, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getRefundNo() {
        return refundNo;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
