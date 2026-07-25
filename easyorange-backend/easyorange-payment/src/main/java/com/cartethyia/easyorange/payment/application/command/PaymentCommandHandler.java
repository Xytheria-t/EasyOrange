package com.cartethyia.easyorange.payment.application.command;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.payment.application.lock.DistributedLockWrapper;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentCreateSpec;
import com.cartethyia.easyorange.payment.domain.event.CompensationFailedAlertEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentCreatedEvent;
import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;
import com.cartethyia.easyorange.payment.domain.constant.PaymentResultCode;
import com.cartethyia.easyorange.payment.domain.exception.PaymentDomainException;
import com.cartethyia.easyorange.payment.domain.exception.SagaCompensationFailedException;
import com.cartethyia.easyorange.payment.domain.port.PaymentGatewayPort;
import com.cartethyia.easyorange.payment.domain.repository.PaymentRepositoryPort;
import com.cartethyia.easyorange.payment.domain.port.PaymentResult;
import com.cartethyia.easyorange.payment.domain.port.RefundResult;
import com.cartethyia.easyorange.payment.domain.saga.SagaExecutionException;
import com.cartethyia.easyorange.payment.domain.saga.SagaOrchestrator;
import com.cartethyia.easyorange.payment.domain.saga.SagaStepResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
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
        PaymentAggregate.PaymentTransition<PaymentCreatedEvent> result = PaymentAggregate.create(spec);

        paymentRepository.save(result.aggregate());
        domainEventPublisher.publish(result.event());

        return result.aggregate().id();
    }

    public void handle(PayCommand command) {
        String lockKey = "payment:pay:" + command.paymentNo();

        lockWrapper.executeWithLock(lockKey, () -> {
            final String paymentId = preparePayPhase1(command.paymentNo());
            try {
                SagaOrchestrator saga = new SagaOrchestrator();

                saga.addStep("pay",
                    () -> {
                        PaymentResult payResult = invokePayGateway(paymentId);
                        if (payResult.isSuccess()) {
                            confirmPayPhase2(paymentId, payResult);
                            return SagaStepResult.success(payResult);
                        } else {
                            rollbackPayStatus(paymentId);
                            return SagaStepResult.failure(payResult.getErrorMessage());
                        }
                    },
                    () -> rollbackPayStatus(paymentId)
                );

                saga.execute();
            } catch (SagaExecutionException e) {
                log.error("支付Saga执行失败 paymentNo={} step={}", command.paymentNo(), e.getFailedStep(), e);
                throw PaymentDomainException.of(PaymentResultCode.PAYMENT_GATEWAY_ERROR, "支付失败: " + e.getMessage());
            } catch (SagaCompensationFailedException e) {
                log.error("支付Saga补偿失败 paymentNo={} paymentId={}", command.paymentNo(), paymentId, e);
                domainEventPublisher.publish(new CompensationFailedAlertEvent(
                    paymentId,
                    "pay",
                    e.getMessage(),
                    e.getFailures().toString()
                ));
                throw PaymentDomainException.of(PaymentResultCode.PAYMENT_GATEWAY_ERROR, "支付失败且补偿失败，请联系客服处理: " + e.getMessage());
            }
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public String preparePayPhase1(String paymentNo) {
        PaymentAggregate aggregate = paymentRepository.findByPaymentNo(paymentNo)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));

        PaymentAggregate updated = aggregate.preparePay();
        paymentRepository.update(updated);

        return updated.id();
    }

    public PaymentResult invokePayGateway(String paymentId) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
        return paymentGateway.pay(aggregate);
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmPayPhase2(String paymentId, PaymentResult result) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));

        var confirmed = aggregate.confirmPay(result);
        paymentRepository.update(confirmed.aggregate());
        domainEventPublisher.publish(confirmed.event());
    }

    public void handle(RefundPaymentCommand command) {
        String lockKey = "payment:refund:" + command.paymentId();

        lockWrapper.executeWithLock(lockKey, () -> {
            BigDecimal refundAmount = command.refundAmount();
            String paymentId = command.paymentId();

            try {
                prepareRefundPhase1(paymentId, refundAmount);

                SagaOrchestrator saga = new SagaOrchestrator();

                saga.addStep("refund",
                    () -> {
                        RefundResult refundResult = invokeRefundGateway(paymentId, refundAmount);
                        if (refundResult.isSuccess()) {
                            confirmRefundPhase2(paymentId, refundResult, refundAmount);
                            return SagaStepResult.success(refundResult);
                        } else {
                            rollbackRefundStatus(paymentId);
                            return SagaStepResult.failure(refundResult.getErrorMessage());
                        }
                    },
                    () -> rollbackRefundStatus(paymentId)
                );

                saga.execute();
            } catch (SagaExecutionException e) {
                log.error("退款Saga执行失败 paymentId={} step={}", paymentId, e.getFailedStep(), e);
                throw PaymentDomainException.of(PaymentResultCode.PAYMENT_GATEWAY_ERROR, "退款失败: " + e.getMessage());
            } catch (SagaCompensationFailedException e) {
                log.error("退款Saga补偿失败 paymentId={}", paymentId, e);
                domainEventPublisher.publish(new CompensationFailedAlertEvent(
                    paymentId,
                    "refund",
                    e.getMessage(),
                    e.getFailures().toString()
                ));
                throw PaymentDomainException.of(PaymentResultCode.PAYMENT_GATEWAY_ERROR, "退款失败且补偿失败，请联系客服处理: " + e.getMessage());
            }
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public String prepareRefundPhase1(String paymentId, BigDecimal refundAmount) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));

        if (refundAmount == null) {
            refundAmount = aggregate.amount();
        }

        PaymentAggregate updated = aggregate.prepareRefund(refundAmount);
        paymentRepository.update(updated);

        return paymentId;
    }

    public RefundResult invokeRefundGateway(String paymentId, BigDecimal refundAmount) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
        return paymentGateway.refund(aggregate, refundAmount);
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmRefundPhase2(String paymentId, RefundResult result, BigDecimal refundAmount) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));

        var confirmed = aggregate.confirmRefund(result, refundAmount);
        paymentRepository.update(confirmed.aggregate());
        domainEventPublisher.publish(confirmed.event());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(ClosePaymentCommand command) {
        PaymentAggregate aggregate = paymentRepository.findById(command.paymentId())
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));

        var result = aggregate.close();
        paymentRepository.update(result.aggregate());
        domainEventPublisher.publish(result.event());
    }

    @Transactional(rollbackFor = Exception.class)
    public void rollbackPayStatus(String paymentId) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
        PaymentAggregate updated = aggregate.cancelPay();
        paymentRepository.update(updated);
    }

    @Transactional(rollbackFor = Exception.class)
    public void rollbackRefundStatus(String paymentId) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
        PaymentAggregate updated = aggregate.cancelRefund();
        paymentRepository.update(updated);
    }
}
