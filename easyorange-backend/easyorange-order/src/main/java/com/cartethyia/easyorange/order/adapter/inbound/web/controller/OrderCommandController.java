package com.cartethyia.easyorange.order.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.order.adapter.inbound.web.assembler.OrderCommandAssembler;
import com.cartethyia.easyorange.order.adapter.inbound.web.dto.request.CancelOrderRequest;
import com.cartethyia.easyorange.order.adapter.inbound.web.dto.request.CreateOrderRequest;
import com.cartethyia.easyorange.order.adapter.inbound.web.dto.request.RefundOrderRequest;
import com.cartethyia.easyorange.order.application.command.ConfirmReceiptCommand;
import com.cartethyia.easyorange.order.application.command.OrderCommandHandler;
import com.cartethyia.easyorange.order.application.command.PayOrderCommand;
import com.cartethyia.easyorange.order.application.command.ShipOrderCommand;
import com.cartethyia.easyorange.order.application.service.OrderCreationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "订单管理", description = "订单创建/取消/确认")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderCommandController {

    private final OrderCommandHandler commandHandler;
    private final OrderCommandAssembler assembler;
    private final OrderCreationService orderCreationService;

    @PostMapping
    public Result<String> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return Result.success(orderCreationService
                .createOrder(assembler.toCreateCommand(request))
                .orderId());
    }

    @PutMapping("/{id}/cancel")
    public Result<Void> cancelOrder(@PathVariable String id, @Valid @RequestBody CancelOrderRequest request) {
        commandHandler.handle(assembler.toCancelCommand(id, request));
        return Result.success();
    }

    @PutMapping("/{id}/pay")
    public Result<Void> payOrder(@PathVariable String id) {
        commandHandler.handle(new PayOrderCommand(id));
        return Result.success();
    }

    @PutMapping("/{id}/ship")
    public Result<Void> shipOrder(@PathVariable String id) {
        commandHandler.handle(new ShipOrderCommand(id));
        return Result.success();
    }

    @PutMapping("/{id}/receive")
    public Result<Void> confirmReceipt(@PathVariable String id) {
        commandHandler.handle(new ConfirmReceiptCommand(id));
        return Result.success();
    }

    @PutMapping("/{id}/refund")
    public Result<Void> refundOrder(@PathVariable String id, @Valid @RequestBody RefundOrderRequest request) {
        commandHandler.handle(assembler.toRefundCommand(id, request));
        return Result.success();
    }
}
