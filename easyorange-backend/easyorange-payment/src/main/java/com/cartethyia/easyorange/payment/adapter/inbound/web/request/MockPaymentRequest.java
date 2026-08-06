package com.cartethyia.easyorange.payment.adapter.inbound.web.request;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MockPaymentRequest {

    @NotNull(message = "支付ID不能为空")
    private String paymentId;

    private String paymentNo;

    private BigDecimal amount;

    private String paymentMethod;

    private Boolean success;
}
