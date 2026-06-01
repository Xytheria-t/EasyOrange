package com.cartethyia.easyorange.payment.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.payment.adapter.inbound.web.assembler.PaymentViewAssembler;
import com.cartethyia.easyorange.payment.adapter.inbound.web.request.QueryPaymentRequest;
import com.cartethyia.easyorange.payment.adapter.inbound.web.response.PaymentResponse;
import com.cartethyia.easyorange.payment.application.query.PaymentQueryHandler;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentQueryController {

    private final PaymentQueryHandler queryHandler;
    private final PaymentViewAssembler paymentViewAssembler;

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<PaymentResponse> getById(@PathVariable Long id) {
        PaymentAggregate aggregate = queryHandler.getPaymentById(id);
        return Result.success(paymentViewAssembler.toPaymentResponse(aggregate));
    }

    @GetMapping("/orders/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public Result<PaymentResponse> getByOrderId(@PathVariable Long orderId) {
        PaymentAggregate aggregate = queryHandler.getPaymentByOrderId(orderId);
        return Result.success(paymentViewAssembler.toPaymentResponse(aggregate));
    }

    @GetMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public Result<PaymentStatusResponse> getStatus(@PathVariable Long id) {
        PaymentAggregate aggregate = queryHandler.getPaymentById(id);
        return Result.success(new PaymentStatusResponse(
                aggregate.status().getDesc(),
                com.cartethyia.easyorange.payment.domain.constant.PaymentMethod.getDescByCode(aggregate.paymentMethod()),
                aggregate.updateTime()
        ));
    }

    public record PaymentStatusResponse(String status, String paymentMethod, LocalDateTime payTime) {}

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<PaymentResponse>> getMyPayments(@Valid QueryPaymentRequest request) {
        PageResult<PaymentAggregate> result = queryHandler.getMyPayments(
                request.getStatus(), request.getPageNum(), request.getPageSize());
        return Result.success(paymentViewAssembler.toPageResult(result));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<PaymentResponse>> queryPayments(@Valid QueryPaymentRequest request) {
        PageResult<PaymentAggregate> result = queryHandler.queryPayments(
                request.getUserId(), request.getStatus(), request.getPageNum(), request.getPageSize());
        return Result.success(paymentViewAssembler.toPageResult(result));
    }
}