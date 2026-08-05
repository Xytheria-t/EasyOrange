package com.cartethyia.easyorange.order.application.command;

import jakarta.validation.constraints.NotBlank;

/**
 * 支付订单命令。
 */
public record PayOrderCommand(
        @NotBlank(message = "订单 ID 不能为空") String orderId
) {}
