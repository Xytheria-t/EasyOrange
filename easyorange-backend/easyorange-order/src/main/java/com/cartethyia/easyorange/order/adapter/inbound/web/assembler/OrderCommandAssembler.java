package com.cartethyia.easyorange.order.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.order.adapter.inbound.web.dto.request.CancelOrderRequest;
import com.cartethyia.easyorange.order.adapter.inbound.web.dto.request.CreateOrderRequest;
import com.cartethyia.easyorange.order.adapter.inbound.web.dto.request.RefundOrderRequest;
import com.cartethyia.easyorange.order.application.command.CancelOrderCommand;
import com.cartethyia.easyorange.order.application.command.CreateOrderCommand;
import com.cartethyia.easyorange.order.application.command.RefundOrderCommand;
import org.springframework.stereotype.Component;

/**
 * 订单命令 DTO 转换器 — 将 HTTP 入参 (Request DTO) 转换为应用层 Command。
 * <p>
 * 遵循项目 Assembler 模式，保持 Controller 薄，映射逻辑集中在此。
 * 因映射为简单的字段复制 + 流转换，使用 {@link Component} 而非 MapStruct，
 * 避免为简单映射引入注解处理器开销。
 */
@Component
public class OrderCommandAssembler {

    /**
     * 创建订单请求 → 创建订单命令。
     * paymentMethod 不来自前端请求，由 Saga 在创建支付时使用默认值。
     */
    public CreateOrderCommand toCreateCommand(CreateOrderRequest request) {
        var items = request.getItems().stream()
                .map(i -> new CreateOrderCommand.CreateOrderItem(i.getProductId(), i.getQuantity()))
                .toList();
        return new CreateOrderCommand(items, request.getAddress(), request.getPhone(), request.getRemark(), null);
    }

    /**
     * 取消订单请求 → 取消订单命令。
     */
    public CancelOrderCommand toCancelCommand(String orderId, CancelOrderRequest request) {
        return new CancelOrderCommand(orderId, request.reason());
    }

    /**
     * 退款订单请求 → 退款订单命令。
     */
    public RefundOrderCommand toRefundCommand(String orderId, RefundOrderRequest request) {
        return new RefundOrderCommand(orderId, request.reason());
    }
}
