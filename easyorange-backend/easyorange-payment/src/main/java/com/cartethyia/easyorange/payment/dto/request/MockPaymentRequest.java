package com.cartethyia.easyorange.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MockPaymentRequest {

    @NotNull(message = "支付ID不能为空")
    private Long paymentId;

    private String paymentNo;

    private BigDecimal amount;

    private Integer paymentMethod;

    private Boolean success;
}
