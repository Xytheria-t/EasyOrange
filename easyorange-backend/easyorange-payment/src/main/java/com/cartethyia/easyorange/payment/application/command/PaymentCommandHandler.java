package com.cartethyia.easyorange.payment.application.command;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.event.Transition;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.payment.application.lock.DistributedLockWrapper;
import com.cartethyia.easyorange.payment.domain.aggregate.Payment;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentCreateSpec;
import com.cartethyia.easyorange.payment.domain.event.PaymentCreatedEvent;
import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;
import com.cartethyia.easyorange.payment.domain.constant.PaymentResultCode;
import com.cartethyia.easyorange.payment.domain.exception.PaymentDomainException;
import com.cartethyia.easyorange.payment.domain.port.PaymentGatewayPort;
import com.cartethyia.easyorange.payment.domain.repository.PaymentRepositoryPort;
import com.cartethyia.easyorange.payment.domain.port.PaymentResult;
import com.cartethyia.easyorange.payment.domain.port.RefundResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentCommandHandler {

    private final PaymentRepositoryPort paymentRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final PaymentGatewayPort paymentGateway;
    private final DistributedLockWrapper lockWrapper;
    private final IdGenerator idGenerator;

    @Transactional(rollbackFor = Exception.class)
    public String handle(CreatePaymentCommand command) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        String paymentId = idGenerator.generateId();
        var spec = new PaymentCreateSpec(paymentId, command.orderId(), userId, command.amount(),
                PaymentMethod.fromCode(command.paymentMethod()), command.attach());
        Transition<Payment, PaymentCreatedEvent> result = Payment.create(spec);

        paymentRepository.save(result.aggregate());
        domainEventPublisher.publish(result.event());

        return result.aggregate().id();
    }

    /**
     * 支付：两阶段（本地事务 + 外部网关）。
     * <p>
     * 单数据库场景下遵循 ADR-0007「拒绝 Saga」——本地事务提供原子性，
     * 外部网关调用无法纳入同一事务，因此用「准备 → 网关 → 确认」顺序两阶段，
     * 网关失败时回退状态，无需跨服务编排。
     */
    public void handle(PayCommand command) {
        String lockKey = "payment:pay:" + command.paymentNo();

        lockWrapper.executeWithLock(lockKey, () -> {
            final String paymentId = preparePayPhase1(command.paymentNo());
            PaymentResult payResult = invokePayGateway(paymentId);
            if (payResult.isSuccess()) {
                confirmPayPhase2(paymentId, payResult);
            } else {
                rollbackPayStatus(paymentId);
            }
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public String preparePayPhase1(String paymentNo) {
        Payment aggregate = paymentRepository.findByPaymentNo(paymentNo)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));

        Payment updated = aggregate.preparePay();
        paymentRepository.update(updated);

        return updated.id();
    }

    public PaymentResult invokePayGateway(String paymentId) {
        Payment aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
        return paymentGateway.pay(aggregate);
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmPayPhase2(String paymentId, PaymentResult result) {
        Payment aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));

        var confirmed = aggregate.confirmPay(result);
        paymentRepository.update(confirmed.aggregate());
        domainEventPublisher.publish(confirmed.event());
    }

    /**
     * 退款：两阶段（本地事务 + 外部网关），与支付一致遵循 ADR-0007 拒绝 Saga。
     */
    public void handle(RefundPaymentCommand command) {
        String lockKey = "payment:refund:" + command.paymentId();

        lockWrapper.executeWithLock(lockKey, () -> {
            BigDecimal refundAmount = command.refundAmount();
            String paymentId = command.paymentId();

            prepareRefundPhase1(paymentId, refundAmount);
            RefundResult refundResult = invokeRefundGateway(paymentId, refundAmount);
            if (refundResult.isSuccess()) {
                confirmRefundPhase2(paymentId, refundResult, refundAmount);
            } else {
                rollbackRefundStatus(paymentId);
            }
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public String prepareRefundPhase1(String paymentId, BigDecimal refundAmount) {
        Payment aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));

        if (refundAmount == null) {
            refundAmount = aggregate.amount();
        }

        Payment updated = aggregate.prepareRefund(refundAmount);
        paymentRepository.update(updated);

        return paymentId;
    }

    public RefundResult invokeRefundGateway(String paymentId, BigDecimal refundAmount) {
        Payment aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
        return paymentGateway.refund(aggregate, refundAmount);
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmRefundPhase2(String paymentId, RefundResult result, BigDecimal refundAmount) {
        Payment aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));

        var confirmed = aggregate.confirmRefund(result, refundAmount);
        paymentRepository.update(confirmed.aggregate());
        domainEventPublisher.publish(confirmed.event());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(ClosePaymentCommand command) {
        Payment aggregate = paymentRepository.findById(command.paymentId())
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));

        var result = aggregate.close();
        paymentRepository.update(result.aggregate());
        domainEventPublisher.publish(result.event());
    }

    @Transactional(rollbackFor = Exception.class)
    public void rollbackPayStatus(String paymentId) {
        Payment aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
        Payment updated = aggregate.cancelPay();
        paymentRepository.update(updated);
    }

    @Transactional(rollbackFor = Exception.class)
    public void rollbackRefundStatus(String paymentId) {
        Payment aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
        Payment updated = aggregate.cancelRefund();
        paymentRepository.update(updated);
    }
}
