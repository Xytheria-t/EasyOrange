package com.cartethyia.easyorange.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录实体
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("eo_payment")
public class Payment extends BaseDO {

    private String paymentNo;

    private Long orderId;

    private Long userId;

    private BigDecimal amount;

    private BigDecimal refundedAmount;

    /**
     * 支付方式：1-微信 2-支付宝 3-余额
     */
    private Integer paymentMethod;

    /**
     * 支付状态：0-待支付 1-支付中 2-已支付 3-已退款 4-已关闭
     */
    private Integer status;

    private String transactionId;

    private String refundReason;

    private LocalDateTime refundTime;

    private String attach;
}
