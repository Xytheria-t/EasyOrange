package com.cartethyia.easyorange.payment.adapter.outbound.persistence.converter;

import com.cartethyia.easyorange.payment.adapter.outbound.persistence.PaymentDO;
import com.cartethyia.easyorange.payment.domain.aggregate.Payment;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentReconstructSpec;
import java.math.BigDecimal;

/**
 * 领域聚合 ↔ 数据对象的手写转换（静态工具类，无 Spring Bean）。
 * <p>
 * 取舍：不用 MapStruct——这里每个方法本就是手写实现，注解处理器无可生成，
 * 只会多编译出一个空的 {@code PaymentDataMapperImpl} Bean 并让注入点显得有意义。
 */
public final class PaymentDataMapper {

    private PaymentDataMapper() {}

    public static Payment toAggregate(PaymentDO po) {
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
                po.getVersion());
        return Payment.from(spec);
    }

    public static PaymentDO toPO(Payment aggregate) {
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
