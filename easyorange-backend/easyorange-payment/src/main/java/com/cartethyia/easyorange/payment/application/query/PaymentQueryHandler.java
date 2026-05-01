package com.cartethyia.easyorange.payment.application.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import com.cartethyia.easyorange.payment.domain.exception.PaymentNotFoundException;
import com.cartethyia.easyorange.payment.domain.repository.PaymentQueryRepository;
import com.cartethyia.easyorange.payment.enums.PaymentMethod;
import com.cartethyia.easyorange.payment.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentQueryHandler {

    private final PaymentQueryRepository paymentQueryRepository;

    public PaymentView getPaymentById(Long paymentId) {
        PaymentAggregate aggregate = paymentQueryRepository.findAggregateById(paymentId)
                .orElseThrow(PaymentNotFoundException::of);
        return toPaymentView(aggregate);
    }

    public PaymentView getPaymentByOrderId(Long orderId) {
        PaymentAggregate aggregate = paymentQueryRepository.findAggregateByOrderId(orderId)
                .orElseThrow(PaymentNotFoundException::of);
        return toPaymentView(aggregate);
    }

    public PageResult<PaymentView> getMyPayments(PaymentQuery query) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return queryPaymentsInternal(userId, query.getStatus(), query.getPageNum(), query.getPageSize());
    }

    public PageResult<PaymentView> queryPayments(PaymentQuery query) {
        return queryPaymentsInternal(query.getUserId(), query.getStatus(), query.getPageNum(), query.getPageSize());
    }

    private PageResult<PaymentView> queryPaymentsInternal(Long userId, Integer status, Integer pageNum, Integer pageSize) {
        int effectivePageNum = pageNum != null ? pageNum : 1;
        int effectivePageSize = pageSize != null ? pageSize : 20;

        List<PaymentAggregate> aggregates = paymentQueryRepository.findByUserIdAndStatus(
                userId, status, effectivePageNum, effectivePageSize);
        long total = paymentQueryRepository.countByUserIdAndStatus(userId, status);
        List<PaymentView> views = aggregates.stream()
                .map(this::toPaymentView)
                .toList();
        return PageResult.of(views, total, effectivePageNum, effectivePageSize);
    }

    private PaymentView toPaymentView(PaymentAggregate aggregate) {
        return PaymentView.builder()
                .id(aggregate.id())
                .paymentNo(aggregate.paymentNo())
                .orderId(aggregate.orderId())
                .userId(aggregate.userId())
                .amount(aggregate.amount())
                .refundedAmount(aggregate.refundedAmount())
                .paymentMethod(aggregate.paymentMethod())
                .paymentMethodDesc(PaymentMethod.getDescByCode(aggregate.paymentMethod()))
                .status(aggregate.status().getCode())
                .statusDesc(PaymentStatus.getDescByCode(aggregate.status().getCode()))
                .transactionId(aggregate.transactionId())
                .refundReason(aggregate.refundReason())
                .refundTime(aggregate.refundTime())
                .createTime(aggregate.createTime())
                .updateTime(aggregate.updateTime())
                .build();
    }
}
