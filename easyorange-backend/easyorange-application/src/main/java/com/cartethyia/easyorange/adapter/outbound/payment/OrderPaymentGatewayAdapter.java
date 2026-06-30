package com.cartethyia.easyorange.adapter.outbound.payment;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.order.domain.constant.OrderResultCode;
import com.cartethyia.easyorange.order.domain.port.PaymentGatewayPort;
import com.cartethyia.easyorange.payment.application.command.CreatePaymentCommand;
import com.cartethyia.easyorange.payment.application.command.PaymentCommandHandler;
import com.cartethyia.easyorange.payment.application.command.RefundPaymentCommand;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import com.cartethyia.easyorange.payment.domain.repository.PaymentRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderPaymentGatewayAdapter implements PaymentGatewayPort {

    private final PaymentCommandHandler paymentCommandHandler;
    private final PaymentRepositoryPort paymentRepository;

    @Override
    public String createPayment(CreatePaymentRequest request) {
        CreatePaymentCommand command = CreatePaymentCommand.builder()
                .orderId(request.orderId())
                .amount(request.amount())
                .paymentMethod(request.paymentMethod())
                .attach(request.attach())
                .build();
        return paymentCommandHandler.handle(command);
    }

    @Override
    public void refundPayment(String orderId, String reason) {
        PaymentAggregate payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> BusinessException.of(OrderResultCode.ORDER_NOT_FOUND, "支付单不存在"));

        RefundPaymentCommand command = RefundPaymentCommand.builder()
                .paymentId(payment.id())
                .refundAmount(payment.amount())
                .refundReason(reason)
                .build();
        paymentCommandHandler.handle(command);
    }
}
