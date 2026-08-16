package com.cartethyia.easyorange.payment.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.payment.adapter.inbound.web.request.MockPaymentRequest;
import com.cartethyia.easyorange.payment.adapter.inbound.web.response.PaymentResponse;
import com.cartethyia.easyorange.payment.application.command.PayCommand;
import com.cartethyia.easyorange.payment.application.command.PaymentCommandHandler;
import com.cartethyia.easyorange.payment.domain.aggregate.Payment;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentCreateSpec;
import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;
import com.cartethyia.easyorange.payment.domain.constant.PaymentResultCode;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
import com.cartethyia.easyorange.payment.domain.exception.PaymentDomainException;
import com.cartethyia.easyorange.payment.domain.repository.PaymentRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@Tag(name = "支付管理", description = "模拟支付回调（开发测试用）")
@RestController
@RequestMapping("/mock-payment")
@Profile("dev")
@RequiredArgsConstructor
public class MockPaymentController {

    private final PaymentRepository paymentRepository;
    private final IdGenerator idGenerator;
    private final PaymentCommandHandler paymentCommandHandler;

    @PostMapping("/create")
    @Transactional(rollbackFor = Exception.class)
    public Result<PaymentResponse> createMockPayment(
            @RequestParam String orderId, @RequestParam String paymentMethod, @RequestParam BigDecimal amount) {
        String paymentId = idGenerator.generateId();
        var spec = new PaymentCreateSpec(paymentId, orderId, "0", amount, PaymentMethod.fromCode(paymentMethod), null);
        var created = Payment.create(spec);

        paymentRepository.save(created.aggregate());

        return Result.success(buildPaymentResponse(created.aggregate()));
    }

    /**
     * 模拟网关成功回调 — 经 {@link PaymentCommandHandler} 走「准备 → 网关 → 确认」两阶段，
     * 成功时经 Outbox 发布 {@code PaymentSucceededEvent}（订单模块消费后置订单 PAID）。
     * <p>
     * 此处不开启事务：两阶段各自持有事务边界（见 {@code PaymentPhaseExecutor}），
     * 控制器外层事务会把两个 phase 合并为跨网关调用的单一事务，违背设计约束。
     */
    @PostMapping("/process")
    public Result<PaymentResponse> processMockPayment(@RequestBody MockPaymentRequest request) {
        Payment aggregate = paymentRepository
                .findById(request.getPaymentId())
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));

        if (Boolean.TRUE.equals(request.getSuccess())) {
            paymentCommandHandler.handle(new PayCommand(aggregate.paymentNo(), "MOCK_TXN_" + System.currentTimeMillis(), null));
        } else {
            var failed = aggregate.fail("模拟支付失败");
            paymentRepository.update(failed.aggregate());
        }

        aggregate = paymentRepository
                .findById(request.getPaymentId())
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
        return Result.success(buildPaymentResponse(aggregate));
    }

    @PostMapping("/success/{paymentId}")
    public Result<PaymentResponse> mockPaymentSuccess(@PathVariable String paymentId) {
        Payment aggregate = paymentRepository
                .findById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
        paymentCommandHandler.handle(new PayCommand(aggregate.paymentNo(), "MOCK_TXN_" + System.currentTimeMillis(), null));

        return Result.success(buildPaymentResponse(paymentRepository
                .findById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND))));
    }

    @PostMapping("/fail/{paymentId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<PaymentResponse> mockPaymentFail(@PathVariable String paymentId) {
        Payment aggregate = paymentRepository
                .findById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
        var failed = aggregate.fail("模拟支付失败");
        paymentRepository.update(failed.aggregate());

        return Result.success(buildPaymentResponse(paymentRepository
                .findById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND))));
    }

    @PostMapping("/refund/{paymentId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> mockRefund(@PathVariable String paymentId, @RequestParam String reason) {
        Payment aggregate = paymentRepository
                .findById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
        var result = aggregate.directRefund(reason);
        paymentRepository.update(result.aggregate());

        return Result.success();
    }

    private PaymentResponse buildPaymentResponse(Payment aggregate) {
        return PaymentResponse.builder()
                .id(aggregate.id())
                .paymentNo(aggregate.paymentNo())
                .orderId(aggregate.orderId())
                .amount(aggregate.amount())
                .paymentMethod(aggregate.paymentMethod().getCode())
                .paymentMethodDesc(
                        PaymentMethod.getDescByCode(aggregate.paymentMethod().getCode()))
                .status(aggregate.status().getCode())
                .statusDesc(PaymentStatus.getDescByCode(aggregate.status().getCode()))
                .transactionId(aggregate.transactionId())
                .createTime(aggregate.createTime())
                .build();
    }
}
