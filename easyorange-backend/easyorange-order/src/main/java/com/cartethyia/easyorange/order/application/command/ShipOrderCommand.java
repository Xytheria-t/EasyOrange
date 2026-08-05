package com.cartethyia.easyorange.order.application.command;

import jakarta.validation.constraints.NotBlank;

/**
 * 发货命令。
 */
public record ShipOrderCommand(
        @NotBlank(message = "订单 ID 不能为空") String orderId
) {}
