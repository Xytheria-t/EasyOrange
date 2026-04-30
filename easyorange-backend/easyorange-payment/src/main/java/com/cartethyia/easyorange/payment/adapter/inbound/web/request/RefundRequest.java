package com.cartethyia.easyorange.payment.adapter.inbound.web.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {

    @NotNull(message = "支付 ID 不能为空")
    private Long paymentId;

    private BigDecimal refundAmount;

    private String refundReason;
}
