package com.cartethyia.easyorange.payment.domain.repository;

import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;

import java.util.List;
import java.util.Optional;

public interface PaymentQueryRepositoryPort {

    Optional<PaymentAggregate> findAggregateById(Long id);

    Optional<PaymentAggregate> findAggregateByPaymentNo(String paymentNo);

    Optional<PaymentAggregate> findAggregateByOrderId(Long orderId);

    List<PaymentAggregate> findByUserIdAndStatus(Long userId, Integer status, int pageNum, int pageSize);

    long countByUserIdAndStatus(Long userId, Integer status);
}