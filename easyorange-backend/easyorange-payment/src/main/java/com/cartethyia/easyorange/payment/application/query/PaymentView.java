package com.cartethyia.easyorange.payment.application.query;

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
public class PaymentView {

    private Long id;
    private String paymentNo;
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private BigDecimal refundedAmount;
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
