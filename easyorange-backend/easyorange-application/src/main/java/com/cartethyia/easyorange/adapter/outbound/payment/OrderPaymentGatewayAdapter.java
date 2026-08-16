package com.cartethyia.easyorange.adapter.outbound.payment;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.order.domain.constant.OrderResultCode;
import com.cartethyia.easyorange.order.domain.port.PaymentGatewayPort;
import com.cartethyia.easyorange.payment.application.command.CreatePaymentCommand;
import com.cartethyia.easyorange.payment.application.command.PayCommand;
import com.cartethyia.easyorange.payment.application.command.PaymentCommandHandler;
import com.cartethyia.easyorange.payment.application.command.RefundPaymentCommand;
import com.cartethyia.easyorange.payment.domain.aggregate.Payment;
import com.cartethyia.easyorange.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@RequiredArgsConstructor
public class OrderPaymentGatewayAdapter implements PaymentGatewayPort {

    private final PaymentCommandHandler paymentCommandHandler;
    private final PaymentRepository paymentRepository;

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
        Payment payment = paymentRepository
                .findByOrderId(orderId)
                .orElseThrow(() -> BusinessException.of(OrderResultCode.ORDER_NOT_FOUND, "支付单不存在"));

        paymentCommandHandler.handle(new PayCommand(payment.paymentNo(), null, null));
    }

    @Override
    public void refundPayment(String orderId, String reason) {
        Payment payment = paymentRepository
                .findByOrderId(orderId)
                .orElseThrow(() -> BusinessException.of(OrderResultCode.ORDER_NOT_FOUND, "支付单不存在"));

        RefundPaymentCommand command = new RefundPaymentCommand(payment.id(), payment.amount(), reason);
        paymentCommandHandler.handle(command);
    }
}
