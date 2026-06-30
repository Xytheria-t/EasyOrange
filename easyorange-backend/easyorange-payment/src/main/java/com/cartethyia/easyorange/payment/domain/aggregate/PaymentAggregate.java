package com.cartethyia.easyorange.payment.domain.aggregate;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
import com.cartethyia.easyorange.payment.domain.event.PaymentClosedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentCreatedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentFailedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentRefundedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentSucceededEvent;
import com.cartethyia.easyorange.payment.domain.constant.PaymentResultCode;
import com.cartethyia.easyorange.payment.domain.exception.PaymentDomainException;
import com.cartethyia.easyorange.payment.domain.port.PaymentResult;
import com.cartethyia.easyorange.payment.domain.port.RefundResult;
import com.cartethyia.easyorange.payment.domain.specification.PaymentSpecification;
import com.cartethyia.easyorange.common.domain.Money;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentAggregate {

    private final String id;
    private final String paymentNo;
    private final String orderId;
    private final String userId;
    private final Money amount;
    private final Money refundedAmount;
    private final Integer paymentMethod;
    private final PaymentStatus status;
    private final String transactionId;
    private final String refundReason;
    private final LocalDateTime refundTime;
    private final String attach;
    private final LocalDateTime createTime;
    private final LocalDateTime updateTime;
    private final int version;

    private PaymentAggregate(String id, String paymentNo, String orderId, String userId,
                             Money amount, Money refundedAmount, Integer paymentMethod,
                             PaymentStatus status, String transactionId, String refundReason,
                             LocalDateTime refundTime, String attach,
                             LocalDateTime createTime, LocalDateTime updateTime, int version) {
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
        this.version = version;
    }

    // ==================== Factory ====================

    public static PaymentCreatedResult create(String paymentId, String orderId, String userId, BigDecimal amount,
                                              Integer paymentMethod, String attach) {
        BizRequire.notNull(paymentId, "支付ID不能为空");
        BizRequire.notNull(orderId, "订单ID不能为空");
        BizRequire.notNull(userId, "用户ID不能为空");
        BizRequire.notNull(amount, "支付金额不能为空");
        BizRequire.requireTrue(amount.compareTo(BigDecimal.ZERO) > 0, "支付金额必须大于0");
        BizRequire.notNull(paymentMethod, "支付方式不能为空");

        String paymentNo = "PAY" + paymentId.hashCode();
        Money paymentAmount = Money.of(amount);

        PaymentAggregate aggregate = new PaymentAggregate(
                paymentId, paymentNo, orderId, userId,
                paymentAmount, Money.ZERO, paymentMethod,
                PaymentStatus.PENDING, null, null,
                null, attach, null, null, 0
        );

        PaymentCreatedEvent event = new PaymentCreatedEvent(
                paymentId, paymentNo, orderId, userId, amount, paymentMethod
        );

        return new PaymentCreatedResult(aggregate, event);
    }

    // ==================== Reconstruction ====================

    public static PaymentAggregate reconstruct(String id, String paymentNo, String orderId, String userId,
                                                BigDecimal amount, BigDecimal refundedAmount, Integer paymentMethod,
                                                PaymentStatus status, String transactionId, String refundReason,
                                                LocalDateTime refundTime, String attach,
                                                LocalDateTime createTime, LocalDateTime updateTime, Integer version) {
        return new PaymentAggregate(id, paymentNo, orderId, userId,
                Money.of(amount), Money.of(refundedAmount),
                paymentMethod, status, transactionId, refundReason, refundTime, attach,
                createTime, updateTime, version != null ? version : 0);
    }

    // ==================== Guard Methods ====================

    public boolean canPay() {
        return PaymentSpecification.canPay(this.status);
    }

    public boolean canRefund() {
        return PaymentSpecification.canRefund(this.status);
    }

    public boolean canClose() {
        return PaymentSpecification.canClose(this.status);
    }

    public boolean canFail() {
        return PaymentSpecification.canFail(this.status);
    }

    public boolean canConfirmPay() {
        return PaymentSpecification.canConfirmPay(this.status);
    }

    public boolean canConfirmRefund() {
        return PaymentSpecification.canConfirmRefund(this.status);
    }

    // ==================== State Transitions ====================

    /**
     * 准备支付：将状态变为 PAYING
     */
    public PayPreparedResult preparePay() {
        if (!canPay()) {
            throw PaymentDomainException.of(PaymentResultCode.PAYMENT_INVALID_STATUS, "当前状态不允许支付: " + this.status);
        }
        PaymentAggregate updated = withStatus(PaymentStatus.PAYING, nextVersion());
        return new PayPreparedResult(updated);
    }

    /**
     * 确认支付结果：根据网关结果变为 SUCCESS 或 FAILED
     */
    public PayConfirmedResult confirmPay(PaymentResult result) {
        if (!canConfirmPay()) {
            throw PaymentDomainException.of(PaymentResultCode.PAYMENT_INVALID_STATUS, "只有支付中状态可以确认支付结果");
        }
        if (result.isSuccess()) {
            PaymentAggregate updated = withSuccess(result.getTransactionId());
            return new PayConfirmedResult(updated, new PaymentSucceededEvent(this.id, result.getTransactionId()));
        } else {
            PaymentAggregate updated = withStatus(PaymentStatus.FAILED, nextVersion());
            return new PayConfirmedResult(updated, new PaymentFailedEvent(this.id, result.getErrorMessage()));
        }
    }

    /**
     * 取消支付：从 PAYING 回退到 PENDING
     */
    public CancelPayResult cancelPay() {
        if (!PaymentStatus.PAYING.equals(this.status)) {
            throw PaymentDomainException.of(PaymentResultCode.PAYMENT_INVALID_STATUS, "只有支付中状态可以取消支付");
        }
        PaymentAggregate updated = withStatus(PaymentStatus.PENDING, nextVersion());
        return new CancelPayResult(updated);
    }

    /**
     * 准备退款：将状态变为 REFUNDING
     */
    public RefundPreparedResult prepareRefund(BigDecimal refundAmount) {
        if (!canRefund()) {
            throw PaymentDomainException.of(PaymentResultCode.REFUND_NOT_ALLOWED, "当前状态不允许退款: " + this.status);
        }
        validateRefundAmount(refundAmount);
        PaymentAggregate updated = withStatus(PaymentStatus.REFUNDING, nextVersion());
        return new RefundPreparedResult(updated);
    }

    /**
     * 确认退款结果：根据网关结果变为 REFUNDED 或 PARTIALLY_REFUNDED
     */
    public RefundConfirmedResult confirmRefund(RefundResult result, BigDecimal refundAmount) {
        if (!canConfirmRefund()) {
            throw PaymentDomainException.of(PaymentResultCode.PAYMENT_INVALID_STATUS, "只有退款中状态可以确认退款结果");
        }
        validateRefundAmount(refundAmount);

        if (!result.isSuccess()) {
            throw PaymentDomainException.of(PaymentResultCode.REFUND_NOT_ALLOWED, result.getErrorMessage() != null ? result.getErrorMessage() : "退款失败");
        }

        Money refundMoney = Money.of(refundAmount);
        Money newRefundedAmount = this.refundedAmount.add(refundMoney);
        PaymentStatus newStatus;
        String refundEventReason;
        if (newRefundedAmount.isEqualTo(this.amount)) {
            newStatus = PaymentStatus.REFUNDED;
            refundEventReason = "全额退款: " + refundAmount;
        } else {
            newStatus = PaymentStatus.PARTIALLY_REFUNDED;
            refundEventReason = "部分退款: " + refundAmount;
        }
        PaymentAggregate updated = withRefundResult(newStatus, newRefundedAmount, refundEventReason, LocalDateTime.now(), nextVersion());
        return new RefundConfirmedResult(updated, new PaymentRefundedEvent(this.id, refundEventReason));
    }

    /**
     * 取消退款：从 REFUNDING 回退到 SUCCESS
     */
    public CancelRefundResult cancelRefund() {
        if (!PaymentStatus.REFUNDING.equals(this.status)) {
            throw PaymentDomainException.of(PaymentResultCode.PAYMENT_INVALID_STATUS, "只有退款中状态可以取消退款");
        }
        PaymentAggregate updated = withStatus(PaymentStatus.SUCCESS, nextVersion());
        return new CancelRefundResult(updated);
    }

    /**
     * 直接退款（不复用网关两阶段流程）
     */
    public DirectRefundResult directRefund(String refundReason) {
        if (!canRefund()) {
            throw PaymentDomainException.of(PaymentResultCode.REFUND_NOT_ALLOWED, "当前状态不允许退款: " + this.status);
        }
        PaymentAggregate updated = withRefundResult(
                PaymentStatus.REFUNDED, this.amount, refundReason, LocalDateTime.now(), nextVersion()
        );
        return new DirectRefundResult(updated, new PaymentRefundedEvent(this.id, refundReason));
    }

    /**
     * 标记为失败
     */
    public FailedResult fail(String reason) {
        if (!canFail()) {
            throw PaymentDomainException.of(PaymentResultCode.PAYMENT_INVALID_STATUS, "只有待支付状态可以标记为失败");
        }
        PaymentAggregate updated = withStatus(PaymentStatus.FAILED, nextVersion());
        return new FailedResult(updated, new PaymentFailedEvent(this.id, reason));
    }

    /**
     * 关闭支付
     */
    public ClosedResult close() {
        if (!canClose()) {
            throw PaymentDomainException.of(PaymentResultCode.PAYMENT_INVALID_STATUS, "当前状态不允许关闭: " + this.status);
        }
        PaymentAggregate updated = withStatus(PaymentStatus.CLOSED, nextVersion());
        return new ClosedResult(updated, new PaymentClosedEvent(this.id));
    }

    // ==================== Result Records ====================

    public record PaymentCreatedResult(PaymentAggregate aggregate, PaymentCreatedEvent event) {}
    public record PayPreparedResult(PaymentAggregate aggregate) {}
    public record PayConfirmedResult(PaymentAggregate aggregate, BaseDomainEvent event) {}
    public record CancelPayResult(PaymentAggregate aggregate) {}
    public record RefundPreparedResult(PaymentAggregate aggregate) {}
    public record RefundConfirmedResult(PaymentAggregate aggregate, PaymentRefundedEvent event) {}
    public record CancelRefundResult(PaymentAggregate aggregate) {}
    public record DirectRefundResult(PaymentAggregate aggregate, PaymentRefundedEvent event) {}
    public record FailedResult(PaymentAggregate aggregate, PaymentFailedEvent event) {}
    public record ClosedResult(PaymentAggregate aggregate, PaymentClosedEvent event) {}

    // ==================== Internal Helpers ====================

    private PaymentAggregate withStatus(PaymentStatus newStatus, int newVersion) {
        return new PaymentAggregate(id, paymentNo, orderId, userId,
                amount, refundedAmount, paymentMethod, newStatus,
                transactionId, refundReason, refundTime, attach,
                createTime, LocalDateTime.now(), newVersion);
    }

    private PaymentAggregate withRefundResult(PaymentStatus newStatus, Money newRefundedAmount,
                                              String reason, LocalDateTime time, int newVersion) {
        return new PaymentAggregate(id, paymentNo, orderId, userId,
                amount, newRefundedAmount, paymentMethod, newStatus,
                transactionId, reason, time, attach,
                createTime, LocalDateTime.now(), newVersion);
    }

    private PaymentAggregate withSuccess(String transactionId) {
        return new PaymentAggregate(id, paymentNo, orderId, userId,
                amount, refundedAmount, paymentMethod, PaymentStatus.SUCCESS,
                transactionId, refundReason, refundTime, attach,
                createTime, LocalDateTime.now(), nextVersion());
    }

    private void validateRefundAmount(BigDecimal refundAmount) {
        Money refundMoney = Money.of(refundAmount);
        if (!refundMoney.isLessThanOrEqual(this.amount)) {
            throw PaymentDomainException.of(PaymentResultCode.REFUND_NOT_ALLOWED, "退款金额不能超过支付金额");
        }
        Money totalRefunded = this.refundedAmount.add(refundMoney);
        if (totalRefunded.isGreaterThan(this.amount)) {
            throw PaymentDomainException.of(PaymentResultCode.REFUND_NOT_ALLOWED, "累计退款金额不能超过支付金额");
        }
    }

    private int nextVersion() {
        return version + 1;
    }

    // ==================== Getters ====================

    public String id() { return id; }
    public String paymentNo() { return paymentNo; }
    public String orderId() { return orderId; }
    public String userId() { return userId; }
    public BigDecimal amount() { return amount.value(); }
    public BigDecimal refundedAmount() { return refundedAmount.value(); }
    public Integer paymentMethod() { return paymentMethod; }
    public PaymentStatus status() { return status; }
    public String transactionId() { return transactionId; }
    public String refundReason() { return refundReason; }
    public LocalDateTime refundTime() { return refundTime; }
    public String attach() { return attach; }
    public LocalDateTime createTime() { return createTime; }
    public LocalDateTime updateTime() { return updateTime; }
    public int version() { return version; }
}