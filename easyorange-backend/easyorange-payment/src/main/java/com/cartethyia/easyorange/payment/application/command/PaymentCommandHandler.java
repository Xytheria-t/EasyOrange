package com.cartethyia.easyorange.payment.application.command;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.payment.application.lock.DistributedLockWrapper;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import com.cartethyia.easyorange.payment.domain.event.PaymentCreatedEvent;
import com.cartethyia.easyorange.payment.domain.exception.PaymentGatewayException;
import com.cartethyia.easyorange.payment.domain.exception.PaymentNotFoundException;
import com.cartethyia.easyorange.payment.domain.factory.PaymentFactory;
import com.cartethyia.easyorange.payment.domain.gateway.PaymentGateway;
import com.cartethyia.easyorange.payment.domain.repository.PaymentRepository;
import com.cartethyia.easyorange.payment.domain.gateway.PaymentResult;
import com.cartethyia.easyorange.payment.domain.gateway.RefundResult;
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

    private final PaymentRepository paymentRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final PaymentGateway paymentGateway;
    private final DistributedLockWrapper lockWrapper;

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

        paymentRepository.save(aggregate);
        
        saveEventsToOutbox(aggregate);

        log.info("支付创建成功 paymentId={} paymentNo={}", aggregate.id(), aggregate.paymentNo());
        return aggregate.id();
    }

    public void handle(PayCommand command) {
        String lockKey = "payment:pay:" + command.getPaymentNo();
        
        lockWrapper.executeWithLock(lockKey, () -> {
            Long paymentId = null;
            
            try {
                paymentId = preparePayPhase1(command.getPaymentNo());
                final Long finalPaymentId = paymentId;
                
                SagaOrchestrator saga = new SagaOrchestrator();
                
                saga.addStep("invokePayGateway", 
                    () -> {
                        PaymentResult result = invokePayGateway(finalPaymentId);
                        if (!result.isSuccess()) {
                            return SagaStepResult.failure(result.getErrorMessage());
                        }
                        return SagaStepResult.success(result);
                    },
                    () -> rollbackPayStatus(finalPaymentId)
                );
                
                saga.addStep("confirmPayPhase2",
                    () -> {
                        PaymentAggregate aggregate = paymentRepository.findById(finalPaymentId)
                                .orElseThrow(PaymentNotFoundException::of);
                        PaymentResult result = paymentGateway.pay(aggregate);
                        confirmPayPhase2(finalPaymentId, result);
                        return SagaStepResult.success(null);
                    },
                    null
                );
                
                saga.execute();
                
                log.info("支付完成 paymentNo={} paymentId={}", command.getPaymentNo(), paymentId);
            } catch (SagaExecutionException e) {
                log.error("支付Saga执行失败 paymentNo={} step={}", command.getPaymentNo(), e.getFailedStep(), e);
                throw PaymentGatewayException.of("支付失败: " + e.getMessage());
            }
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public Long preparePayPhase1(String paymentNo) {
        PaymentAggregate aggregate = paymentRepository.findByPaymentNo(paymentNo)
                .orElseThrow(PaymentNotFoundException::of);

        aggregate.preparePay();
        paymentRepository.update(aggregate);

        log.info("支付预处理完成 paymentId={} status=PAYING", aggregate.id());
        return aggregate.id();
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

        aggregate.confirmPay(result);

        paymentRepository.update(aggregate);
        
        saveEventsToOutbox(aggregate);
    }

    public void handle(RefundPaymentCommand command) {
        String lockKey = "payment:refund:" + command.getPaymentId();
        
        lockWrapper.executeWithLock(lockKey, () -> {
            BigDecimal refundAmount = command.getRefundAmount();
            Long paymentId = command.getPaymentId();

            try {
                prepareRefundPhase1(paymentId, refundAmount);
                
                SagaOrchestrator saga = new SagaOrchestrator();
                
                saga.addStep("invokeRefundGateway",
                    () -> {
                        RefundResult result = invokeRefundGateway(paymentId, refundAmount);
                        if (!result.isSuccess()) {
                            return SagaStepResult.failure(result.getErrorMessage());
                        }
                        return SagaStepResult.success(result);
                    },
                    () -> rollbackRefundStatus(paymentId)
                );
                
                saga.addStep("confirmRefundPhase2",
                    () -> {
                        RefundResult result = invokeRefundGateway(paymentId, refundAmount);
                        confirmRefundPhase2(paymentId, result, refundAmount);
                        return SagaStepResult.success(null);
                    },
                    null
                );
                
                saga.execute();
                
                log.info("支付退款完成 paymentId={} reason={}", command.getPaymentId(), command.getRefundReason());
            } catch (SagaExecutionException e) {
                log.error("退款Saga执行失败 paymentId={} step={}", paymentId, e.getFailedStep(), e);
                throw PaymentGatewayException.of("退款失败: " + e.getMessage());
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

        aggregate.prepareRefund(refundAmount);
        paymentRepository.update(aggregate);

        log.info("退款预处理完成 paymentId={} status=REFUNDING", paymentId);
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

        aggregate.confirmRefund(result, refundAmount);

        paymentRepository.update(aggregate);
        
        saveEventsToOutbox(aggregate);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(ClosePaymentCommand command) {
        PaymentAggregate aggregate = paymentRepository.findById(command.getPaymentId())
                .orElseThrow(PaymentNotFoundException::of);

        aggregate.close();

        paymentRepository.update(aggregate);
        
        saveEventsToOutbox(aggregate);

        log.info("支付关闭成功 paymentId={}", command.getPaymentId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void rollbackPayStatus(Long paymentId) {
        try {
            PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                    .orElseThrow(PaymentNotFoundException::of);
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
                    .orElseThrow(PaymentNotFoundException::of);
            aggregate.cancelRefund();
            paymentRepository.update(aggregate);
            log.info("退款状态回退成功 paymentId={} status=SUCCESS", paymentId);
        } catch (Exception e) {
            log.error("退款状态回退失败 paymentId={}", paymentId, e);
        }
    }
    
    private void saveEventsToOutbox(PaymentAggregate aggregate) {
        aggregate.domainEvents().forEach(event -> {
            try {
                domainEventPublisher.publish(event);
            } catch (Exception e) {
                log.error("事件发布失败，已保存到 Outbox eventId={}", event.getEventId(), e);
            }
        });
        aggregate.clearDomainEvents();
    }
}
