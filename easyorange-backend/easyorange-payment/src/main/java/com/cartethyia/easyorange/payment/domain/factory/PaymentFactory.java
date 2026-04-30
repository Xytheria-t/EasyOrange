package com.cartethyia.easyorange.payment.domain.factory;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.common.util.SnowflakeIdGenerator;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import com.cartethyia.easyorange.payment.domain.valueobject.PaymentAmount;
import com.cartethyia.easyorange.payment.domain.valueobject.PaymentMethodVO;
import com.cartethyia.easyorange.payment.enums.PaymentStatus;

import java.math.BigDecimal;

public class PaymentFactory {

    private PaymentFactory() {}

    public static PaymentAggregate create(Long orderId, Long userId, BigDecimal amount,
                                          Integer paymentMethodCode, String attach) {
        BizRequire.notNull(orderId, "订单ID不能为空");
        BizRequire.notNull(userId, "用户ID不能为空");
        BizRequire.notNull(amount, "支付金额不能为空");
        BizRequire.requireTrue(amount.compareTo(BigDecimal.ZERO) > 0, "支付金额必须大于0");
        BizRequire.notNull(paymentMethodCode, "支付方式不能为空");

        PaymentAmount paymentAmount = PaymentAmount.of(amount);
        PaymentMethodVO paymentMethod = PaymentMethodVO.of(paymentMethodCode);

        Long paymentId = SnowflakeIdGenerator.getInstance().nextId();
        String paymentNo = "PAY" + SnowflakeIdGenerator.getInstance().nextId();

        return PaymentAggregate.reconstruct(
                paymentId, paymentNo, orderId, userId,
                paymentAmount.value(), BigDecimal.ZERO, paymentMethod.code(),
                PaymentStatus.PENDING, null, null, null, attach, null, null
        );
    }
}
