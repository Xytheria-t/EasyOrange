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

    default CreatePaymentCommand toCreateCommand(CreatePaymentRequest request, String userId) {
        return new CreatePaymentCommand(
                request.getOrderId(), request.getAmount(), request.getPaymentMethod(), request.getPayPassword(), null);
    }

    PayCommand toPayCommand(PaymentCallback callback);

    default RefundPaymentCommand toRefundCommand(String paymentId, RefundRequest request) {
        return new RefundPaymentCommand(paymentId, request.getRefundAmount(), request.getRefundReason());
    }

    default ClosePaymentCommand toCloseCommand(String paymentId) {
        return new ClosePaymentCommand(paymentId);
    }
}
