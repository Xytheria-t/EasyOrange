package com.cartethyia.easyorange.payment.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建支付请求
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

    @NotNull(message = "订单 ID 不能为空")
    private Long orderId;

    @NotNull(message = "支付方式不能为空")
    private Integer paymentMethod;

    @JsonIgnore
    private String payPassword;
}
