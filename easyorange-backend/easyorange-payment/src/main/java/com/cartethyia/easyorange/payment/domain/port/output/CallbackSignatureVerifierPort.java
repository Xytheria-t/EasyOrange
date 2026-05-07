package com.cartethyia.easyorange.payment.domain.port.output;

public interface CallbackSignatureVerifierPort {

    void verify(String paymentNo, String transactionId, String sign);
}
