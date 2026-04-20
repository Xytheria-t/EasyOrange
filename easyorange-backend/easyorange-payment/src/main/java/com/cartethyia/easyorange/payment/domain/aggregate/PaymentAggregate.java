package com.cartethyia.easyorange.payment.domain.aggregate;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.payment.domain.event.PaymentCreatedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentFailedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentRefundedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentSucceededEvent;
import com.cartethyia.easyorange.payment.entity.Payment;
import com.cartethyia.easyorange.payment.enums.PaymentResultCode;
import com.cartethyia.easyorange.payment.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Accessors(chain = true)
public class PaymentAggregate {

    private Long id;
    private String paymentNo;
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private Integer paymentMethod;
    private Integer status;
    private String transactionId;
    private String refundReason;
    private LocalDateTime refundTime;
    private String attach;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static PaymentCreatedEvent create(Long orderId, Long userId, BigDecimal amount,
                                            Integer paymentMethod, String attach) {
        BizRequire.notNull(orderId, "订单ID不能为空");
        BizRequire.notNull(userId, "用户ID不能为空");
        BizRequire.notNull(amount, "支付金额不能为空");
        BizRequire.isTrue(amount.compareTo(BigDecimal.ZERO) > 0, "支付金额必须大于0");
        BizRequire.notNull(paymentMethod, "支付方式不能为空");

        Long paymentId = generatePaymentId();
        String paymentNo = generatePaymentNo();

        PaymentAggregate aggregate = PaymentAggregate.builder()
                .id(paymentId)
                .paymentNo(paymentNo)
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .paymentMethod(paymentMethod)
                .status(PaymentStatus.PENDING.getCode())
                .attach(attach)
                .build();

        return new PaymentCreatedEvent(paymentId, paymentNo, orderId, userId, amount, paymentMethod);
    }

    public PaymentSucceededEvent pay(String transactionId) {
        BizRequire.isTrue(PaymentStatus.canPay(this.status), PaymentResultCode.PAYMENT_INVALID_STATUS);

        this.status = PaymentStatus.SUCCESS.getCode();
        this.transactionId = transactionId;

        return new PaymentSucceededEvent(this.id, transactionId);
    }

    public PaymentFailedEvent fail(String reason) {
        BizRequire.isTrue(PaymentStatus.PENDING.getCode().equals(this.status), "只有待支付状态可以标记为失败");

        this.status = PaymentStatus.FAILED.getCode();

        return new PaymentFailedEvent(this.id, reason);
    }

    public PaymentRefundedEvent refund(String refundReason) {
        BizRequire.isTrue(PaymentStatus.canRefund(this.status), PaymentResultCode.REFUND_NOT_ALLOWED);

        this.status = PaymentStatus.REFUNDED.getCode();
        this.refundReason = refundReason;
        this.refundTime = LocalDateTime.now();

        return new PaymentRefundedEvent(this.id, refundReason);
    }

    public void close() {
        BizRequire.isTrue(PaymentStatus.canClose(this.status), PaymentResultCode.PAYMENT_INVALID_STATUS);

        this.status = PaymentStatus.CLOSED.getCode();
    }

    public static PaymentAggregate fromEntity(Payment payment) {
        if (payment == null) {
            return null;
        }
        return PaymentAggregate.builder()
                .id(payment.getId())
                .paymentNo(payment.getPaymentNo())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .refundReason(payment.getRefundReason())
                .refundTime(payment.getRefundTime())
                .attach(payment.getAttach())
                .createTime(payment.getCreateTime())
                .updateTime(payment.getUpdateTime())
                .build();
    }

    public Payment toEntity() {
        return Payment.builder()
                .id(this.id)
                .paymentNo(this.paymentNo)
                .orderId(this.orderId)
                .userId(this.userId)
                .amount(this.amount)
                .paymentMethod(this.paymentMethod)
                .status(this.status)
                .transactionId(this.transactionId)
                .refundReason(this.refundReason)
                .refundTime(this.refundTime)
                .attach(this.attach)
                .build();
    }

    private static Long generatePaymentId() {
        return System.currentTimeMillis();
    }

    private static String generatePaymentNo() {
        return "PAY" + System.currentTimeMillis();
    }
}