package com.cartethyia.easyorange.payment.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.payment.adapter.inbound.web.assembler.PaymentCommandAssembler;
import com.cartethyia.easyorange.payment.adapter.inbound.web.request.CreatePaymentRequest;
import com.cartethyia.easyorange.payment.adapter.inbound.web.request.PaymentCallback;
import com.cartethyia.easyorange.payment.adapter.inbound.web.request.RefundRequest;
import com.cartethyia.easyorange.payment.adapter.inbound.web.response.PaymentResponse;
import com.cartethyia.easyorange.payment.application.command.PaymentCommandHandler;
import com.cartethyia.easyorange.payment.domain.port.output.CallbackSignatureVerifierPort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentCommandController {

    private final PaymentCommandHandler commandHandler;
    private final CallbackSignatureVerifierPort signatureVerifier;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public Result<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        Long paymentId = commandHandler.handle(PaymentCommandAssembler.toCreateCommand(request, null));
        PaymentResponse response = PaymentResponse.builder().id(paymentId).build();
        return Result.success(response);
    }

    @PostMapping("/callback")
    public Result<Void> paymentCallback(@RequestBody PaymentCallback callback) {
        signatureVerifier.verify(callback.getPaymentNo(), callback.getTransactionId(), callback.getSign());
        commandHandler.handle(PaymentCommandAssembler.toPayCommand(callback));
        return Result.success();
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> refund(@PathVariable Long id, @Valid @RequestBody RefundRequest request) {
        commandHandler.handle(PaymentCommandAssembler.toRefundCommand(id, request));
        return Result.success();
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> close(@PathVariable Long id) {
        commandHandler.handle(PaymentCommandAssembler.toCloseCommand(id));
        return Result.success();
    }
}