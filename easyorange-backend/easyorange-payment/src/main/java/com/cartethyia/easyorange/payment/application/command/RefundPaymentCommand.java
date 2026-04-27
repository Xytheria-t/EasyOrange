package com.cartethyia.easyorange.payment.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundPaymentCommand {

    private Long paymentId;
    private BigDecimal refundAmount;
    private String refundReason;
}