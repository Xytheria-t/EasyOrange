package com.cartethyia.easyorange.payment.domain.port;

import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;

import java.util.List;
import java.util.Optional;

public interface PaymentQueryRepositoryPort {

    Optional<PaymentAggregate> findAggregateById(String id);

    Optional<PaymentAggregate> findAggregateByPaymentNo(String paymentNo);

    Optional<PaymentAggregate> findAggregateByOrderId(String orderId);

    List<PaymentAggregate> findByUserIdAndStatus(String userId, String status, int pageNum, int pageSize);

    long countByUserIdAndStatus(String userId, String status);
}
