package com.cartethyia.easyorange.payment.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.annotation.Idempotent;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.payment.adapter.inbound.web.assembler.PaymentCommandMapper;
import com.cartethyia.easyorange.payment.adapter.inbound.web.request.CreatePaymentRequest;
import com.cartethyia.easyorange.payment.adapter.inbound.web.request.PaymentCallback;
import com.cartethyia.easyorange.payment.adapter.inbound.web.request.RefundRequest;
import com.cartethyia.easyorange.payment.adapter.inbound.web.response.PaymentResponse;
import com.cartethyia.easyorange.payment.application.command.PaymentCommandHandler;
import com.cartethyia.easyorange.payment.domain.port.CallbackSignatureVerifierPort;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "支付管理", description = "支付发起/确认/退款")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentCommandController {

    private final PaymentCommandHandler commandHandler;
    private final CallbackSignatureVerifierPort signatureVerifier;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final PaymentCommandMapper paymentCommandMapper;

    @PostMapping
    @Idempotent
    public Result<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        String paymentId = commandHandler.handle(paymentCommandMapper.toCreateCommand(request, null));
        PaymentResponse response = PaymentResponse.builder().id(paymentId).build();
        return Result.success(response);
    }

    @PostMapping("/callback")
    public Result<Void> paymentCallback(@RequestBody PaymentCallback callback) {
        signatureVerifier.verify(callback.getPaymentNo(), callback.getTransactionId(), callback.getSign());
        commandHandler.handle(paymentCommandMapper.toPayCommand(callback));
        return Result.success();
    }

    @PostMapping("/{id}/refund")
    public Result<Void> refund(@PathVariable String id, @Valid @RequestBody RefundRequest request) {
        commandHandler.handle(paymentCommandMapper.toRefundCommand(id, request));
        return Result.success();
    }

    @PostMapping("/{id}/close")
    public Result<Void> close(@PathVariable String id) {
        commandHandler.handle(paymentCommandMapper.toCloseCommand(id));
        return Result.success();
    }
}