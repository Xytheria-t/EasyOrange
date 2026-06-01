package com.cartethyia.easyorange.order.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.order.application.query.OrderQueryHandler;
import com.cartethyia.easyorange.order.application.dto.OrderVO;
import com.cartethyia.easyorange.order.adapter.inbound.web.dto.request.QueryOrderRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderQueryController {

    private final OrderQueryHandler queryHandler;

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<OrderVO> getOrderDetail(@PathVariable Long id) {
        OrderVO order = queryHandler.getOrderDetailForOwner(id);
        return Result.success(order);
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<OrderVO>> getMyOrders(@Valid QueryOrderRequest request) {
        var normalized = request.normalized();
        PageResult<OrderVO> orders = queryHandler.getMyOrders(normalized.getStatus(),
                normalized.getPageNum(), normalized.getPageSize());
        return Result.success(orders);
    }

    @GetMapping("/sold")
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<OrderVO>> getSoldOrders(@Valid QueryOrderRequest request) {
        var normalized = request.normalized();
        PageResult<OrderVO> orders = queryHandler.getSoldOrders(normalized.getStatus(),
                normalized.getPageNum(), normalized.getPageSize());
        return Result.success(orders);
    }

    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<OrderVO>> queryOrders(@Valid QueryOrderRequest request) {
        var normalized = request.normalized();
        PageResult<OrderVO> orders = queryHandler.handle(
                normalized.getOrderNo(),
                normalized.getStatus(), normalized.getBuyerId(), normalized.getSellerId(),
                normalized.getPageNum(), normalized.getPageSize());
        return Result.success(orders);
    }
}
