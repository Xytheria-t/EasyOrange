package com.cartethyia.easyorange.payment.application.command;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.util.SecurityContextUtil;
import com.cartethyia.easyorange.payment.application.factory.PaymentStrategyFactory;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import com.cartethyia.easyorange.payment.domain.event.PaymentCreatedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentRefundedEvent;
import com.cartethyia.easyorange.payment.domain.repository.PaymentRepository;
import com.cartethyia.easyorange.payment.domain.strategy.PaymentStrategy;
import com.cartethyia.easyorange.payment.dto.request.PaymentCallback;
import com.cartethyia.easyorange.payment.entity.Payment;
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
    private final PaymentStrategyFactory paymentStrategyFactory;

    @Transactional(rollbackFor = Exception.class)
    public Long handle(CreatePaymentCommand command) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        PaymentCreatedEvent event = PaymentAggregate.create(
                command.getOrderId(),
                userId,
                command.getAmount(),
                command.getPaymentMethod(),
                command.getAttach()
        );

        PaymentAggregate aggregate = PaymentAggregate.builder()
                .id(event.getPaymentId())
                .paymentNo(event.getPaymentNo())
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .amount(event.getAmount())
                .paymentMethod(event.getPaymentMethod())
                .build();

        paymentRepository.save(aggregate.toEntity());
        domainEventPublisher.publish(event);

        log.info("支付创建成功 paymentId={} paymentNo={}", event.getPaymentId(), event.getPaymentNo());
        return event.getPaymentId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(RefundPaymentCommand command) {
        Payment payment = paymentRepository.findById(command.getPaymentId())
                .orElseThrow(() -> BusinessException.of(PaymentResultCode.PAYMENT_NOT_FOUND));

        PaymentAggregate aggregate = PaymentAggregate.fromEntity(payment);
        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(payment.getPaymentMethod());

        BigDecimal refundAmount = command.getRefundAmount();
        if (refundAmount == null) {
            refundAmount = payment.getAmount();
        }

        BaseDomainEvent event = aggregate.refund(refundAmount, strategy);

        paymentRepository.update(aggregate.toEntity());
        domainEventPublisher.publish(event);

        log.info("支付退款成功 paymentId={} reason={}", command.getPaymentId(), command.getRefundReason());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(PayCommand command) {
        Payment payment = paymentRepository.findByPaymentNo(command.getPaymentNo())
                .orElseThrow(() -> BusinessException.of(PaymentResultCode.PAYMENT_NOT_FOUND));

        PaymentAggregate aggregate = PaymentAggregate.fromEntity(payment);
        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(payment.getPaymentMethod());
        BaseDomainEvent event = aggregate.pay(strategy);

        if (command.getAttach() != null) {
            aggregate.setAttach(command.getAttach());
        }

        paymentRepository.update(aggregate.toEntity());
        domainEventPublisher.publish(event);

        log.info("支付成功 paymentNo={} transactionId={}", command.getPaymentNo(), command.getTransactionId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(PaymentCallback callback) {
        PayCommand command = PayCommand.builder()
                .paymentNo(callback.getPaymentNo())
                .transactionId(callback.getTransactionId())
                .attach(callback.getAttach())
                .build();
        handle(command);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(ClosePaymentCommand command) {
        Payment payment = paymentRepository.findById(command.getPaymentId())
                .orElseThrow(() -> BusinessException.of(PaymentResultCode.PAYMENT_NOT_FOUND));

        PaymentAggregate aggregate = PaymentAggregate.fromEntity(payment);
        aggregate.close();

        paymentRepository.update(aggregate.toEntity());

        log.info("支付关闭成功 paymentId={}", command.getPaymentId());
    }
}
