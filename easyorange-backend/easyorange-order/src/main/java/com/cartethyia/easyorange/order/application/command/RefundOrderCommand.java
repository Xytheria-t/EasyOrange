package com.cartethyia.easyorange.order.application.command;

import jakarta.validation.constraints.NotBlank;

/**
 * 退款命令。
 */
public record RefundOrderCommand(
        @NotBlank(message = "订单 ID 不能为空") String orderId,
        @NotBlank(message = "退款原因不能为空") String reason
) {}
