package com.cartethyia.easyorange.payment.application.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentQuery {

    private Long paymentId;
    private Long orderId;
    private Long userId;
    private Integer status;
    private Integer paymentMethod;
    private Integer pageNum;
    private Integer pageSize;
}
