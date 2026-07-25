package com.cartethyia.easyorange.payment.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.common.result.Result;
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
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/mock-payment")
@Profile("dev")
@RequiredArgsConstructor
public class MockPaymentController {

    private final PaymentRepositoryPort paymentRepository;
    private final IdGenerator idGenerator;

    @PostMapping("/create")
    @Transactional(rollbackFor = Exception.class)
    public Result<PaymentResponse> createMockPayment(
            @RequestParam String orderId,
            @RequestParam String paymentMethod,
            @RequestParam BigDecimal amount) {
        String paymentId = idGenerator.generateId();
        var spec = new PaymentCreateSpec(paymentId, orderId, "0", amount,
                PaymentMethod.fromCode(paymentMethod), null);
        var created = PaymentAggregate.create(spec);

        paymentRepository.save(created.aggregate());

        return Result.success(buildPaymentResponse(created.aggregate()));
    }

    @PostMapping("/process")
    @Transactional(rollbackFor = Exception.class)
    public Result<PaymentResponse> processMockPayment(@RequestBody MockPaymentRequest request) {
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

        aggregate = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
        return Result.success(buildPaymentResponse(aggregate));
    }

    @PostMapping("/success/{paymentId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<PaymentResponse> mockPaymentSuccess(@PathVariable String paymentId) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
        PaymentAggregate prepared = aggregate.preparePay();
        var confirmed = prepared.confirmPay(PaymentResult.success("MOCK_TXN_" + System.currentTimeMillis()));
        paymentRepository.update(confirmed.aggregate());

        return Result.success(buildPaymentResponse(paymentRepository.findById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND))));
    }

    @PostMapping("/fail/{paymentId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<PaymentResponse> mockPaymentFail(@PathVariable String paymentId) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
        var failed = aggregate.fail("模拟支付失败");
        paymentRepository.update(failed.aggregate());

        return Result.success(buildPaymentResponse(paymentRepository.findById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND))));
    }

    @PostMapping("/refund/{paymentId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> mockRefund(@PathVariable String paymentId, @RequestParam String reason) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
        var result = aggregate.directRefund(reason);
        paymentRepository.update(result.aggregate());

        return Result.success();
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
