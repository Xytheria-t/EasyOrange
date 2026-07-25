package com.cartethyia.easyorange.order.application.command;

import jakarta.validation.constraints.NotBlank;

/**
 * 确认收货命令。
 */
public record ConfirmReceiptCommand(
        @NotBlank(message = "订单 ID 不能为空") String orderId
) implements OrderCommand {}
