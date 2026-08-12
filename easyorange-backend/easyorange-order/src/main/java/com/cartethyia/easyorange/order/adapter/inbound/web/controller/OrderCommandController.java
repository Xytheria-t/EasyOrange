package com.cartethyia.easyorange.order.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.security.AuthUser;
import com.cartethyia.easyorange.order.adapter.inbound.web.assembler.OrderCommandAssembler;
import com.cartethyia.easyorange.order.adapter.inbound.web.dto.request.CancelOrderRequest;
import com.cartethyia.easyorange.order.adapter.inbound.web.dto.request.CreateOrderRequest;
import com.cartethyia.easyorange.order.adapter.inbound.web.dto.request.RefundOrderRequest;
import com.cartethyia.easyorange.order.application.command.ConfirmReceiptCommand;
import com.cartethyia.easyorange.order.application.command.OrderCommandHandler;
import com.cartethyia.easyorange.order.application.command.PayOrderCommand;
import com.cartethyia.easyorange.order.application.command.ShipOrderCommand;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "订单管理", description = "订单创建/取消/确认")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderCommandController {

    private final OrderCommandHandler commandHandler;
    private final OrderCommandAssembler assembler;

    @PostMapping
    public Result<String> createOrder(
            @AuthenticationPrincipal AuthUser user, @Valid @RequestBody CreateOrderRequest request) {
        return Result.success(commandHandler
                .handle(user.userId(), assembler.toCreateCommand(request))
                .orderId());
    }

    @PutMapping("/{id}/cancel")
    public Result<Void> cancelOrder(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable String id,
            @Valid @RequestBody CancelOrderRequest request) {
        commandHandler.handle(user.userId(), assembler.toCancelCommand(id, request));
        return Result.success();
    }

    @PutMapping("/{id}/pay")
    public Result<Void> payOrder(@AuthenticationPrincipal AuthUser user, @PathVariable String id) {
        commandHandler.handle(user.userId(), new PayOrderCommand(id));
        return Result.success();
    }

    @PutMapping("/{id}/ship")
    public Result<Void> shipOrder(@AuthenticationPrincipal AuthUser user, @PathVariable String id) {
        commandHandler.handle(user.userId(), new ShipOrderCommand(id));
        return Result.success();
    }

    @PutMapping("/{id}/receive")
    public Result<Void> confirmReceipt(@AuthenticationPrincipal AuthUser user, @PathVariable String id) {
        commandHandler.handle(user.userId(), new ConfirmReceiptCommand(id));
        return Result.success();
    }

    @PutMapping("/{id}/refund")
    public Result<Void> refundOrder(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable String id,
            @Valid @RequestBody RefundOrderRequest request) {
        commandHandler.handle(user.userId(), assembler.toRefundCommand(id, request));
        return Result.success();
    }
}
