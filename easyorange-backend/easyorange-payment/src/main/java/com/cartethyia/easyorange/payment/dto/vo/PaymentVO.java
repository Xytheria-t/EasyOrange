package com.cartethyia.easyorange.payment.dto.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录 VO
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentVO {

    private Long id;

    private String paymentNo;

    private Long orderId;

    private String orderNo;

    private Long userId;

    private String username;

    private BigDecimal amount;

    private Integer paymentMethod;

    private String paymentMethodDesc;

    private Integer status;

    private String statusDesc;

    private String transactionId;

    private String refundReason;

    private LocalDateTime refundTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
