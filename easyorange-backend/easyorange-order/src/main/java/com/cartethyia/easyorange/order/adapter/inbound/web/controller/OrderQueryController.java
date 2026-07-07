package com.cartethyia.easyorange.order.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.order.application.query.OrderQueryHandler;
import com.cartethyia.easyorange.order.application.dto.OrderVO;
import com.cartethyia.easyorange.order.adapter.inbound.web.dto.request.QueryOrderRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderQueryController {

    private final OrderQueryHandler queryHandler;

    @GetMapping("/{id}")
    public Result<OrderVO> getOrderDetail(@PathVariable String id) {
        return Result.success(queryHandler.getOrderDetailForOwner(id));
    }

    @GetMapping("/my")
    public Result<PageResult<OrderVO>> getMyOrders(@Valid QueryOrderRequest request) {
        return Result.success(queryHandler.getMyOrders(request.getStatus(),
                request.getPageNum(), request.getPageSize()));
    }

    @GetMapping("/sold")
    public Result<PageResult<OrderVO>> getSoldOrders(@Valid QueryOrderRequest request) {
        return Result.success(queryHandler.getSoldOrders(request.getStatus(),
                request.getPageNum(), request.getPageSize()));
    }

    @GetMapping("/list")
    public Result<PageResult<OrderVO>> queryOrders(@Valid QueryOrderRequest request) {
        return Result.success(queryHandler.handle(
                request.getOrderNo(),
                request.getStatus(), request.getBuyerId(), request.getSellerId(),
                request.getPageNum(), request.getPageSize()));
    }
}
