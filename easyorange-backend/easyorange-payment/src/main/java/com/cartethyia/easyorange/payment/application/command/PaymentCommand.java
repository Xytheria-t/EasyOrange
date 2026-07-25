package com.cartethyia.easyorange.payment.application.command;

/**
 * 支付命令密封接口。
 * <p>
 * permits 子句列出所有支持的支付命令类型，编译器确保 switch 穷尽性。
 */
public sealed interface PaymentCommand
    permits CreatePaymentCommand, PayCommand, RefundPaymentCommand, ClosePaymentCommand {
}
