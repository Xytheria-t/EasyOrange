package com.cartethyia.easyorange.payment.application.mock;

import com.cartethyia.easyorange.payment.adapter.inbound.web.request.MockPaymentRequest;
import com.cartethyia.easyorange.payment.adapter.inbound.web.response.PaymentResponse;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentCreateSpec;
import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;
import com.cartethyia.easyorange.payment.domain.constant.PaymentResultCode;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
import com.cartethyia.easyorange.payment.domain.exception.PaymentDomainException;
import com.cartethyia.easyorange.payment.domain.port.PaymentResult;
import com.cartethyia.easyorange.payment.domain.repository.PaymentRepositoryPort;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class MockPaymentUseCase {

    private final PaymentRepositoryPort paymentRepository;
    private final IdGenerator idGenerator;

    public PaymentResponse createMockPayment(String orderId, String paymentMethod, BigDecimal amount) {
        String paymentId = idGenerator.generateId();
        var spec = new PaymentCreateSpec(paymentId, orderId, "0", amount,
                PaymentMethod.fromCode(paymentMethod), null);
        var created = PaymentAggregate.create(spec);

        paymentRepository.save(created.aggregate());

        return buildPaymentResponse(created.aggregate());
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse processMockPayment(MockPaymentRequest request) {
        PaymentAggregate aggregate = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));

        if (Boolean.TRUE.equals(request.getSuccess())) {
            PaymentAggregate prepared = aggregate.preparePay();
            var confirmed = prepared.confirmPay(PaymentResult.success("MOCK_TXN_" + System.currentTimeMillis()));
            paymentRepository.update(confirmed.aggregate());
        } else {
            var failed = aggregate.fail("模拟支付失败");
            paymentRepository.update(failed.aggregate());
        }

        return buildPaymentResponse(paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND)));
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse mockPaymentSuccess(String paymentId) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
        PaymentAggregate prepared = aggregate.preparePay();
        var confirmed = prepared.confirmPay(PaymentResult.success("MOCK_TXN_" + System.currentTimeMillis()));
        paymentRepository.update(confirmed.aggregate());

        return buildPaymentResponse(paymentRepository.findById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND)));
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse mockPaymentFail(String paymentId) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
        var failed = aggregate.fail("模拟支付失败");
        paymentRepository.update(failed.aggregate());

        return buildPaymentResponse(paymentRepository.findById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND)));
    }

    @Transactional(rollbackFor = Exception.class)
    public void mockRefund(String paymentId, String reason) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
        var result = aggregate.directRefund(reason);
        paymentRepository.update(result.aggregate());
    }

    private PaymentResponse buildPaymentResponse(PaymentAggregate aggregate) {
        return PaymentResponse.builder()
                .id(aggregate.id())
                .paymentNo(aggregate.paymentNo())
                .orderId(aggregate.orderId())
                .amount(aggregate.amount())
                .paymentMethod(aggregate.paymentMethod().getCode())
                .paymentMethodDesc(PaymentMethod.getDescByCode(aggregate.paymentMethod().getCode()))
                .status(aggregate.status().getCode())
                .statusDesc(PaymentStatus.getDescByCode(aggregate.status().getCode()))
                .transactionId(aggregate.transactionId())
                .createTime(aggregate.createTime())
                .build();
    }
}
