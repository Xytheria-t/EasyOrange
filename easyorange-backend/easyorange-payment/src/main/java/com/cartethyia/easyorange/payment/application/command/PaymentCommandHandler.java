package com.cartethyia.easyorange.payment.application.command;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.util.SnowflakeIdGenerator;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.payment.application.lock.DistributedLockWrapper;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import com.cartethyia.easyorange.payment.domain.event.CompensationFailedAlertEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentCreatedEvent;
import com.cartethyia.easyorange.payment.domain.exception.PaymentGatewayException;
import com.cartethyia.easyorange.payment.domain.exception.PaymentNotFoundException;
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

    @Transactional(rollbackFor = Exception.class)
    public Long handle(CreatePaymentCommand command) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        Long paymentId = SnowflakeIdGenerator.getInstance().nextId();
        PaymentAggregate.PaymentCreatedResult result = PaymentAggregate.create(paymentId, command.getOrderId(), userId, command.getAmount(), command.getPaymentMethod(), command.getAttach());

        paymentRepository.save(result.aggregate());
        domainEventPublisher.publish(result.event());

        return result.aggregate().id();
    }

    public void handle(PayCommand command) {
        String lockKey = "payment:pay:" + command.getPaymentNo();

        lockWrapper.executeWithLock(lockKey, () -> {
            final Long paymentId = preparePayPhase1(command.getPaymentNo());
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
                log.error("支付Saga执行失败 paymentNo={} step={}", command.getPaymentNo(), e.getFailedStep(), e);
                throw PaymentGatewayException.of("支付失败: " + e.getMessage());
            } catch (SagaCompensationFailedException e) {
                log.error("支付Saga补偿失败 paymentNo={} paymentId={}", command.getPaymentNo(), paymentId, e);
                domainEventPublisher.publish(new CompensationFailedAlertEvent(
                    paymentId,
                    "pay",
                    e.getMessage(),
                    e.getFailures().toString()
                ));
                throw PaymentGatewayException.of("支付失败且补偿失败，请联系客服处理: " + e.getMessage());
            }
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public Long preparePayPhase1(String paymentNo) {
        PaymentAggregate aggregate = paymentRepository.findByPaymentNo(paymentNo)
                .orElseThrow(PaymentNotFoundException::of);

        PaymentAggregate.PayPreparedResult result = aggregate.preparePay();
        paymentRepository.update(result.aggregate());

        return result.aggregate().id();
    }

    public PaymentResult invokePayGateway(Long paymentId) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(PaymentNotFoundException::of);
        return paymentGateway.pay(aggregate);
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmPayPhase2(Long paymentId, PaymentResult result) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(PaymentNotFoundException::of);

        PaymentAggregate.PayConfirmedResult confirmed = aggregate.confirmPay(result);
        paymentRepository.update(confirmed.aggregate());
        domainEventPublisher.publish(confirmed.event());
    }

    public void handle(RefundPaymentCommand command) {
        String lockKey = "payment:refund:" + command.getPaymentId();

        lockWrapper.executeWithLock(lockKey, () -> {
            BigDecimal refundAmount = command.getRefundAmount();
            Long paymentId = command.getPaymentId();

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
                throw PaymentGatewayException.of("退款失败: " + e.getMessage());
            } catch (SagaCompensationFailedException e) {
                log.error("退款Saga补偿失败 paymentId={}", paymentId, e);
                domainEventPublisher.publish(new CompensationFailedAlertEvent(
                    paymentId,
                    "refund",
                    e.getMessage(),
                    e.getFailures().toString()
                ));
                throw PaymentGatewayException.of("退款失败且补偿失败，请联系客服处理: " + e.getMessage());
            }
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public Long prepareRefundPhase1(Long paymentId, BigDecimal refundAmount) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(PaymentNotFoundException::of);

        if (refundAmount == null) {
            refundAmount = aggregate.amount();
        }

        PaymentAggregate.RefundPreparedResult result = aggregate.prepareRefund(refundAmount);
        paymentRepository.update(result.aggregate());

        return paymentId;
    }

    public RefundResult invokeRefundGateway(Long paymentId, BigDecimal refundAmount) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(PaymentNotFoundException::of);
        return paymentGateway.refund(aggregate, refundAmount);
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmRefundPhase2(Long paymentId, RefundResult result, BigDecimal refundAmount) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(PaymentNotFoundException::of);

        PaymentAggregate.RefundConfirmedResult confirmed = aggregate.confirmRefund(result, refundAmount);
        paymentRepository.update(confirmed.aggregate());
        domainEventPublisher.publish(confirmed.event());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(ClosePaymentCommand command) {
        PaymentAggregate aggregate = paymentRepository.findById(command.getPaymentId())
                .orElseThrow(PaymentNotFoundException::of);

        PaymentAggregate.ClosedResult result = aggregate.close();
        paymentRepository.update(result.aggregate());
        domainEventPublisher.publish(result.event());
    }

    @Transactional(rollbackFor = Exception.class)
    public void rollbackPayStatus(Long paymentId) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(PaymentNotFoundException::of);
        PaymentAggregate.CancelPayResult result = aggregate.cancelPay();
        paymentRepository.update(result.aggregate());
    }

    @Transactional(rollbackFor = Exception.class)
    public void rollbackRefundStatus(Long paymentId) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(PaymentNotFoundException::of);
        PaymentAggregate.CancelRefundResult result = aggregate.cancelRefund();
        paymentRepository.update(result.aggregate());
    }
}