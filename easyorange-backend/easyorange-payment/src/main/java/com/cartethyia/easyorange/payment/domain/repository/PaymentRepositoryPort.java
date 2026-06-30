package com.cartethyia.easyorange.payment.domain.repository;

import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;

import java.util.Optional;

public interface PaymentRepositoryPort {

    void save(PaymentAggregate aggregate);

    void update(PaymentAggregate aggregate);

    Optional<PaymentAggregate> findById(String id);

    Optional<PaymentAggregate> findByPaymentNo(String paymentNo);

    Optional<PaymentAggregate> findByOrderId(String orderId);
}