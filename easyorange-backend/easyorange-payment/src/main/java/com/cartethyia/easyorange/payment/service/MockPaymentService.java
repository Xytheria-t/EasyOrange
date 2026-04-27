package com.cartethyia.easyorange.payment.service;

import com.cartethyia.easyorange.payment.dto.request.MockPaymentRequest;
import com.cartethyia.easyorange.payment.dto.vo.PaymentVO;
import com.cartethyia.easyorange.payment.entity.Payment;

import java.math.BigDecimal;

public interface MockPaymentService {

    PaymentVO createMockPayment(Long orderId, Integer paymentMethod, BigDecimal amount);

    PaymentVO processMockPayment(MockPaymentRequest request);

    PaymentVO mockPaymentSuccess(Long paymentId);

    PaymentVO mockPaymentFail(Long paymentId);

    void mockRefund(Long paymentId, String reason);
}
