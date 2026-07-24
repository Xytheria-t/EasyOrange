package com.cartethyia.easyorange.payment.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.payment.adapter.inbound.web.response.PaymentResponse;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentViewAssembler {

    public PaymentResponse toPaymentResponse(PaymentAggregate aggregate) {
        if (aggregate == null) {
            return null;
        }
        return PaymentResponse.builder()
                .id(aggregate.id())
                .paymentNo(aggregate.paymentNo())
                .orderId(aggregate.orderId())
                .userId(aggregate.userId())
                .amount(aggregate.amount())
                .paymentMethod(aggregate.paymentMethod().getCode())
                .paymentMethodDesc(PaymentMethod.getDescByCode(aggregate.paymentMethod().getCode()))
                .status(aggregate.status().getCode())
                .statusDesc(PaymentStatus.getDescByCode(aggregate.status().getCode()))
                .transactionId(aggregate.transactionId())
                .refundReason(aggregate.refundReason())
                .refundTime(aggregate.refundTime())
                .createTime(aggregate.createTime())
                .updateTime(aggregate.updateTime())
                .build();
    }

    public PageResult<PaymentResponse> toPageResult(PageResult<PaymentAggregate> page) {
        List<PaymentResponse> records = page.records().stream()
                .map(this::toPaymentResponse)
                .toList();
        return PageResult.of(records, page.total(), page.current(), page.size());
    }

    public PaymentResponse toPaymentResponseWithOrderInfo(PaymentAggregate aggregate, String orderNo, String username) {
        PaymentResponse response = toPaymentResponse(aggregate);
        return PaymentResponse.builder()
                .id(response.getId())
                .paymentNo(response.getPaymentNo())
                .orderId(response.getOrderId())
                .orderNo(orderNo)
                .userId(response.getUserId())
                .username(username)
                .amount(response.getAmount())
                .paymentMethod(response.getPaymentMethod())
                .paymentMethodDesc(response.getPaymentMethodDesc())
                .status(response.getStatus())
                .statusDesc(response.getStatusDesc())
                .transactionId(response.getTransactionId())
                .refundReason(response.getRefundReason())
                .refundTime(response.getRefundTime())
                .createTime(response.getCreateTime())
                .updateTime(response.getUpdateTime())
                .build();
    }
}