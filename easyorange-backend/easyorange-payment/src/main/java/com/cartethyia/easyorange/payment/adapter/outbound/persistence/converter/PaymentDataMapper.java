package com.cartethyia.easyorange.payment.adapter.outbound.persistence.converter;

import com.cartethyia.easyorange.payment.adapter.outbound.persistence.PaymentDO;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentReconstructSpec;
import org.mapstruct.Mapper;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface PaymentDataMapper {

    default PaymentAggregate toAggregate(PaymentDO po) {
        if (po == null) {
            return null;
        }
        var spec = new PaymentReconstructSpec(
                po.getId(),
                po.getPaymentNo(),
                po.getOrderId(),
                po.getUserId(),
                po.getAmount(),
                po.getRefundedAmount() != null ? po.getRefundedAmount() : BigDecimal.ZERO,
                po.getPaymentMethod(),
                po.getStatus(),
                po.getTransactionId(),
                po.getRefundReason(),
                po.getRefundTime(),
                po.getAttach(),
                po.getCreateTime(),
                po.getUpdateTime(),
                po.getVersion()
        );
        return PaymentAggregate.from(spec);
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
                .status(aggregate.status())
                .transactionId(aggregate.transactionId())
                .refundReason(aggregate.refundReason())
                .refundTime(aggregate.refundTime())
                .attach(aggregate.attach())
                .version(aggregate.version())
                .build();
    }
}
