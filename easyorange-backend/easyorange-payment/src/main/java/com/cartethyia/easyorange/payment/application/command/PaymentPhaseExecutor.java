package com.cartethyia.easyorange.payment.application.command;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.payment.domain.aggregate.Payment;
import com.cartethyia.easyorange.payment.domain.constant.PaymentResultCode;
import com.cartethyia.easyorange.payment.domain.exception.PaymentDomainException;
import com.cartethyia.easyorange.payment.domain.port.PaymentGatewayPort;
import com.cartethyia.easyorange.payment.domain.port.PaymentResult;
import com.cartethyia.easyorange.payment.domain.port.RefundResult;
import com.cartethyia.easyorange.payment.domain.repository.PaymentRepositoryPort;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 支付/退款两阶段执行器 — 独立 Bean 保证 {@code @Transactional} 经 Spring 代理生效。
 * <p>
 * 每个 phase（准备/确认/回退）是独立的事务边界：「聚合更新 + Outbox 事件写入」在同一
 * 数据库事务内原子提交（Modulith {@code EVENT_PUBLICATION} 依赖调用方活动事务）。
 * 若这些方法被合并回编排类成为同类自调用，Spring 代理会被绕过、事务静默失效，
 * 状态更新与事件发布将退化为两次独立提交（崩溃窗口内支付成功但事件丢失），
 * 该结构不变量由 {@code PaymentTransactionBoundaryTest} 守卫。
 * <p>
 * 两阶段编排（而非 Saga）的原因与顺序见 {@link PaymentCommandHandler} 与 ADR-0007。
 */
@Service
@RequiredArgsConstructor
public class PaymentPhaseExecutor {

    private final PaymentRepositoryPort paymentRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final PaymentGatewayPort paymentGateway;

    /**
     * 准备支付：PENDING → PAYING（中间态，不发事件）。
     */
    @Transactional(rollbackFor = Exception.class)
    public String preparePayPhase1(String paymentNo) {
        Payment aggregate = paymentRepository
                .findByPaymentNo(paymentNo)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));

        Payment updated = aggregate.preparePay();
        paymentRepository.update(updated);

        return updated.id();
    }

    /**
     * 调用支付网关（无 DB 变更，不开启事务，避免挂住连接跨外部调用）。
     */
    public PaymentResult invokePayGateway(String paymentId) {
        return paymentGateway.pay(findRequired(paymentId));
    }

    /**
     * 确认支付结果：根据网关结果 PAYING → SUCCESS / FAILED，并发布事件。
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmPayPhase2(String paymentId, PaymentResult result) {
        var confirmed = findRequired(paymentId).confirmPay(result);
        paymentRepository.update(confirmed.aggregate());
        domainEventPublisher.publish(confirmed.event());
    }

    /**
     * 回退支付状态：PAYING → PENDING（中间态，不发事件）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void rollbackPayStatus(String paymentId) {
        Payment updated = findRequired(paymentId).cancelPay();
        paymentRepository.update(updated);
    }

    /**
     * 准备退款：SUCCESS → REFUNDING（中间态，不发事件）。
     *
     * @param refundAmount 退款金额，为空时默认全额
     */
    @Transactional(rollbackFor = Exception.class)
    public void prepareRefundPhase1(String paymentId, BigDecimal refundAmount) {
        Payment aggregate = findRequired(paymentId);
        BigDecimal amount = refundAmount != null ? refundAmount : aggregate.amount();

        Payment updated = aggregate.prepareRefund(amount);
        paymentRepository.update(updated);
    }

    /**
     * 调用退款网关（无 DB 变更，不开启事务）。
     */
    public RefundResult invokeRefundGateway(String paymentId, BigDecimal refundAmount) {
        return paymentGateway.refund(findRequired(paymentId), refundAmount);
    }

    /**
     * 确认退款结果：REFUNDING → REFUNDED / PARTIALLY_REFUNDED，并发布事件。
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmRefundPhase2(String paymentId, RefundResult result, BigDecimal refundAmount) {
        var confirmed = findRequired(paymentId).confirmRefund(result, refundAmount);
        paymentRepository.update(confirmed.aggregate());
        domainEventPublisher.publish(confirmed.event());
    }

    /**
     * 回退退款状态：REFUNDING → SUCCESS（中间态，不发事件）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void rollbackRefundStatus(String paymentId) {
        Payment updated = findRequired(paymentId).cancelRefund();
        paymentRepository.update(updated);
    }

    private Payment findRequired(String paymentId) {
        return paymentRepository
                .findById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
    }
}
