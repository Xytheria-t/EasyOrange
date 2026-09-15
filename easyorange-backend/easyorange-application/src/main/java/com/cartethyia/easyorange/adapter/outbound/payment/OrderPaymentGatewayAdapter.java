package com.cartethyia.easyorange.adapter.outbound.payment;

import com.cartethyia.easyorange.order.domain.port.PaymentGatewayPort;
import com.cartethyia.easyorange.payment.application.command.CreatePaymentCommand;
import com.cartethyia.easyorange.payment.application.command.PaymentCommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 订单 → 支付 的 ACL 出口 — 把订单侧端口（以 orderId 为键）翻译成支付模块的应用用例。
 * <p>
 * 只做键翻译与委托：支付单解析与「不存在」的错误码（B4001）留在支付模块内，
 * 本类不得直接依赖支付的领域仓储或领域模型。
 */
@Primary
@Component
@RequiredArgsConstructor
public class OrderPaymentGatewayAdapter implements PaymentGatewayPort {

    private final PaymentCommandHandler paymentCommandHandler;

    @Override
    public String createPayment(CreatePaymentRequest request) {
        CreatePaymentCommand command = new CreatePaymentCommand(
                request.orderId(),
                request.amount(),
                request.paymentMethod(),
                null, // payPassword
                request.attach());
        return paymentCommandHandler.handle(request.buyerId(), command);
    }

    @Override
    public void pay(String orderId) {
        paymentCommandHandler.payByOrderId(orderId);
    }

    /**
     * 订单取消自动退款 — 系统内部路径，操作者记为支付单所属用户（通过归属校验）。
     */
    @Override
    public void refundPayment(String orderId, String reason) {
        paymentCommandHandler.refundByOrderId(orderId, reason);
    }
}
