package com.cartethyia.easyorange.payment.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.payment.adapter.inbound.web.request.CreatePaymentRequest;
import com.cartethyia.easyorange.payment.adapter.inbound.web.request.PaymentCallback;
import com.cartethyia.easyorange.payment.adapter.inbound.web.request.RefundRequest;
import com.cartethyia.easyorange.payment.application.command.ClosePaymentCommand;
import com.cartethyia.easyorange.payment.application.command.CreatePaymentCommand;
import com.cartethyia.easyorange.payment.application.command.PayCommand;
import com.cartethyia.easyorange.payment.application.command.RefundPaymentCommand;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentCommandMapper {

    default CreatePaymentCommand toCreateCommand(CreatePaymentRequest request, Long userId) {
        return CreatePaymentCommand.builder()
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .payPassword(request.getPayPassword())
                .attach(null)
                .build();
    }

    PayCommand toPayCommand(PaymentCallback callback);

    default RefundPaymentCommand toRefundCommand(Long paymentId, RefundRequest request) {
        return RefundPaymentCommand.builder()
                .paymentId(paymentId)
                .refundAmount(request.getRefundAmount())
                .refundReason(request.getRefundReason())
                .build();
    }

    default ClosePaymentCommand toCloseCommand(Long paymentId) {
        return ClosePaymentCommand.builder()
                .paymentId(paymentId)
                .build();
    }
}
