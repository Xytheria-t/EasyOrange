package com.cartethyia.easyorange.payment.adapter.outbound.persistence.converter;

import com.cartethyia.easyorange.payment.adapter.outbound.persistence.PaymentDO;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
import org.mapstruct.Mapper;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface PaymentDataMapper {

    default PaymentAggregate toAggregate(PaymentDO po) {
        if (po == null) {
            return null;
        }
        return PaymentAggregate.reconstruct(
                po.getId(),
                po.getPaymentNo(),
                po.getOrderId(),
                po.getUserId(),
                po.getAmount(),
                po.getRefundedAmount() != null ? po.getRefundedAmount() : BigDecimal.ZERO,
                po.getPaymentMethod(),
                PaymentStatus.fromCode(po.getStatus()),
                po.getTransactionId(),
                po.getRefundReason(),
                po.getRefundTime(),
                po.getAttach(),
                po.getCreateTime(),
                po.getUpdateTime(),
                po.getVersion()
        );
    }

    default PaymentDO toPO(PaymentAggregate aggregate) {
        if (aggregate == null) {
            return null;
        }
        return PaymentDO.builder()
                .id(aggregate.id())
                .paymentNo(aggregate.paymentNo())
                .orderId(aggregate.orderId())
                .userId(aggregate.userId())
                .amount(aggregate.amount())
                .refundedAmount(aggregate.refundedAmount())
                .paymentMethod(aggregate.paymentMethod())
                .status(aggregate.status().getCode())
                .transactionId(aggregate.transactionId())
                .refundReason(aggregate.refundReason())
                .refundTime(aggregate.refundTime())
                .attach(aggregate.attach())
                .version(aggregate.version())
                .build();
    }
}
