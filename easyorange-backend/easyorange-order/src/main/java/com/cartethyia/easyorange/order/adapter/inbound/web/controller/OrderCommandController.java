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
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "买家下单：按资产分布式锁排队，同事务扣库存并创建支付单")
    public Result<String> createOrder(
            @AuthenticationPrincipal AuthUser user, @Valid @RequestBody CreateOrderRequest request) {
        return Result.success(commandHandler
                .createOrder(user.userId(), assembler.toCreateCommand(request))
                .orderId());
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "买家取消待付款订单（仅 PENDING_PAYMENT），库存经事件恢复")
    public Result<Void> cancelOrder(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable String id,
            @Valid @RequestBody CancelOrderRequest request) {
        commandHandler.cancelOrder(user.userId(), assembler.toCancelCommand(id, request));
        return Result.success();
    }

    @PutMapping("/{id}/pay")
    @Operation(summary = "买家发起支付（须待付款态），订单置 PAID 由支付成功事件驱动")
    public Result<Void> payOrder(@AuthenticationPrincipal AuthUser user, @PathVariable String id) {
        commandHandler.payOrder(user.userId(), new PayOrderCommand(id));
        return Result.success();
    }

    @PutMapping("/{id}/ship")
    @Operation(summary = "卖家发货（仅 PAID → SHIPPED，限订单卖家本人）")
    public Result<Void> shipOrder(@AuthenticationPrincipal AuthUser user, @PathVariable String id) {
        commandHandler.shipOrder(user.userId(), new ShipOrderCommand(id));
        return Result.success();
    }

    @PutMapping("/{id}/receive")
    @Operation(summary = "买家确认收货（SHIPPED → COMPLETED），事件驱动资产标记售出")
    public Result<Void> confirmReceipt(@AuthenticationPrincipal AuthUser user, @PathVariable String id) {
        commandHandler.confirmReceipt(user.userId(), new ConfirmReceiptCommand(id));
        return Result.success();
    }

    @PutMapping("/{id}/refund")
    @Operation(summary = "买家申请退款（PAID / SHIPPED 且支付已付），退款与库存恢复走事件")
    public Result<Void> refundOrder(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable String id,
            @Valid @RequestBody RefundOrderRequest request) {
        commandHandler.refundOrder(user.userId(), assembler.toRefundCommand(id, request));
        return Result.success();
    }
}
