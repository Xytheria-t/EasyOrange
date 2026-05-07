package com.cartethyia.easyorange.order.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.order.application.query.OrderQueryHandler;
import com.cartethyia.easyorange.order.application.query.QueryOrderRequest;
import com.cartethyia.easyorange.order.adapter.inbound.web.dto.response.OrderVO;
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
        PageResult<OrderVO> orders = queryHandler.getMyOrders(request);
        return Result.success(orders);
    }

    @GetMapping("/sold")
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<OrderVO>> getSoldOrders(@Valid QueryOrderRequest request) {
        PageResult<OrderVO> orders = queryHandler.getSoldOrders(request);
        return Result.success(orders);
    }

    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<OrderVO>> queryOrders(@Valid QueryOrderRequest request) {
        PageResult<OrderVO> orders = queryHandler.handle(request);
        return Result.success(orders);
    }
}
