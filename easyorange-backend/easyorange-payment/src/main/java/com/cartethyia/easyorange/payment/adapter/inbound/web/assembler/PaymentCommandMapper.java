package com.cartethyia.easyorange.payment.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.payment.adapter.inbound.web.request.CreatePaymentRequest;
import com.cartethyia.easyorange.payment.adapter.inbound.web.request.PaymentCallback;
import com.cartethyia.easyorange.payment.adapter.inbound.web.request.RefundRequest;
import com.cartethyia.easyorange.payment.application.command.ClosePaymentCommand;
import com.cartethyia.easyorange.payment.application.command.CreatePaymentCommand;
import com.cartethyia.easyorange.payment.application.command.PaymentCallbackCommand;
import com.cartethyia.easyorange.payment.application.command.RefundPaymentCommand;

/**
 * 支付命令装配 — 请求 DTO → 命令对象（纯手工映射，无 MapStruct 注解处理器介入）。
 */
public final class PaymentCommandMapper {

    private PaymentCommandMapper() {}

    public static CreatePaymentCommand toCreateCommand(CreatePaymentRequest request, String userId) {
        return new CreatePaymentCommand(request.orderId(), request.amount(), request.paymentMethod(), null, null);
    }

    public static PaymentCallbackCommand toCallbackCommand(PaymentCallback callback) {
        return new PaymentCallbackCommand(callback.getPaymentNo(), callback.getTransactionId(), callback.getAmount());
    }

    public static RefundPaymentCommand toRefundCommand(String paymentId, String userId, RefundRequest request) {
        return new RefundPaymentCommand(paymentId, userId, request.refundAmount(), request.refundReason());
    }

    public static ClosePaymentCommand toCloseCommand(String paymentId, String userId) {
        return new ClosePaymentCommand(paymentId, userId);
    }
}
