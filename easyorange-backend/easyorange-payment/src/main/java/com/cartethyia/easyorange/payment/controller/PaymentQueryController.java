package com.cartethyia.easyorange.payment.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.payment.application.query.PaymentQueryHandler;
import com.cartethyia.easyorange.payment.dto.request.QueryPaymentRequest;
import com.cartethyia.easyorange.payment.dto.vo.PaymentVO;
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
    public Result<PaymentVO> getById(@PathVariable Long id) {
        return Result.success(queryHandler.getPaymentById(id));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<PaymentVO>> getMyPayments(@Valid QueryPaymentRequest request) {
        return Result.success(queryHandler.getMyPayments(request));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<PaymentVO>> queryPayments(@Valid QueryPaymentRequest request) {
        return Result.success(queryHandler.queryPayments(request));
    }
}
