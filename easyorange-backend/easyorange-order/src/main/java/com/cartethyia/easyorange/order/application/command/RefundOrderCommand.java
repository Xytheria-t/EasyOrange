package com.cartethyia.easyorange.order.application.command;

/**
 * 退款命令。
 */
public record RefundOrderCommand(String orderId, String reason) {}
