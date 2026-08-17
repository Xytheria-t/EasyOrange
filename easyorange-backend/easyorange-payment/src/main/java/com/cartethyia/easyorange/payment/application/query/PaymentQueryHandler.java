package com.cartethyia.easyorange.payment.application.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.payment.application.port.query.PaymentQueryRepository;
import com.cartethyia.easyorange.payment.domain.aggregate.Payment;
import com.cartethyia.easyorange.payment.domain.constant.PaymentResultCode;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
import com.cartethyia.easyorange.payment.domain.exception.PaymentDomainException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentQueryHandler {

    private final PaymentQueryRepository paymentQueryRepository;

    public Payment getPaymentById(String paymentId, String operatorId) {
        return assertOwnership(paymentQueryRepository.findAggregateById(paymentId), operatorId);
    }

    public Payment getPaymentByOrderId(String orderId, String operatorId) {
        return assertOwnership(paymentQueryRepository.findAggregateByOrderId(orderId), operatorId);
    }

    /**
     * 资源归属校验（越权防护）— 查询者必须与支付单所属用户一致，
     * 不一致时按「记录不存在」处理（B4001→404），避免泄露支付单存在性。
     */
    private Payment assertOwnership(Optional<Payment> found, String operatorId) {
        Payment payment = found.orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
        if (!payment.userId().equals(operatorId)) {
            throw PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND);
        }
        return payment;
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
