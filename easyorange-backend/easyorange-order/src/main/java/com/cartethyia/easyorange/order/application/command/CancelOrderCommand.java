package com.cartethyia.easyorange.order.application.command;

import jakarta.validation.constraints.NotBlank;

/**
 * 取消订单命令。
 */
public record CancelOrderCommand(
        @NotBlank(message = "订单 ID 不能为空") String orderId,
        @NotBlank(message = "取消原因不能为空") String reason) {}
