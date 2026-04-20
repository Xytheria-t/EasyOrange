package com.cartethyia.easyorange.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退款请求
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {

    @NotNull(message = "支付 ID 不能为空")
    private Long paymentId;

    private String refundReason;
}
