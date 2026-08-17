package com.cartethyia.easyorange.payment.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.payment.domain.constant.PaymentConstant;

/**
 * 支付单号值对象 — 唯一生成入口与格式约束。
 * <p>
 * 支付单号由支付 ID（UUID v7，全局唯一）派生：{@code PAY + UUID 去横线}。
 * 与 {@code "PAY" + hashCode()} 相比，UUID 派生保证唯一（无哈希碰撞），
 * 且支付单号与支付 ID 一一对应；唯一性同时受 {@code uk_eo_payment_payment_no} 约束。
 */
public record PaymentNo(String value) {
    public PaymentNo {
        BizRequire.notNull(value, "支付编号不能为空");
        BizRequire.requireTrue(value.startsWith(PaymentConstant.PAYMENT_NO_PREFIX), "支付编号格式无效");
    }

    public static PaymentNo of(String value) {
        return new PaymentNo(value);
    }

    /**
     * 由支付 ID 生成支付单号。
     *
     * @param paymentId 支付 ID（UUID v7）
     */
    public static PaymentNo generate(String paymentId) {
        BizRequire.notNull(paymentId, "支付ID不能为空");
        return new PaymentNo(PaymentConstant.PAYMENT_NO_PREFIX + paymentId.replace("-", ""));
    }

    @Override
    public String toString() {
        return value;
    }
}
