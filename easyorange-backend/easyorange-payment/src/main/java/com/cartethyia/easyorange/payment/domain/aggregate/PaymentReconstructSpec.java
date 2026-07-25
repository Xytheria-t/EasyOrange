package com.cartethyia.easyorange.payment.domain.aggregate;

import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付聚合根重建参数 — 收敛 from() 的长参数列表。
 * <p>
 * 状态字段使用领域枚举类型，由 TypeHandler 完成 VARCHAR 列互转。
 *
 * @param version 乐观锁版本号（可为 null，重建时按 0 处理）
 */
public record PaymentReconstructSpec(
        String id,
        String paymentNo,
        String orderId,
        String userId,
        BigDecimal amount,
        BigDecimal refundedAmount,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        String transactionId,
        String refundReason,
        LocalDateTime refundTime,
        String attach,
        LocalDateTime createTime,
        LocalDateTime updateTime,
        Integer version
) {}
