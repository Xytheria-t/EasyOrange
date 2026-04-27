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
public class CreatePaymentCommand {

    private Long orderId;
    private BigDecimal amount;
    private Integer paymentMethod;
    private String payPassword;
    private String attach;
}