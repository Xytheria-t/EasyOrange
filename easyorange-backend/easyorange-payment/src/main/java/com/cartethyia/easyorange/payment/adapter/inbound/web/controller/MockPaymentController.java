package com.cartethyia.easyorange.payment.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.payment.adapter.inbound.web.request.MockPaymentRequest;
import com.cartethyia.easyorange.payment.adapter.inbound.web.response.PaymentResponse;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
import com.cartethyia.easyorange.payment.domain.exception.PaymentNotFoundException;
import com.cartethyia.easyorange.payment.domain.port.PaymentGatewayPort;
import com.cartethyia.easyorange.payment.domain.repository.PaymentRepositoryPort;
import com.cartethyia.easyorange.payment.domain.port.PaymentResult;
import com.cartethyia.easyorange.common.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/mock-payment")
@Profile("dev")
@RequiredArgsConstructor
public class MockPaymentController {

    private final PaymentRepositoryPort paymentRepository;
    private final PaymentGatewayPort paymentGateway;

    @PostMapping("/create")
    public Result<PaymentResponse> createMockPayment(
            @RequestParam Long orderId,
            @RequestParam Integer paymentMethod,
            @RequestParam BigDecimal amount) {
        Long paymentId = SnowflakeIdGenerator.getInstance().nextId();
        PaymentAggregate.PaymentCreatedResult created = PaymentAggregate.create(paymentId, orderId, 0L, amount, paymentMethod, null);

        paymentRepository.save(created.aggregate());

        return Result.success(PaymentResponse.builder()
                .id(created.aggregate().id())
                .paymentNo(created.aggregate().paymentNo())
                .orderId(orderId)
                .amount(amount)
                .paymentMethod(paymentMethod)
                .paymentMethodDesc(PaymentMethod.getDescByCode(paymentMethod))
                .status(created.aggregate().status().getCode())
                .statusDesc(PaymentStatus.getDescByCode(created.aggregate().status().getCode()))
                .createTime(created.aggregate().createTime())
                .build());
    }

    @PostMapping("/process")
    public Result<PaymentResponse> processMockPayment(@RequestBody MockPaymentRequest request) {
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
        return Result.success(buildPaymentResponse(aggregate));
    }

    @PostMapping("/success/{paymentId}")
    public Result<PaymentResponse> mockPaymentSuccess(@PathVariable Long paymentId) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(PaymentNotFoundException::of);
        PaymentAggregate.PayPreparedResult prepared = aggregate.preparePay();
        PaymentAggregate.PayConfirmedResult confirmed = prepared.aggregate()
                .confirmPay(PaymentResult.success("MOCK_TXN_" + System.currentTimeMillis()));
        paymentRepository.update(confirmed.aggregate());

        return Result.success(buildPaymentResponse(paymentRepository.findById(paymentId)
                .orElseThrow(PaymentNotFoundException::of)));
    }

    @PostMapping("/fail/{paymentId}")
    public Result<PaymentResponse> mockPaymentFail(@PathVariable Long paymentId) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(PaymentNotFoundException::of);
        PaymentAggregate.FailedResult failed = aggregate.fail("模拟支付失败");
        paymentRepository.update(failed.aggregate());

        return Result.success(buildPaymentResponse(paymentRepository.findById(paymentId)
                .orElseThrow(PaymentNotFoundException::of)));
    }

    @PostMapping("/refund/{paymentId}")
    public Result<Void> mockRefund(@PathVariable Long paymentId, @RequestParam String reason) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(PaymentNotFoundException::of);
        PaymentAggregate.DirectRefundResult result = aggregate.directRefund(reason);
        paymentRepository.update(result.aggregate());

        return Result.success();
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