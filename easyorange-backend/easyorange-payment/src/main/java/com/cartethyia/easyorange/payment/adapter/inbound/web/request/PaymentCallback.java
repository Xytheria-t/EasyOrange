package com.cartethyia.easyorange.payment.adapter.inbound.web.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentCallback {

    private String paymentNo;

    private String transactionId;

    private BigDecimal amount;

    private Integer paymentMethod;

    private String attach;

    private String sign;
}
