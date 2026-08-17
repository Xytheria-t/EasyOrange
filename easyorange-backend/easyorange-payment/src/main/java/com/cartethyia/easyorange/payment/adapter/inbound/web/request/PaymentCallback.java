package com.cartethyia.easyorange.payment.adapter.inbound.web.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentCallback {

    @NotBlank(message = "支付单号不能为空")
    private String paymentNo;

    @NotBlank(message = "渠道交易流水号不能为空")
    private String transactionId;

    private BigDecimal amount;

    private String paymentMethod;

    private String attach;

    private String sign;
}
