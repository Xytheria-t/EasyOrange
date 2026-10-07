package com.cartethyia.easyorange.payment.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.security.AuthUser;
import com.cartethyia.easyorange.payment.adapter.inbound.web.assembler.PaymentCommandMapper;
import com.cartethyia.easyorange.payment.adapter.inbound.web.assembler.PaymentViewAssembler;
import com.cartethyia.easyorange.payment.adapter.inbound.web.request.CreatePaymentRequest;
import com.cartethyia.easyorange.payment.adapter.inbound.web.request.PaymentCallback;
import com.cartethyia.easyorange.payment.adapter.inbound.web.request.RefundRequest;
import com.cartethyia.easyorange.payment.adapter.inbound.web.response.PaymentResponse;
import com.cartethyia.easyorange.payment.application.command.PaymentCommandHandler;
import com.cartethyia.easyorange.payment.domain.port.CallbackSignatureVerifierPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "支付管理", description = "支付发起/确认/退款")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentCommandController {

    private final PaymentCommandHandler commandHandler;
    private final CallbackSignatureVerifierPort signatureVerifier;
    private final PaymentViewAssembler paymentViewAssembler;

    @PostMapping
    @Operation(summary = "创建支付单（初始 PENDING，金额必须大于 0）")
    public Result<PaymentResponse> createPayment(
            @AuthenticationPrincipal AuthUser user, @Valid @RequestBody CreatePaymentRequest request) {
        String paymentId = commandHandler.createPayment(user.userId(), PaymentCommandMapper.toCreateCommand(request));
        return Result.success(paymentViewAssembler.toIdOnlyResponse(paymentId));
    }

    @PostMapping("/callback")
    @Operation(summary = "网关回调：匿名可达，HMAC-SHA256 验签 + 金额比对后确认支付")
    public Result<Void> paymentCallback(@Valid @RequestBody PaymentCallback callback) {
        signatureVerifier.verify(callback.getPaymentNo(), callback.getTransactionId(), callback.getSign());
        commandHandler.processCallback(PaymentCommandMapper.toCallbackCommand(callback));
        return Result.success();
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "支付单本人发起退款（两阶段走网关，累计退款不得超支付金额）")
    public Result<Void> refund(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable String id,
            @Valid @RequestBody RefundRequest request) {
        commandHandler.refundPayment(PaymentCommandMapper.toRefundCommand(id, user.userId(), request));
        return Result.success();
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "本人关闭支付单（仅 PENDING / FAILED 可关）并发关闭事件")
    public Result<Void> close(@AuthenticationPrincipal AuthUser user, @PathVariable String id) {
        commandHandler.closePayment(PaymentCommandMapper.toCloseCommand(id, user.userId()));
        return Result.success();
    }
}
