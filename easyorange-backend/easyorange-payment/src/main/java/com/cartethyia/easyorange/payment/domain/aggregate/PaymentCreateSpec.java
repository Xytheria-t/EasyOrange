package com.cartethyia.easyorange.payment.domain.aggregate;

import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;

import java.math.BigDecimal;

/**
 * 支付聚合根工厂参数 — 收敛 create() 的长参数列表。
 *
 * @param paymentId     支付 ID
 * @param orderId       订单 ID
 * @param userId        用户 ID
 * @param amount        支付金额
 * @param paymentMethod 支付方式
 * @param attach        附加数据
 */
public record PaymentCreateSpec(
        String paymentId,
        String orderId,
        String userId,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        String attach
) {}
