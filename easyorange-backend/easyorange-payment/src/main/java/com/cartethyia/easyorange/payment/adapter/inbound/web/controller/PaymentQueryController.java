package com.cartethyia.easyorange.payment.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.payment.adapter.inbound.web.assembler.PaymentViewAssembler;
import com.cartethyia.easyorange.payment.adapter.inbound.web.request.QueryPaymentRequest;
import com.cartethyia.easyorange.payment.adapter.inbound.web.response.PaymentResponse;
import com.cartethyia.easyorange.payment.application.query.PaymentListQuery;
import com.cartethyia.easyorange.payment.application.query.PaymentQueryHandler;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
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
    public Result<PaymentResponse> getById(@PathVariable String id) {
        PaymentAggregate aggregate = queryHandler.getPaymentById(id);
        return Result.success(paymentViewAssembler.toPaymentResponse(aggregate));
    }

    @GetMapping("/orders/{orderId}")
    public Result<PaymentResponse> getByOrderId(@PathVariable String orderId) {
        PaymentAggregate aggregate = queryHandler.getPaymentByOrderId(orderId);
        return Result.success(paymentViewAssembler.toPaymentResponse(aggregate));
    }

    @GetMapping("/{id}/status")
    public Result<PaymentStatusResponse> getStatus(@PathVariable String id) {
        PaymentAggregate aggregate = queryHandler.getPaymentById(id);
        return Result.success(new PaymentStatusResponse(
                aggregate.status().getDesc(),
                com.cartethyia.easyorange.payment.domain.constant.PaymentMethod.getDescByCode(aggregate.paymentMethod().getCode()),
                aggregate.updateTime()
        ));
    }

    public record PaymentStatusResponse(String status, String paymentMethod, LocalDateTime payTime) {}

    @GetMapping("/my")
    public Result<PageResult<PaymentResponse>> getMyPayments(@Valid QueryPaymentRequest request) {
        PaymentListQuery query = new PaymentListQuery(
                null, resolveStatus(request.getStatus()),
                request.getPageNum(), request.getPageSize());
        PageResult<PaymentAggregate> result = queryHandler.getMyPayments(query);
        return Result.success(paymentViewAssembler.toPageResult(result));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<PaymentResponse>> queryPayments(@Valid QueryPaymentRequest request) {
        PaymentListQuery query = new PaymentListQuery(
                request.getUserId(), resolveStatus(request.getStatus()),
                request.getPageNum(), request.getPageSize());
        PageResult<PaymentAggregate> result = queryHandler.queryPayments(query);
        return Result.success(paymentViewAssembler.toPageResult(result));
    }

    /**
     * 边界层 String code → PaymentStatus 转换，blank 视为 null（查询全部状态）。
     * 非法 code 由 {@link PaymentStatus#fromCode(String)} 抛出 IllegalArgumentException，
     * 由全局异常处理器映射为 400 响应。
     */
    private static PaymentStatus resolveStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return PaymentStatus.fromCode(status);
    }
}
