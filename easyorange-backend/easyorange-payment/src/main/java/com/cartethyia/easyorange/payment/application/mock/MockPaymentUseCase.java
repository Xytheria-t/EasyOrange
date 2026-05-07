package com.cartethyia.easyorange.payment.application.mock;

import com.cartethyia.easyorange.payment.adapter.inbound.web.request.MockPaymentRequest;
import com.cartethyia.easyorange.payment.adapter.inbound.web.response.PaymentResponse;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import com.cartethyia.easyorange.payment.domain.exception.PaymentNotFoundException;
import com.cartethyia.easyorange.payment.domain.factory.PaymentFactory;
import com.cartethyia.easyorange.payment.domain.port.output.PaymentGatewayPort;
import com.cartethyia.easyorange.payment.domain.port.output.PaymentRepositoryPort;
import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class MockPaymentUseCase {

    private final PaymentRepositoryPort paymentRepository;
    private final PaymentGatewayPort paymentGateway;

    public PaymentResponse createMockPayment(Long orderId, Integer paymentMethod, BigDecimal amount) {
        PaymentAggregate aggregate = PaymentFactory.create(orderId, 0L, amount, paymentMethod, null);

        paymentRepository.save(aggregate);

        return PaymentResponse.builder()
                .id(aggregate.id())
                .paymentNo(aggregate.paymentNo())
                .orderId(orderId)
                .amount(amount)
                .paymentMethod(paymentMethod)
                .paymentMethodDesc(PaymentMethod.getDescByCode(paymentMethod))
                .status(aggregate.status().getCode())
                .statusDesc(PaymentStatus.getDescByCode(aggregate.status().getCode()))
                .createTime(aggregate.createTime())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse processMockPayment(MockPaymentRequest request) {
        PaymentAggregate aggregate = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(PaymentNotFoundException::of);

        if (Boolean.TRUE.equals(request.getSuccess())) {
            aggregate.pay(paymentGateway);
        } else {
            aggregate.fail("模拟支付失败");
        }
        paymentRepository.update(aggregate);

        return buildPaymentResponse(aggregate);
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse mockPaymentSuccess(Long paymentId) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(PaymentNotFoundException::of);
        aggregate.pay(paymentGateway);
        paymentRepository.update(aggregate);

        return buildPaymentResponse(aggregate);
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse mockPaymentFail(Long paymentId) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(PaymentNotFoundException::of);
        aggregate.fail("模拟支付失败");
        paymentRepository.update(aggregate);

        return buildPaymentResponse(aggregate);
    }

    @Transactional(rollbackFor = Exception.class)
    public void mockRefund(Long paymentId, String reason) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(PaymentNotFoundException::of);
        aggregate.directRefund(reason);
        paymentRepository.update(aggregate);
    }

    private PaymentResponse buildPaymentResponse(PaymentAggregate aggregate) {
        return PaymentResponse.builder()
                .id(aggregate.id())
                .paymentNo(aggregate.paymentNo())
                .orderId(aggregate.orderId())
                .amount(aggregate.amount())
                .paymentMethod(aggregate.paymentMethod())
                .paymentMethodDesc(PaymentMethod.getDescByCode(aggregate.paymentMethod()))
                .status(aggregate.status().getCode())
                .statusDesc(PaymentStatus.getDescByCode(aggregate.status().getCode()))
                .transactionId(aggregate.transactionId())
                .createTime(aggregate.createTime())
                .build();
    }
}
