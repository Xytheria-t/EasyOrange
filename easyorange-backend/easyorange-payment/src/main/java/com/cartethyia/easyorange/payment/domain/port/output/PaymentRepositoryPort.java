package com.cartethyia.easyorange.payment.domain.port.output;

import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;

import java.util.Optional;

public interface PaymentRepositoryPort {

    void save(PaymentAggregate aggregate);

    void update(PaymentAggregate aggregate);

    Optional<PaymentAggregate> findById(Long id);

    Optional<PaymentAggregate> findByPaymentNo(String paymentNo);

    Optional<PaymentAggregate> findByOrderId(Long orderId);
}
