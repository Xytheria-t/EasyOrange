package com.cartethyia.easyorange.payment.adapter.inbound.web;

import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.payment.adapter.inbound.web.request.MockPaymentRequest;
import com.cartethyia.easyorange.payment.adapter.inbound.web.response.PaymentResponse;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
import com.cartethyia.easyorange.payment.domain.exception.PaymentNotFoundException;
import com.cartethyia.easyorange.payment.domain.port.output.PaymentGatewayPort;
import com.cartethyia.easyorange.payment.domain.port.output.PaymentRepositoryPort;
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
        PaymentAggregate aggregate = com.cartethyia.easyorange.payment.domain.factory.PaymentFactory.create(
                orderId, 0L, amount, paymentMethod, null);

        paymentRepository.save(aggregate);

        return Result.success(PaymentResponse.builder()
                .id(aggregate.id())
                .paymentNo(aggregate.paymentNo())
                .orderId(orderId)
                .amount(amount)
                .paymentMethod(paymentMethod)
                .paymentMethodDesc(PaymentMethod.getDescByCode(paymentMethod))
                .status(aggregate.status().getCode())
                .statusDesc(PaymentStatus.getDescByCode(aggregate.status().getCode()))
                .createTime(aggregate.createTime())
                .build());
    }

    @PostMapping("/process")
    public Result<PaymentResponse> processMockPayment(@RequestBody MockPaymentRequest request) {
        PaymentAggregate aggregate = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(PaymentNotFoundException::of);

        if (Boolean.TRUE.equals(request.getSuccess())) {
            aggregate.pay(paymentGateway);
        } else {
            aggregate.fail("模拟支付失败");
        }
        paymentRepository.update(aggregate);

        return Result.success(buildPaymentResponse(aggregate));
    }

    @PostMapping("/success/{paymentId}")
    public Result<PaymentResponse> mockPaymentSuccess(@PathVariable Long paymentId) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(PaymentNotFoundException::of);
        aggregate.pay(paymentGateway);
        paymentRepository.update(aggregate);

        return Result.success(buildPaymentResponse(aggregate));
    }

    @PostMapping("/fail/{paymentId}")
    public Result<PaymentResponse> mockPaymentFail(@PathVariable Long paymentId) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(PaymentNotFoundException::of);
        aggregate.fail("模拟支付失败");
        paymentRepository.update(aggregate);

        return Result.success(buildPaymentResponse(aggregate));
    }

    @PostMapping("/refund/{paymentId}")
    public Result<Void> mockRefund(@PathVariable Long paymentId, @RequestParam String reason) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .orElseThrow(PaymentNotFoundException::of);
        aggregate.directRefund(reason);
        paymentRepository.update(aggregate);

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
