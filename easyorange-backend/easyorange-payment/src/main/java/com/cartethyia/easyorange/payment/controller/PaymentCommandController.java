package com.cartethyia.easyorange.payment.controller;

import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.payment.application.command.ClosePaymentCommand;
import com.cartethyia.easyorange.payment.application.command.CreatePaymentCommand;
import com.cartethyia.easyorange.payment.application.command.PaymentCommandHandler;
import com.cartethyia.easyorange.payment.application.command.RefundPaymentCommand;
import com.cartethyia.easyorange.payment.dto.request.PaymentCallback;
import com.cartethyia.easyorange.payment.dto.request.RefundRequest;
import com.cartethyia.easyorange.payment.dto.vo.PaymentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentCommandController {

    private final PaymentCommandHandler commandHandler;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public Result<PaymentVO> createPayment(@Valid @RequestBody CreatePaymentCommand command) {
        Long paymentId = commandHandler.handle(command);
        PaymentVO vo = PaymentVO.builder().id(paymentId).build();
        return Result.success(vo);
    }

    @PostMapping("/callback")
    public Result<Void> paymentCallback(@RequestBody PaymentCallback callback) {
        commandHandler.handle(callback);
        return Result.success();
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> refund(@PathVariable Long id, @Valid @RequestBody RefundRequest request) {
        RefundPaymentCommand command = RefundPaymentCommand.builder()
                .paymentId(id)
                .refundAmount(request.getRefundAmount())
                .refundReason(request.getRefundReason())
                .build();
        commandHandler.handle(command);
        return Result.success();
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> close(@PathVariable Long id) {
        ClosePaymentCommand command = ClosePaymentCommand.builder()
                .paymentId(id)
                .build();
        commandHandler.handle(command);
        return Result.success();
    }
}
