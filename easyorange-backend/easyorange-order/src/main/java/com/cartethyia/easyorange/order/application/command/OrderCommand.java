package com.cartethyia.easyorange.order.application.command;

/**
 * 订单命令密封接口。
 * <p>
 * permits 子句列出所有支持的订单命令类型，编译器确保 switch 穷尽性。
 */
public sealed interface OrderCommand
    permits CreateOrderCommand, PayOrderCommand, CancelOrderCommand,
            ShipOrderCommand, ConfirmReceiptCommand, RefundOrderCommand {
}
