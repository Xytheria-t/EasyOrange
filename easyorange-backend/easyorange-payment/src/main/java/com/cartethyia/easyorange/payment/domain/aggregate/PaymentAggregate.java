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
import com.cartethyia.easyorange.payment.domain.exception.PaymentInvalidStatusException;
import com.cartethyia.easyorange.payment.domain.exception.RefundNotAllowedException;
import com.cartethyia.easyorange.payment.enums.PaymentResultCode;
import com.cartethyia.easyorange.payment.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PaymentAggregate {

    private final Long id;
    private final String paymentNo;
    private final Long orderId;
    private final Long userId;
    private final PaymentAmount amount;
    private PaymentAmount refundedAmount;
    private final Integer paymentMethod;
    private PaymentStatus status;
    private String transactionId;
    private String refundReason;
    private LocalDateTime refundTime;
    private final String attach;
    private final LocalDateTime createTime;
    private final LocalDateTime updateTime;
    private Integer version;
    
    private final List<BaseDomainEvent> domainEvents = new ArrayList<>();

    private PaymentAggregate(Long id, String paymentNo, Long orderId, Long userId,
                             PaymentAmount amount, PaymentAmount refundedAmount, Integer paymentMethod,
                             PaymentStatus status, String transactionId, String refundReason,
                             LocalDateTime refundTime, String attach,
                             LocalDateTime createTime, LocalDateTime updateTime, Integer version) {
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

    public static PaymentAggregate create(Long orderId, Long userId, BigDecimal amount,
                                          Integer paymentMethod, String attach) {
        BizRequire.notNull(orderId, "订单ID不能为空");
        BizRequire.notNull(userId, "用户ID不能为空");
        BizRequire.notNull(amount, "支付金额不能为空");
        BizRequire.requireTrue(amount.compareTo(BigDecimal.ZERO) > 0, "支付金额必须大于0");
        BizRequire.notNull(paymentMethod, "支付方式不能为空");

        Long paymentId = generatePaymentId();
        String paymentNo = generatePaymentNo();
        
        PaymentAmount paymentAmount = PaymentAmount.of(amount);

        PaymentAggregate aggregate = new PaymentAggregate(
                paymentId, paymentNo, orderId, userId,
                paymentAmount, PaymentAmount.zero(), paymentMethod,
                PaymentStatus.PENDING, null, null,
                null, attach, null, null, 0
        );
        
        aggregate.registerEvent(new PaymentCreatedEvent(
                paymentId, paymentNo, orderId, userId, amount, paymentMethod
        ));
        
        return aggregate;
    }

    public void pay(PaymentGateway gateway) {
        if (!PaymentSpecification.canPay(this.status)) {
            throw PaymentInvalidStatusException.of("当前状态不允许支付: " + this.status);
        }

        PaymentResult result = gateway.pay(this);

        if (result.isSuccess()) {
            this.status = PaymentStatus.SUCCESS;
            this.transactionId = result.getTransactionId();
            incrementVersion();
            registerEvent(new PaymentSucceededEvent(this.id, result.getTransactionId()));
        } else {
            this.status = PaymentStatus.FAILED;
            incrementVersion();
            registerEvent(new PaymentFailedEvent(this.id, result.getErrorMessage()));
        }
    }

    public void preparePay() {
        if (!PaymentSpecification.canPay(this.status)) {
            throw PaymentInvalidStatusException.of("当前状态不允许支付: " + this.status);
        }
        this.status = PaymentStatus.PAYING;
        incrementVersion();
    }

    public void cancelPay() {
        if (!PaymentStatus.PAYING.equals(this.status)) {
            throw PaymentInvalidStatusException.of("只有支付中状态可以取消支付");
        }
        this.status = PaymentStatus.PENDING;
        incrementVersion();
    }

    public void confirmPay(PaymentResult result) {
        if (!PaymentStatus.PAYING.equals(this.status)) {
            throw PaymentInvalidStatusException.of("只有支付中状态可以确认支付结果");
        }

        if (result.isSuccess()) {
            this.status = PaymentStatus.SUCCESS;
            this.transactionId = result.getTransactionId();
            incrementVersion();
            registerEvent(new PaymentSucceededEvent(this.id, result.getTransactionId()));
        } else {
            this.status = PaymentStatus.FAILED;
            incrementVersion();
            registerEvent(new PaymentFailedEvent(this.id, result.getErrorMessage()));
        }
    }

    public void refund(BigDecimal refundAmount, PaymentGateway gateway) {
        if (!PaymentSpecification.canRefund(this.status)) {
            throw RefundNotAllowedException.of("当前状态不允许退款: " + this.status);
        }

        PaymentAmount refundAmountVO = PaymentAmount.of(refundAmount);
        if (!refundAmountVO.isLessThanOrEqualTo(this.amount.value())) {
            throw RefundNotAllowedException.of("退款金额不能超过支付金额");
        }

        BigDecimal totalRefunded = this.refundedAmount.value().add(refundAmount);
        if (totalRefunded.compareTo(this.amount.value()) > 0) {
            throw RefundNotAllowedException.of("累计退款金额不能超过支付金额");
        }

        RefundResult result = gateway.refund(this, refundAmount);

        if (result.isSuccess()) {
            this.refundedAmount = this.refundedAmount.add(refundAmount);
            if (this.refundedAmount.isEqualTo(this.amount.value())) {
                this.status = PaymentStatus.REFUNDED;
            } else {
                this.status = PaymentStatus.PARTIALLY_REFUNDED;
            }
            this.refundReason = "退款成功";
            this.refundTime = LocalDateTime.now();
            incrementVersion();
            registerEvent(new PaymentRefundedEvent(this.id, "部分退款: " + refundAmount));
        } else {
            throw BusinessException.of(result.getErrorMessage());
        }
    }

    public void prepareRefund(BigDecimal refundAmount) {
        if (!PaymentSpecification.canRefund(this.status)) {
            throw RefundNotAllowedException.of("当前状态不允许退款: " + this.status);
        }

        PaymentAmount refundAmountVO = PaymentAmount.of(refundAmount);
        if (!refundAmountVO.isLessThanOrEqualTo(this.amount.value())) {
            throw RefundNotAllowedException.of("退款金额不能超过支付金额");
        }

        BigDecimal totalRefunded = this.refundedAmount.value().add(refundAmount);
        if (totalRefunded.compareTo(this.amount.value()) > 0) {
            throw RefundNotAllowedException.of("累计退款金额不能超过支付金额");
        }

        this.status = PaymentStatus.REFUNDING;
        incrementVersion();
    }

    public void cancelRefund() {
        if (!PaymentStatus.REFUNDING.equals(this.status)) {
            throw PaymentInvalidStatusException.of("只有退款中状态可以取消退款");
        }
        this.status = PaymentStatus.SUCCESS;
        incrementVersion();
    }

    public void confirmRefund(RefundResult result, BigDecimal refundAmount) {
        if (!PaymentStatus.REFUNDING.equals(this.status)) {
            throw PaymentInvalidStatusException.of("只有退款中状态可以确认退款结果");
        }

        if (result.isSuccess()) {
            this.refundedAmount = this.refundedAmount.add(refundAmount);
            if (this.refundedAmount.isEqualTo(this.amount.value())) {
                this.status = PaymentStatus.REFUNDED;
            } else {
                this.status = PaymentStatus.PARTIALLY_REFUNDED;
            }
            this.refundReason = "退款成功";
            this.refundTime = LocalDateTime.now();
            incrementVersion();
            registerEvent(new PaymentRefundedEvent(this.id, "部分退款: " + refundAmount));
        } else {
            this.status = PaymentStatus.SUCCESS;
            incrementVersion();
            throw BusinessException.of(result.getErrorMessage());
        }
    }

    public void directRefund(String refundReason) {
        if (!PaymentSpecification.canRefund(this.status)) {
            throw RefundNotAllowedException.of("当前状态不允许退款: " + this.status);
        }

        this.status = PaymentStatus.REFUNDED;
        this.refundedAmount = this.amount;
        this.refundReason = refundReason;
        this.refundTime = LocalDateTime.now();
        incrementVersion();

        registerEvent(new PaymentRefundedEvent(this.id, refundReason));
    }

    public void fail(String reason) {
        if (!PaymentSpecification.canFail(this.status)) {
            throw PaymentInvalidStatusException.of("只有待支付状态可以标记为失败");
        }

        this.status = PaymentStatus.FAILED;
        incrementVersion();

        registerEvent(new PaymentFailedEvent(this.id, reason));
    }

    public void close() {
        if (!PaymentSpecification.canClose(this.status)) {
            throw PaymentInvalidStatusException.of("当前状态不允许关闭: " + this.status);
        }

        this.status = PaymentStatus.CLOSED;
        incrementVersion();

        registerEvent(new PaymentClosedEvent(this.id));
    }

    public static PaymentAggregate reconstruct(Long id, String paymentNo, Long orderId, Long userId,
                                                BigDecimal amount, BigDecimal refundedAmount, Integer paymentMethod,
                                                PaymentStatus status, String transactionId, String refundReason,
                                                LocalDateTime refundTime, String attach,
                                                LocalDateTime createTime, LocalDateTime updateTime, Integer version) {
        return new PaymentAggregate(id, paymentNo, orderId, userId, 
                PaymentAmount.of(amount), PaymentAmount.of(refundedAmount),
                paymentMethod, status, transactionId, refundReason, refundTime, attach, createTime, updateTime, version);
    }

    public Long id() { return id; }
    public String paymentNo() { return paymentNo; }
    public Long orderId() { return orderId; }
    public Long userId() { return userId; }
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
    public Integer version() { return version; }

    private void incrementVersion() {
        this.version++;
    }

    private static Long generatePaymentId() {
        return SnowflakeIdGenerator.getInstance().nextId();
    }

    private static String generatePaymentNo() {
        return "PAY" + SnowflakeIdGenerator.getInstance().nextId();
    }
    
    private void registerEvent(BaseDomainEvent event) {
        this.domainEvents.add(event);
    }
    
    public List<BaseDomainEvent> domainEvents() {
        return Collections.unmodifiableList(this.domainEvents);
    }
    
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
}
