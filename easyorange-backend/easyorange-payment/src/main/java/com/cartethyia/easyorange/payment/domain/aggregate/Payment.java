package com.cartethyia.easyorange.payment.domain.aggregate;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.event.Transition;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;
import com.cartethyia.easyorange.payment.domain.constant.PaymentResultCode;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
import com.cartethyia.easyorange.payment.domain.event.PaymentClosedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentConfirmEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentCreatedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentFailedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentRefundedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentSucceededEvent;
import com.cartethyia.easyorange.payment.domain.exception.PaymentDomainException;
import com.cartethyia.easyorange.payment.domain.port.PaymentResult;
import com.cartethyia.easyorange.payment.domain.port.RefundResult;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付聚合根 —— 不可变对象
 * <p>
 * 状态机：
 * <pre>
 * PENDING → PAYING → SUCCESS
 *   ↓         ↓        ↓
 * CLOSED    FAILED   REFUNDING → REFUNDED
 *                      ↓
 *                PARTIALLY_REFUNDED
 * </pre>
 * <p>
 * 状态转换返回 {@link Transition}（带事件）或 {@code Payment}（中间态，无事件）。
 * 聚合根工厂与重建入口通过 spec record 收敛参数。
 */
public class Payment {

    private final String id;
    private final String paymentNo;
    private final String orderId;
    private final String userId;
    private final Money amount;
    private final Money refundedAmount;
    private final PaymentMethod paymentMethod;
    private final PaymentStatus status;
    private final String transactionId;
    private final String refundReason;
    private final LocalDateTime refundTime;
    private final String attach;
    private final LocalDateTime createTime;
    private final LocalDateTime updateTime;
    private final int version;

    private Payment(
            String id,
            String paymentNo,
            String orderId,
            String userId,
            Money amount,
            Money refundedAmount,
            PaymentMethod paymentMethod,
            PaymentStatus status,
            String transactionId,
            String refundReason,
            LocalDateTime refundTime,
            String attach,
            LocalDateTime createTime,
            LocalDateTime updateTime,
            int version) {
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

    /**
     * 创建新支付。
     *
     * @param spec 创建参数（收敛 paymentId/orderId/userId/amount/paymentMethod/attach）
     * @return 支付创建结果（含聚合根与领域事件）
     */
    public static Transition<Payment, PaymentCreatedEvent> create(PaymentCreateSpec spec) {
        BizRequire.notNull(spec.paymentId(), "支付ID不能为空");
        BizRequire.notNull(spec.orderId(), "订单ID不能为空");
        BizRequire.notNull(spec.userId(), "用户ID不能为空");
        BizRequire.notNull(spec.amount(), "支付金额不能为空");
        BizRequire.requireTrue(spec.amount().compareTo(BigDecimal.ZERO) > 0, "支付金额必须大于0");
        BizRequire.notNull(spec.paymentMethod(), "支付方式不能为空");

        String paymentNo = "PAY" + spec.paymentId().hashCode();
        Money paymentAmount = Money.of(spec.amount());

        Payment aggregate = new Payment(
                spec.paymentId(),
                paymentNo,
                spec.orderId(),
                spec.userId(),
                paymentAmount,
                Money.ZERO,
                spec.paymentMethod(),
                PaymentStatus.PENDING,
                null,
                null,
                null,
                spec.attach(),
                null,
                null,
                0);

        PaymentCreatedEvent event = new PaymentCreatedEvent(
                spec.paymentId(),
                paymentNo,
                spec.orderId(),
                spec.userId(),
                spec.amount(),
                spec.paymentMethod().getCode());

        return new Transition<>(aggregate, event);
    }

    // ==================== Reconstruction ====================

    /**
     * 从持久层重建聚合根（统一入口）。
     * <p>
     * 状态字段使用领域枚举类型，由 TypeHandler 完成 VARCHAR 列互转。
     */
    public static Payment from(PaymentReconstructSpec spec) {
        return new Payment(
                spec.id(),
                spec.paymentNo(),
                spec.orderId(),
                spec.userId(),
                Money.of(spec.amount()),
                Money.of(spec.refundedAmount()),
                spec.paymentMethod(),
                spec.status(),
                spec.transactionId(),
                spec.refundReason(),
                spec.refundTime(),
                spec.attach(),
                spec.createTime(),
                spec.updateTime(),
                spec.version() != null ? spec.version() : 0);
    }

    // ==================== Guard Methods ====================

    public boolean canPay() {
        return PaymentStatusGuard.canPay(this.status);
    }

    public boolean canRefund() {
        return PaymentStatusGuard.canRefund(this.status);
    }

    public boolean canClose() {
        return PaymentStatusGuard.canClose(this.status);
    }

    public boolean canFail() {
        return PaymentStatusGuard.canFail(this.status);
    }

    public boolean canConfirmPay() {
        return PaymentStatusGuard.canConfirmPay(this.status);
    }

    public boolean canConfirmRefund() {
        return PaymentStatusGuard.canConfirmRefund(this.status);
    }

    // ==================== State Transitions ====================

    /**
     * 准备支付：将状态变为 PAYING（中间态，不发事件）。
     */
    public Payment preparePay() {
        if (!canPay()) {
            throw PaymentDomainException.of(PaymentResultCode.PAYMENT_INVALID_STATUS, "当前状态不允许支付: " + this.status);
        }
        return withStatus(PaymentStatus.PAYING, nextVersion());
    }

    /**
     * 确认支付结果：根据网关结果变为 SUCCESS 或 FAILED。
     */
    public Transition<Payment, PaymentConfirmEvent> confirmPay(PaymentResult result) {
        if (!canConfirmPay()) {
            throw PaymentDomainException.of(PaymentResultCode.PAYMENT_INVALID_STATUS, "只有支付中状态可以确认支付结果");
        }
        if (result.isSuccess()) {
            Payment updated = withSuccess(result.getTransactionId());
            return new Transition<>(updated, new PaymentSucceededEvent(this.id, result.getTransactionId()));
        } else {
            Payment updated = withStatus(PaymentStatus.FAILED, nextVersion());
            return new Transition<>(updated, new PaymentFailedEvent(this.id, result.getErrorMessage()));
        }
    }

    /**
     * 取消支付：从 PAYING 回退到 PENDING（中间态，不发事件）。
     */
    public Payment cancelPay() {
        if (!PaymentStatus.PAYING.equals(this.status)) {
            throw PaymentDomainException.of(PaymentResultCode.PAYMENT_INVALID_STATUS, "只有支付中状态可以取消支付");
        }
        return withStatus(PaymentStatus.PENDING, nextVersion());
    }

    /**
     * 准备退款：将状态变为 REFUNDING（中间态，不发事件）。
     */
    public Payment prepareRefund(BigDecimal refundAmount) {
        if (!canRefund()) {
            throw PaymentDomainException.of(PaymentResultCode.REFUND_NOT_ALLOWED, "当前状态不允许退款: " + this.status);
        }
        validateRefundAmount(refundAmount);
        return withStatus(PaymentStatus.REFUNDING, nextVersion());
    }

    /**
     * 确认退款结果：根据网关结果变为 REFUNDED 或 PARTIALLY_REFUNDED。
     */
    public Transition<Payment, PaymentRefundedEvent> confirmRefund(RefundResult result, BigDecimal refundAmount) {
        if (!canConfirmRefund()) {
            throw PaymentDomainException.of(PaymentResultCode.PAYMENT_INVALID_STATUS, "只有退款中状态可以确认退款结果");
        }
        validateRefundAmount(refundAmount);

        if (!result.isSuccess()) {
            throw PaymentDomainException.of(
                    PaymentResultCode.REFUND_NOT_ALLOWED,
                    result.getErrorMessage() != null ? result.getErrorMessage() : "退款失败");
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
        Payment updated =
                withRefundResult(newStatus, newRefundedAmount, refundEventReason, LocalDateTime.now(), nextVersion());
        return new Transition<>(updated, new PaymentRefundedEvent(this.id, refundEventReason));
    }

    /**
     * 取消退款：从 REFUNDING 回退到 SUCCESS（中间态，不发事件）。
     */
    public Payment cancelRefund() {
        if (!PaymentStatus.REFUNDING.equals(this.status)) {
            throw PaymentDomainException.of(PaymentResultCode.PAYMENT_INVALID_STATUS, "只有退款中状态可以取消退款");
        }
        return withStatus(PaymentStatus.SUCCESS, nextVersion());
    }

    /**
     * 直接退款（不复用网关两阶段流程，dev mock 路径使用）。
     */
    public Transition<Payment, PaymentRefundedEvent> directRefund(String refundReason) {
        if (!canRefund()) {
            throw PaymentDomainException.of(PaymentResultCode.REFUND_NOT_ALLOWED, "当前状态不允许退款: " + this.status);
        }
        Payment updated =
                withRefundResult(PaymentStatus.REFUNDED, this.amount, refundReason, LocalDateTime.now(), nextVersion());
        return new Transition<>(updated, new PaymentRefundedEvent(this.id, refundReason));
    }

    /**
     * 标记为失败。
     */
    public Transition<Payment, PaymentFailedEvent> fail(String reason) {
        if (!canFail()) {
            throw PaymentDomainException.of(PaymentResultCode.PAYMENT_INVALID_STATUS, "只有待支付状态可以标记为失败");
        }
        Payment updated = withStatus(PaymentStatus.FAILED, nextVersion());
        return new Transition<>(updated, new PaymentFailedEvent(this.id, reason));
    }

    /**
     * 关闭支付。
     */
    public Transition<Payment, PaymentClosedEvent> close() {
        if (!canClose()) {
            throw PaymentDomainException.of(PaymentResultCode.PAYMENT_INVALID_STATUS, "当前状态不允许关闭: " + this.status);
        }
        Payment updated = withStatus(PaymentStatus.CLOSED, nextVersion());
        return new Transition<>(updated, new PaymentClosedEvent(this.id));
    }

    // ==================== Internal Helpers ====================

    private Payment withStatus(PaymentStatus newStatus, int newVersion) {
        return new Payment(
                id,
                paymentNo,
                orderId,
                userId,
                amount,
                refundedAmount,
                paymentMethod,
                newStatus,
                transactionId,
                refundReason,
                refundTime,
                attach,
                createTime,
                LocalDateTime.now(),
                newVersion);
    }

    private Payment withRefundResult(
            PaymentStatus newStatus, Money newRefundedAmount, String reason, LocalDateTime time, int newVersion) {
        return new Payment(
                id,
                paymentNo,
                orderId,
                userId,
                amount,
                newRefundedAmount,
                paymentMethod,
                newStatus,
                transactionId,
                reason,
                time,
                attach,
                createTime,
                LocalDateTime.now(),
                newVersion);
    }

    private Payment withSuccess(String transactionId) {
        return new Payment(
                id,
                paymentNo,
                orderId,
                userId,
                amount,
                refundedAmount,
                paymentMethod,
                PaymentStatus.SUCCESS,
                transactionId,
                refundReason,
                refundTime,
                attach,
                createTime,
                LocalDateTime.now(),
                nextVersion());
    }

    private void validateRefundAmount(BigDecimal refundAmount) {
        Money refundMoney = Money.of(refundAmount);
        if (!refundMoney.isLessThanOrEqualTo(this.amount)) {
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

    public String id() {
        return id;
    }

    public String paymentNo() {
        return paymentNo;
    }

    public String orderId() {
        return orderId;
    }

    public String userId() {
        return userId;
    }

    public BigDecimal amount() {
        return amount.value();
    }

    public BigDecimal refundedAmount() {
        return refundedAmount.value();
    }

    public PaymentMethod paymentMethod() {
        return paymentMethod;
    }

    public PaymentStatus status() {
        return status;
    }

    public String transactionId() {
        return transactionId;
    }

    public String refundReason() {
        return refundReason;
    }

    public LocalDateTime refundTime() {
        return refundTime;
    }

    public String attach() {
        return attach;
    }

    public LocalDateTime createTime() {
        return createTime;
    }

    public LocalDateTime updateTime() {
        return updateTime;
    }

    public int version() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Payment other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Payment{id=" + id + ", paymentNo=" + paymentNo + ", status=" + status + "}";
    }
}
