package com.cartethyia.easyorange.payment.application.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.payment.application.port.query.PaymentQueryRepository;
import com.cartethyia.easyorange.payment.domain.aggregate.Payment;
import com.cartethyia.easyorange.payment.domain.constant.PaymentResultCode;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
import com.cartethyia.easyorange.payment.domain.exception.PaymentDomainException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentQueryHandler {

    private final PaymentQueryRepository paymentQueryRepository;

    public Payment getPaymentById(String paymentId) {
        return paymentQueryRepository
                .findAggregateById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
    }

    public Payment getPaymentByOrderId(String orderId) {
        return paymentQueryRepository
                .findAggregateByOrderId(orderId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
    }

    /**
     * 我的支付记录 — userId 由 Web 边界解析。
     */
    public PageResult<Payment> getMyPayments(String userId, PaymentListQuery query) {
        return queryPaymentsInternal(userId, query.status(), query.pageNum(), query.pageSize());
    }

    /**
     * 通用支付记录查询（管理端） — 通过 PaymentListQuery 收敛参数。
     */
    public PageResult<Payment> queryPayments(PaymentListQuery query) {
        return queryPaymentsInternal(query.userId(), query.status(), query.pageNum(), query.pageSize());
    }

    private PageResult<Payment> queryPaymentsInternal(
            String userId, PaymentStatus status, Integer pageNum, Integer pageSize) {
        int effectivePageNum = pageNum != null ? pageNum : 1;
        int effectivePageSize = pageSize != null ? pageSize : 20;

        List<Payment> aggregates =
                paymentQueryRepository.findByUserIdAndStatus(userId, status, effectivePageNum, effectivePageSize);
        long total = paymentQueryRepository.countByUserIdAndStatus(userId, status);
        return PageResult.of(aggregates, total, effectivePageNum, effectivePageSize);
    }
}
