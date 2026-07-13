package com.cartethyia.easyorange.order.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.annotation.Idempotent;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.order.application.command.CancelOrderCommand;
import com.cartethyia.easyorange.order.application.command.ConfirmReceiptCommand;
import com.cartethyia.easyorange.order.application.command.CreateOrderCommand;
import com.cartethyia.easyorange.order.application.command.CreateOrderResult;
import com.cartethyia.easyorange.order.application.command.OrderCommandHandler;
import com.cartethyia.easyorange.order.application.command.PayOrderCommand;
import com.cartethyia.easyorange.order.application.command.RefundOrderCommand;
import com.cartethyia.easyorange.order.application.command.ShipOrderCommand;
import com.cartethyia.easyorange.order.adapter.inbound.web.dto.request.CreateOrderRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderCommandController {

    private final OrderCommandHandler commandHandler;

    @PostMapping
    @Idempotent
    public Result<String> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        List<CreateOrderCommand.CreateOrderItem> items = request.getItems().stream()
                .map(i -> CreateOrderCommand.CreateOrderItem.builder()
                        .productId(i.getProductId())
                        .quantity(i.getQuantity())
                        .build())
                .toList();
        CreateOrderCommand command = CreateOrderCommand.builder()
                .items(items)
                .address(request.getAddress())
                .phone(request.getPhone())
                .remark(request.getRemark())
                .build();
        CreateOrderResult result = commandHandler.handle(command);
        return Result.success(result.orderId());
    }

    @PutMapping("/{id}/cancel")
    public Result<Void> cancelOrder(@PathVariable String id, @RequestParam(required = false) String reason) {
        CancelOrderCommand command = CancelOrderCommand.builder()
                .orderId(id)
                .reason(reason)
                .build();
        commandHandler.handle(command);
        return Result.success();
    }

    @PutMapping("/{id}/pay")
    public Result<Void> payOrder(@PathVariable String id) {
        PayOrderCommand command = PayOrderCommand.builder()
                .orderId(id)
                .build();
        commandHandler.handle(command);
        return Result.success();
    }

    @PutMapping("/{id}/ship")
    public Result<Void> shipOrder(@PathVariable String id) {
        ShipOrderCommand command = ShipOrderCommand.builder()
                .orderId(id)
                .build();
        commandHandler.handle(command);
        return Result.success();
    }

    @PutMapping("/{id}/receive")
    public Result<Void> confirmReceipt(@PathVariable String id) {
        ConfirmReceiptCommand command = ConfirmReceiptCommand.builder()
                .orderId(id)
                .build();
        commandHandler.handle(command);
        return Result.success();
    }

    @PutMapping("/{id}/refund")
    public Result<Void> refundOrder(@PathVariable String id, @RequestParam(required = false) String reason) {
        RefundOrderCommand command = RefundOrderCommand.builder()
                .orderId(id)
                .reason(reason)
                .build();
        commandHandler.handle(command);
        return Result.success();
    }
}