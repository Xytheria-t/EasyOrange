package com.cartethyia.easyorange.order.infrastructure.adapter;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.order.domain.port.outbound.PaymentGatewayPort;
import com.cartethyia.easyorange.order.enums.OrderResultCode;
import com.cartethyia.easyorange.payment.application.command.CreatePaymentCommand;
import com.cartethyia.easyorange.payment.application.command.PaymentCommandHandler;
import com.cartethyia.easyorange.payment.application.command.RefundPaymentCommand;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import com.cartethyia.easyorange.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentGatewayAdapter implements PaymentGatewayPort {

    private final PaymentCommandHandler paymentCommandHandler;
    private final PaymentRepository paymentRepository;

    @Override
    public Long createPayment(CreatePaymentRequest request) {
        CreatePaymentCommand command = new CreatePaymentCommand(
                request.orderId(),
                request.amount(),
                request.paymentMethod(),
                request.attach(),
                request.description()
        );
        return paymentCommandHandler.handle(command);
    }

    @Override
    public void refundPayment(Long orderId, String reason) {
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
