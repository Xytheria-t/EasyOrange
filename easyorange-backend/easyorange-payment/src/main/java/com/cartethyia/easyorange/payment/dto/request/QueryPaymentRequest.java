package com.cartethyia.easyorange.payment.dto.request;

import com.cartethyia.easyorange.common.dto.PageRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 查询支付记录请求
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QueryPaymentRequest extends PageRequest {

    private String paymentNo;

    private Long orderId;

    private Long userId;

    private Integer status;

    private Integer paymentMethod;
}
