package com.cartethyia.easyorange.order.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.order.application.command.CancelOrderCommand;
import com.cartethyia.easyorange.order.application.command.ConfirmReceiptCommand;
import com.cartethyia.easyorange.order.application.command.CreateOrderCommand;
import com.cartethyia.easyorange.order.application.command.CreateOrderResult;
import com.cartethyia.easyorange.order.application.command.OrderCommandHandler;
import com.cartethyia.easyorange.order.application.command.PayOrderCommand;
import com.cartethyia.easyorange.order.application.command.RefundOrderCommand;
import com.cartethyia.easyorange.order.application.command.ShipOrderCommand;
import com.cartethyia.easyorange.order.application.query.OrderQueryHandler;
import com.cartethyia.easyorange.order.adapter.inbound.web.dto.request.CreateOrderRequest;
import com.cartethyia.easyorange.order.application.dto.OrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderCommandController {

    private final OrderCommandHandler commandHandler;
    private final OrderQueryHandler queryHandler;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public Result<OrderVO> createOrder(@Valid @RequestBody CreateOrderRequest request) {
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
        return Result.success(queryHandler.getOrderDetail(result.orderId()));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> cancelOrder(@PathVariable Long id, @RequestParam(required = false) String reason) {
        CancelOrderCommand command = CancelOrderCommand.builder()
                .orderId(id)
                .reason(reason)
                .build();
        commandHandler.handle(command);
        return Result.success();
    }

    @PutMapping("/{id}/pay")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> payOrder(@PathVariable Long id) {
        PayOrderCommand command = PayOrderCommand.builder()
                .orderId(id)
                .build();
        commandHandler.handle(command);
        return Result.success();
    }

    @PutMapping("/{id}/ship")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> shipOrder(@PathVariable Long id) {
        ShipOrderCommand command = ShipOrderCommand.builder()
                .orderId(id)
                .build();
        commandHandler.handle(command);
        return Result.success();
    }

    @PutMapping("/{id}/receive")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> confirmReceipt(@PathVariable Long id) {
        ConfirmReceiptCommand command = ConfirmReceiptCommand.builder()
                .orderId(id)
                .build();
        commandHandler.handle(command);
        return Result.success();
    }

    @PutMapping("/{id}/refund")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> refundOrder(@PathVariable Long id, @RequestParam(required = false) String reason) {
        RefundOrderCommand command = RefundOrderCommand.builder()
                .orderId(id)
                .reason(reason)
                .build();
        commandHandler.handle(command);
        return Result.success();
    }
}
