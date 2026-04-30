package com.cartethyia.easyorange.payment.adapter.outbound.persistence.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("eo_payment")
public class PaymentPO extends BaseDO {

    private String paymentNo;

    private Long orderId;

    private Long userId;

    private BigDecimal amount;

    private BigDecimal refundedAmount;

    private Integer paymentMethod;

    private Integer status;

    private String transactionId;

    private String refundReason;

    private LocalDateTime refundTime;

    private String attach;
}
