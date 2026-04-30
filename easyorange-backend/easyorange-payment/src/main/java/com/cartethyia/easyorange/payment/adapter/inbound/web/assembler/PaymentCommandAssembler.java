package com.cartethyia.easyorange.payment.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.payment.adapter.inbound.web.request.CreatePaymentRequest;
import com.cartethyia.easyorange.payment.adapter.inbound.web.request.PaymentCallback;
import com.cartethyia.easyorange.payment.adapter.inbound.web.request.RefundRequest;
import com.cartethyia.easyorange.payment.application.command.ClosePaymentCommand;
import com.cartethyia.easyorange.payment.application.command.CreatePaymentCommand;
import com.cartethyia.easyorange.payment.application.command.PayCommand;
import com.cartethyia.easyorange.payment.application.command.RefundPaymentCommand;

public final class PaymentCommandAssembler {

    private PaymentCommandAssembler() {}

    public static CreatePaymentCommand toCreateCommand(CreatePaymentRequest request, Long userId) {
        return CreatePaymentCommand.builder()
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .payPassword(request.getPayPassword())
                .attach(null)
                .build();
    }

    public static PayCommand toPayCommand(PaymentCallback callback) {
        return PayCommand.builder()
                .paymentNo(callback.getPaymentNo())
                .transactionId(callback.getTransactionId())
                .attach(callback.getAttach())
                .build();
    }

    public static RefundPaymentCommand toRefundCommand(Long paymentId, RefundRequest request) {
        return RefundPaymentCommand.builder()
                .paymentId(paymentId)
                .refundAmount(request.getRefundAmount())
                .refundReason(request.getRefundReason())
                .build();
    }

    public static ClosePaymentCommand toCloseCommand(Long paymentId) {
        return ClosePaymentCommand.builder()
                .paymentId(paymentId)
                .build();
    }
}
