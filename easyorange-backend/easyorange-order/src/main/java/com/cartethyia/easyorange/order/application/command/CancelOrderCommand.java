package com.cartethyia.easyorange.order.application.command;

/**
 * 取消订单命令。
 */
public record CancelOrderCommand(String orderId, String reason) {}
