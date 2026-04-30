package com.cartethyia.easyorange.payment.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import com.cartethyia.easyorange.payment.domain.specification.PaymentSpecification;
import com.cartethyia.easyorange.payment.domain.valueobject.PaymentAmount;
import com.cartethyia.easyorange.payment.enums.PaymentResultCode;

import java.math.BigDecimal;

public class PaymentDomainService {

    public PaymentDomainService() {}

    public void validateRefund(PaymentAggregate aggregate, BigDecimal refundAmount) {
        if (!PaymentSpecification.canRefund(aggregate.status())) {
            throw BusinessException.of(PaymentResultCode.REFUND_NOT_ALLOWED);
        }

        PaymentAmount refundAmountVO = PaymentAmount.of(refundAmount);
        PaymentAmount currentAmount = PaymentAmount.of(aggregate.amount());
        if (!refundAmountVO.isLessThanOrEqualTo(currentAmount.value())) {
            throw BusinessException.of("退款金额不能超过支付金额");
        }

        BigDecimal totalRefunded = aggregate.refundedAmount().add(refundAmount);
        if (totalRefunded.compareTo(aggregate.amount()) > 0) {
            throw BusinessException.of("累计退款金额不能超过支付金额");
        }
    }
}
