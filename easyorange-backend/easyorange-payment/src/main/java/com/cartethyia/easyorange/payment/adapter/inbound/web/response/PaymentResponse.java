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

    private Long id;

    private String paymentNo;

    private Long orderId;

    private String orderNo;

    private Long userId;

    private String username;

    private BigDecimal amount;

    private Integer paymentMethod;

    private String paymentMethodDesc;

    private Integer status;

    private String statusDesc;

    private String transactionId;

    private String refundReason;

    private LocalDateTime refundTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
