package com.cartethyia.easyorange.payment.domain.repository;

import com.cartethyia.easyorange.payment.domain.aggregate.Payment;

import java.util.Optional;

public interface PaymentRepositoryPort {

    void save(Payment aggregate);

    void update(Payment aggregate);

    Optional<Payment> findById(String id);

    Optional<Payment> findByPaymentNo(String paymentNo);

    Optional<Payment> findByOrderId(String orderId);
}