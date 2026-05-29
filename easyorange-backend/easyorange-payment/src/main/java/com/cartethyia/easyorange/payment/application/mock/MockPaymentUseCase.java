package com.cartethyia.easyorange.payment.application.mock;

import com.cartethyia.easyorange.payment.adapter.inbound.web.request.MockPaymentRequest;
import com.cartethyia.easyorange.payment.adapter.inbound.web.response.PaymentResponse;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import com.cartethyia.easyorange.payment.domain.exception.PaymentNotFoundException;
import com.cartethyia.easyorange.payment.domain.repository.PaymentRepositoryPort;
import com.cartethyia.easyorange.common.util.SnowflakeIdGenerator;
import com.cartethyia.easyorange.payment.domain.port.PaymentResult;
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

    public PaymentResponse createMockPayment(Long orderId, Integer paymentMethod, BigDecimal amount) {
        Long paymentId = SnowflakeIdGenerator.getInstance().nextId();
        PaymentAggregate.PaymentCreatedResult created = PaymentAggregate.create(paymentId, orderId, 0L, amount, paymentMethod, null);

        paymentRepository.save(created.aggregate());

        return PaymentResponse.builder()
                .id(created.aggregate().id())
                .paymentNo(created.aggregate().paymentNo())
                .orderId(orderId)
                .amount(amount)
                .paymentMethod(paymentMethod)
                .paymentMethodDesc(PaymentMethod.getDescByCode(paymentMethod))
                .status(created.aggregate().status().getCode())
                .statusDesc(PaymentStatus.getDescByCode(created.aggregate().status().getCode()))
                .createTime(created.aggregate().createTime())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse processMockPayment(MockPaymentRequest request) {
        PaymentAggregate aggregate = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(PaymentNotFoundException::of);

        if (Boolean.TRUE.equals(request.getSuccess())) {
            PaymentAggregate.PayPreparedResult prepared = aggregate.preparePay();
            PaymentAggregate.PayConfirmedResult confirmed = prepared.aggregate()
                    .confirmPay(PaymentResult.success("MOCK_TXN_" + System.currentTimeMillis()));
            paymentRepository.update(confirmed.aggregate());
        } else {
            PaymentAggregate.FailedResult failed = aggregate.fail("模拟支付失败");
            paymentRepository.update(failed.aggregate());
        }

        aggregate = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(PaymentNotFoundException::of);
        return buildPaymentResponse(aggregate);
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse mockPaymentSuccess(Long paymentId) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(PaymentNotFoundException::of);
        PaymentAggregate.PayPreparedResult prepared = aggregate.preparePay();
        PaymentAggregate.PayConfirmedResult confirmed = prepared.aggregate()
                .confirmPay(PaymentResult.success("MOCK_TXN_" + System.currentTimeMillis()));
        paymentRepository.update(confirmed.aggregate());

        return buildPaymentResponse(paymentRepository.findById(paymentId)
                .orElseThrow(PaymentNotFoundException::of));
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse mockPaymentFail(Long paymentId) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(PaymentNotFoundException::of);
        PaymentAggregate.FailedResult failed = aggregate.fail("模拟支付失败");
        paymentRepository.update(failed.aggregate());

        return buildPaymentResponse(paymentRepository.findById(paymentId)
                .orElseThrow(PaymentNotFoundException::of));
    }

    @Transactional(rollbackFor = Exception.class)
    public void mockRefund(Long paymentId, String reason) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(PaymentNotFoundException::of);
        PaymentAggregate.DirectRefundResult result = aggregate.directRefund(reason);
        paymentRepository.update(result.aggregate());
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