package com.cartethyia.easyorange.payment.domain.aggregate;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.payment.domain.event.PaymentClosedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentCreatedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentFailedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentRefundedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentSucceededEvent;
import com.cartethyia.easyorange.payment.domain.gateway.PaymentGateway;
import com.cartethyia.easyorange.payment.domain.gateway.PaymentResult;
import com.cartethyia.easyorange.payment.domain.gateway.RefundResult;
import com.cartethyia.easyorange.common.util.SnowflakeIdGenerator;
import com.cartethyia.easyorange.payment.domain.specification.PaymentSpecification;
import com.cartethyia.easyorange.payment.domain.valueobject.PaymentAmount;
import com.cartethyia.easyorange.payment.enums.PaymentResultCode;
import com.cartethyia.easyorange.payment.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentAggregate {

    private final Long id;
    private final String paymentNo;
    private final Long orderId;
    private final Long userId;
    private final BigDecimal amount;
    private BigDecimal refundedAmount;
    private final Integer paymentMethod;
    private PaymentStatus status;
    private String transactionId;
    private String refundReason;
    private LocalDateTime refundTime;
    private final String attach;
    private final LocalDateTime createTime;
    private final LocalDateTime updateTime;

    private PaymentAggregate(Long id, String paymentNo, Long orderId, Long userId,
                             BigDecimal amount, BigDecimal refundedAmount, Integer paymentMethod,
                             PaymentStatus status, String transactionId, String refundReason,
                             LocalDateTime refundTime, String attach,
                             LocalDateTime createTime, LocalDateTime updateTime) {
        this.id = id;
        this.paymentNo = paymentNo;
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.refundedAmount = refundedAmount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.transactionId = transactionId;
        this.refundReason = refundReason;
        this.refundTime = refundTime;
        this.attach = attach;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    public static PaymentAggregate create(Long orderId, Long userId, BigDecimal amount,
                                          Integer paymentMethod, String attach) {
        BizRequire.notNull(orderId, "订单ID不能为空");
        BizRequire.notNull(userId, "用户ID不能为空");
        BizRequire.notNull(amount, "支付金额不能为空");
        BizRequire.requireTrue(amount.compareTo(BigDecimal.ZERO) > 0, "支付金额必须大于0");
        BizRequire.notNull(paymentMethod, "支付方式不能为空");

        Long paymentId = generatePaymentId();
        String paymentNo = generatePaymentNo();

        return new PaymentAggregate(
                paymentId, paymentNo, orderId, userId,
                amount, BigDecimal.ZERO, paymentMethod,
                PaymentStatus.PENDING, null, null,
                null, attach, null, null
        );
    }

    public static PaymentCreatedEvent publishCreatedEvent(PaymentAggregate aggregate) {
        return new PaymentCreatedEvent(
                aggregate.id, aggregate.paymentNo, aggregate.orderId,
                aggregate.userId, aggregate.amount, aggregate.paymentMethod
        );
    }

    public BaseDomainEvent pay(PaymentGateway gateway) {
        BizRequire.requireTrue(PaymentSpecification.canPay(this.status), PaymentResultCode.PAYMENT_INVALID_STATUS);

        PaymentResult result = gateway.pay(this);

        if (result.isSuccess()) {
            this.status = PaymentStatus.SUCCESS;
            this.transactionId = result.getTransactionId();
            return new PaymentSucceededEvent(this.id, result.getTransactionId());
        } else {
            this.status = PaymentStatus.FAILED;
            return new PaymentFailedEvent(this.id, result.getErrorMessage());
        }
    }

    public void preparePay() {
        BizRequire.requireTrue(PaymentSpecification.canPay(this.status), PaymentResultCode.PAYMENT_INVALID_STATUS);
        this.status = PaymentStatus.PAYING;
    }

    public void cancelPay() {
        BizRequire.requireTrue(PaymentStatus.PAYING.equals(this.status), "只有支付中状态可以取消支付");
        this.status = PaymentStatus.PENDING;
    }

    public BaseDomainEvent confirmPay(PaymentResult result) {
        BizRequire.requireTrue(PaymentStatus.PAYING.equals(this.status), "只有支付中状态可以确认支付结果");

        if (result.isSuccess()) {
            this.status = PaymentStatus.SUCCESS;
            this.transactionId = result.getTransactionId();
            return new PaymentSucceededEvent(this.id, result.getTransactionId());
        } else {
            this.status = PaymentStatus.FAILED;
            return new PaymentFailedEvent(this.id, result.getErrorMessage());
        }
    }

    public BaseDomainEvent refund(BigDecimal refundAmount, PaymentGateway gateway) {
        BizRequire.requireTrue(PaymentSpecification.canRefund(this.status), PaymentResultCode.REFUND_NOT_ALLOWED);

        PaymentAmount refundAmountVO = PaymentAmount.of(refundAmount);
        PaymentAmount currentAmount = PaymentAmount.of(this.amount);
        BizRequire.requireTrue(refundAmountVO.isLessThanOrEqualTo(currentAmount.value()), "退款金额不能超过支付金额");

        BigDecimal totalRefunded = this.refundedAmount.add(refundAmount);
        BizRequire.requireTrue(totalRefunded.compareTo(this.amount) <= 0, "累计退款金额不能超过支付金额");

        RefundResult result = gateway.refund(this, refundAmount);

        if (result.isSuccess()) {
            this.refundedAmount = totalRefunded;
            if (this.refundedAmount.compareTo(this.amount) == 0) {
                this.status = PaymentStatus.REFUNDED;
            } else {
                this.status = PaymentStatus.PARTIALLY_REFUNDED;
            }
            this.refundReason = "退款成功";
            this.refundTime = LocalDateTime.now();
            return new PaymentRefundedEvent(this.id, "部分退款: " + refundAmount);
        } else {
            throw BusinessException.of(result.getErrorMessage());
        }
    }

    public void prepareRefund(BigDecimal refundAmount) {
        BizRequire.requireTrue(PaymentSpecification.canRefund(this.status), PaymentResultCode.REFUND_NOT_ALLOWED);

        PaymentAmount refundAmountVO = PaymentAmount.of(refundAmount);
        PaymentAmount currentAmount = PaymentAmount.of(this.amount);
        BizRequire.requireTrue(refundAmountVO.isLessThanOrEqualTo(currentAmount.value()), "退款金额不能超过支付金额");

        BigDecimal totalRefunded = this.refundedAmount.add(refundAmount);
        BizRequire.requireTrue(totalRefunded.compareTo(this.amount) <= 0, "累计退款金额不能超过支付金额");

        this.status = PaymentStatus.REFUNDING;
    }

    public void cancelRefund() {
        BizRequire.requireTrue(PaymentStatus.REFUNDING.equals(this.status), "只有退款中状态可以取消退款");
        this.status = PaymentStatus.SUCCESS;
    }

    public BaseDomainEvent confirmRefund(RefundResult result, BigDecimal refundAmount) {
        BizRequire.requireTrue(PaymentStatus.REFUNDING.equals(this.status), "只有退款中状态可以确认退款结果");

        if (result.isSuccess()) {
            this.refundedAmount = this.refundedAmount.add(refundAmount);
            if (this.refundedAmount.compareTo(this.amount) == 0) {
                this.status = PaymentStatus.REFUNDED;
            } else {
                this.status = PaymentStatus.PARTIALLY_REFUNDED;
            }
            this.refundReason = "退款成功";
            this.refundTime = LocalDateTime.now();
            return new PaymentRefundedEvent(this.id, "部分退款: " + refundAmount);
        } else {
            this.status = PaymentStatus.SUCCESS;
            throw BusinessException.of(result.getErrorMessage());
        }
    }

    public BaseDomainEvent directRefund(String refundReason) {
        BizRequire.requireTrue(PaymentSpecification.canRefund(this.status), PaymentResultCode.REFUND_NOT_ALLOWED);

        this.status = PaymentStatus.REFUNDED;
        this.refundedAmount = this.amount;
        this.refundReason = refundReason;
        this.refundTime = LocalDateTime.now();

        return new PaymentRefundedEvent(this.id, refundReason);
    }

    public PaymentFailedEvent fail(String reason) {
        BizRequire.requireTrue(PaymentSpecification.canFail(this.status), "只有待支付状态可以标记为失败");

        this.status = PaymentStatus.FAILED;

        return new PaymentFailedEvent(this.id, reason);
    }

    public PaymentClosedEvent close() {
        BizRequire.requireTrue(PaymentSpecification.canClose(this.status), PaymentResultCode.PAYMENT_INVALID_STATUS);

        this.status = PaymentStatus.CLOSED;

        return new PaymentClosedEvent(this.id);
    }

    public static PaymentAggregate reconstruct(Long id, String paymentNo, Long orderId, Long userId,
                                                BigDecimal amount, BigDecimal refundedAmount, Integer paymentMethod,
                                                PaymentStatus status, String transactionId, String refundReason,
                                                LocalDateTime refundTime, String attach,
                                                LocalDateTime createTime, LocalDateTime updateTime) {
        return new PaymentAggregate(id, paymentNo, orderId, userId, amount, refundedAmount,
                paymentMethod, status, transactionId, refundReason, refundTime, attach, createTime, updateTime);
    }

    public Long id() { return id; }
    public String paymentNo() { return paymentNo; }
    public Long orderId() { return orderId; }
    public Long userId() { return userId; }
    public BigDecimal amount() { return amount; }
    public BigDecimal refundedAmount() { return refundedAmount; }
    public Integer paymentMethod() { return paymentMethod; }
    public PaymentStatus status() { return status; }
    public String transactionId() { return transactionId; }
    public String refundReason() { return refundReason; }
    public LocalDateTime refundTime() { return refundTime; }
    public String attach() { return attach; }
    public LocalDateTime createTime() { return createTime; }
    public LocalDateTime updateTime() { return updateTime; }

    private static Long generatePaymentId() {
        return SnowflakeIdGenerator.getInstance().nextId();
    }

    private static String generatePaymentNo() {
        return "PAY" + SnowflakeIdGenerator.getInstance().nextId();
    }
}
