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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "订单管理", description = "订单创建/取消/确认")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class OrderCommandController {

    private final OrderCommandHandler commandHandler;

    @PostMapping
    @Idempotent
    public Result<String> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        List<CreateOrderCommand.CreateOrderItem> items = request.getItems().stream()
                .map(i -> new CreateOrderCommand.CreateOrderItem(i.getProductId(), i.getQuantity()))
                .toList();
        CreateOrderCommand command = new CreateOrderCommand(
                items, request.getAddress(), request.getPhone(), request.getRemark(), null);
        CreateOrderResult result = commandHandler.handle(command);
        return Result.success(result.orderId());
    }

    @PutMapping("/{id}/cancel")
    public Result<Void> cancelOrder(@PathVariable @NotBlank(message = "订单 ID 不能为空") String id,
                                     @RequestParam @NotBlank(message = "取消原因不能为空") String reason) {
        commandHandler.handle(new CancelOrderCommand(id, reason));
        return Result.success();
    }

    @PutMapping("/{id}/pay")
    public Result<Void> payOrder(@PathVariable @NotBlank(message = "订单 ID 不能为空") String id) {
        commandHandler.handle(new PayOrderCommand(id));
        return Result.success();
    }

    @PutMapping("/{id}/ship")
    public Result<Void> shipOrder(@PathVariable @NotBlank(message = "订单 ID 不能为空") String id) {
        commandHandler.handle(new ShipOrderCommand(id));
        return Result.success();
    }

    @PutMapping("/{id}/receive")
    public Result<Void> confirmReceipt(@PathVariable @NotBlank(message = "订单 ID 不能为空") String id) {
        commandHandler.handle(new ConfirmReceiptCommand(id));
        return Result.success();
    }

    @PutMapping("/{id}/refund")
    public Result<Void> refundOrder(@PathVariable @NotBlank(message = "订单 ID 不能为空") String id,
                                     @RequestParam @NotBlank(message = "退款原因不能为空") String reason) {
        commandHandler.handle(new RefundOrderCommand(id, reason));
        return Result.success();
    }
}
