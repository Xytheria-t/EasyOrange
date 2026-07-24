package com.cartethyia.easyorange.payment.adapter.inbound.web.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private String id;

    private String paymentNo;

    private String orderId;

    private String orderNo;

    private String userId;

    private String username;

    private BigDecimal amount;

    private String paymentMethod;

    private String paymentMethodDesc;

    private String status;

    private String statusDesc;

    private String transactionId;

    private String refundReason;

    private LocalDateTime refundTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
