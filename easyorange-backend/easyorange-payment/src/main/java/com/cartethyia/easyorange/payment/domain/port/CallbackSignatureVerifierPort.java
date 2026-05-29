package com.cartethyia.easyorange.payment.domain.port;

public interface CallbackSignatureVerifierPort {

    void verify(String paymentNo, String transactionId, String sign);
}