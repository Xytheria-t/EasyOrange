package com.cartethyia.easyorange.payment.adapter.inbound.web;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.payment.adapter.inbound.web.request.QueryPaymentRequest;
import com.cartethyia.easyorange.payment.adapter.inbound.web.response.PaymentResponse;
import com.cartethyia.easyorange.payment.application.query.PaymentQuery;
import com.cartethyia.easyorange.payment.application.query.PaymentQueryHandler;
import com.cartethyia.easyorange.payment.application.query.PaymentView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentQueryController {

    private final PaymentQueryHandler queryHandler;

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<PaymentResponse> getById(@PathVariable Long id) {
        PaymentView view = queryHandler.getPaymentById(id);
        return Result.success(toPaymentResponse(view));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<PaymentResponse>> getMyPayments(@Valid QueryPaymentRequest request) {
        PaymentQuery query = toQuery(request);
        PageResult<PaymentView> result = queryHandler.getMyPayments(query);
        return Result.success(toPageResponse(result));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<PaymentResponse>> queryPayments(@Valid QueryPaymentRequest request) {
        PaymentQuery query = toQuery(request);
        PageResult<PaymentView> result = queryHandler.queryPayments(query);
        return Result.success(toPageResponse(result));
    }

    private PaymentQuery toQuery(QueryPaymentRequest request) {
        return PaymentQuery.builder()
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .status(request.getStatus())
                .paymentMethod(request.getPaymentMethod())
                .pageNum(request.getPageNum())
                .pageSize(request.getPageSize())
                .build();
    }

    private PaymentResponse toPaymentResponse(PaymentView view) {
        return PaymentResponse.builder()
                .id(view.getId())
                .paymentNo(view.getPaymentNo())
                .orderId(view.getOrderId())
                .userId(view.getUserId())
                .amount(view.getAmount())
                .paymentMethod(view.getPaymentMethod())
                .paymentMethodDesc(view.getPaymentMethodDesc())
                .status(view.getStatus())
                .statusDesc(view.getStatusDesc())
                .transactionId(view.getTransactionId())
                .refundReason(view.getRefundReason())
                .refundTime(view.getRefundTime())
                .createTime(view.getCreateTime())
                .updateTime(view.getUpdateTime())
                .build();
    }

    private PageResult<PaymentResponse> toPageResponse(PageResult<PaymentView> result) {
        return PageResult.of(
                result.records().stream().map(this::toPaymentResponse).toList(),
                result.total(),
                result.current(),
                result.size()
        );
    }
}
