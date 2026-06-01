package com.cartethyia.easyorange.payment.application.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import com.cartethyia.easyorange.payment.domain.exception.PaymentNotFoundException;
import com.cartethyia.easyorange.payment.domain.repository.PaymentQueryRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentQueryHandler {

    private final PaymentQueryRepositoryPort paymentQueryRepository;

    public PaymentAggregate getPaymentById(Long paymentId) {
        return paymentQueryRepository.findAggregateById(paymentId)
                .orElseThrow(PaymentNotFoundException::of);
    }

    public PaymentAggregate getPaymentByOrderId(Long orderId) {
        return paymentQueryRepository.findAggregateByOrderId(orderId)
                .orElseThrow(PaymentNotFoundException::of);
    }

    public PageResult<PaymentAggregate> getMyPayments(Integer status, Integer pageNum, Integer pageSize) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return queryPaymentsInternal(userId, status, pageNum, pageSize);
    }

    public PageResult<PaymentAggregate> queryPayments(Long userId, Integer status, Integer pageNum, Integer pageSize) {
        return queryPaymentsInternal(userId, status, pageNum, pageSize);
    }

    private PageResult<PaymentAggregate> queryPaymentsInternal(Long userId, Integer status, Integer pageNum, Integer pageSize) {
        int effectivePageNum = pageNum != null ? pageNum : 1;
        int effectivePageSize = pageSize != null ? pageSize : 20;

        List<PaymentAggregate> aggregates = paymentQueryRepository.findByUserIdAndStatus(
                userId, status, effectivePageNum, effectivePageSize);
        long total = paymentQueryRepository.countByUserIdAndStatus(userId, status);
        return PageResult.of(aggregates, total, effectivePageNum, effectivePageSize);
    }
}