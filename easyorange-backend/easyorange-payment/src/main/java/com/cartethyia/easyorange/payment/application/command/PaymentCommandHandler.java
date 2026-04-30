package com.cartethyia.easyorange.payment.application.command;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import com.cartethyia.easyorange.payment.domain.event.PaymentCreatedEvent;
import com.cartethyia.easyorange.payment.domain.factory.PaymentFactory;
import com.cartethyia.easyorange.payment.domain.gateway.PaymentGateway;
import com.cartethyia.easyorange.payment.domain.repository.PaymentRepository;
import com.cartethyia.easyorange.payment.domain.gateway.PaymentResult;
import com.cartethyia.easyorange.payment.domain.gateway.RefundResult;
import com.cartethyia.easyorange.payment.enums.PaymentResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCommandHandler {

    private final PaymentRepository paymentRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final PaymentGateway paymentGateway;

    @Transactional(rollbackFor = Exception.class)
    public Long handle(CreatePaymentCommand command) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        PaymentAggregate aggregate = PaymentFactory.create(
                command.getOrderId(),
                userId,
                command.getAmount(),
                command.getPaymentMethod(),
                command.getAttach()
        );

        PaymentCreatedEvent event = PaymentAggregate.publishCreatedEvent(aggregate);

        paymentRepository.save(aggregate);
        domainEventPublisher.publish(event);

        log.info("支付创建成功 paymentId={} paymentNo={}", aggregate.id(), aggregate.paymentNo());
        return aggregate.id();
    }

    public void handle(PayCommand command) {
        Long paymentId = preparePayPhase1(command.getPaymentNo());

        try {
            PaymentResult result = invokePayGateway(paymentId);
            confirmPayPhase2(paymentId, result);
        } catch (Exception e) {
            log.error("支付网关调用失败，回退支付状态 paymentNo={}", command.getPaymentNo(), e);
            rollbackPayStatus(paymentId);
            throw e;
        }

        log.info("支付完成 paymentNo={} transactionId={}", command.getPaymentNo(), command.getTransactionId());
    }

    @Transactional(rollbackFor = Exception.class)
    public Long preparePayPhase1(String paymentNo) {
        PaymentAggregate aggregate = paymentRepository.findByPaymentNo(paymentNo)
                .orElseThrow(() -> BusinessException.of(PaymentResultCode.PAYMENT_NOT_FOUND));

        aggregate.preparePay();
        paymentRepository.update(aggregate);

        log.info("支付预处理完成 paymentId={} status=PAYING", aggregate.id());
        return aggregate.id();
    }

    public PaymentResult invokePayGateway(Long paymentId) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(() -> BusinessException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
        return paymentGateway.pay(aggregate);
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmPayPhase2(Long paymentId, PaymentResult result) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(() -> BusinessException.of(PaymentResultCode.PAYMENT_NOT_FOUND));

        BaseDomainEvent event = aggregate.confirmPay(result);

        paymentRepository.update(aggregate);
        domainEventPublisher.publish(event);
    }

    public void handle(RefundPaymentCommand command) {
        BigDecimal refundAmount = command.getRefundAmount();

        Long paymentId = prepareRefundPhase1(command.getPaymentId(), refundAmount);

        try {
            RefundResult result = invokeRefundGateway(paymentId, refundAmount);
            confirmRefundPhase2(paymentId, result, refundAmount);
        } catch (Exception e) {
            log.error("退款网关调用失败，回退退款状态 paymentId={}", command.getPaymentId(), e);
            rollbackRefundStatus(paymentId);
            throw e;
        }

        log.info("支付退款完成 paymentId={} reason={}", command.getPaymentId(), command.getRefundReason());
    }

    @Transactional(rollbackFor = Exception.class)
    public Long prepareRefundPhase1(Long paymentId, BigDecimal refundAmount) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(() -> BusinessException.of(PaymentResultCode.PAYMENT_NOT_FOUND));

        if (refundAmount == null) {
            refundAmount = aggregate.amount();
        }

        aggregate.prepareRefund(refundAmount);
        paymentRepository.update(aggregate);

        log.info("退款预处理完成 paymentId={} status=REFUNDING", paymentId);
        return paymentId;
    }

    public RefundResult invokeRefundGateway(Long paymentId, BigDecimal refundAmount) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(() -> BusinessException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
        return paymentGateway.refund(aggregate, refundAmount);
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmRefundPhase2(Long paymentId, RefundResult result, BigDecimal refundAmount) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(() -> BusinessException.of(PaymentResultCode.PAYMENT_NOT_FOUND));

        BaseDomainEvent event = aggregate.confirmRefund(result, refundAmount);

        paymentRepository.update(aggregate);
        domainEventPublisher.publish(event);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(ClosePaymentCommand command) {
        PaymentAggregate aggregate = paymentRepository.findById(command.getPaymentId())
                .orElseThrow(() -> BusinessException.of(PaymentResultCode.PAYMENT_NOT_FOUND));

        BaseDomainEvent event = aggregate.close();

        paymentRepository.update(aggregate);
        domainEventPublisher.publish(event);

        log.info("支付关闭成功 paymentId={}", command.getPaymentId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void rollbackPayStatus(Long paymentId) {
        try {
            PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> BusinessException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
            aggregate.cancelPay();
            paymentRepository.update(aggregate);
            log.info("支付状态回退成功 paymentId={} status=PENDING", paymentId);
        } catch (Exception e) {
            log.error("支付状态回退失败 paymentId={}", paymentId, e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void rollbackRefundStatus(Long paymentId) {
        try {
            PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> BusinessException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
            aggregate.cancelRefund();
            paymentRepository.update(aggregate);
            log.info("退款状态回退成功 paymentId={} status=SUCCESS", paymentId);
        } catch (Exception e) {
            log.error("退款状态回退失败 paymentId={}", paymentId, e);
        }
    }
}
