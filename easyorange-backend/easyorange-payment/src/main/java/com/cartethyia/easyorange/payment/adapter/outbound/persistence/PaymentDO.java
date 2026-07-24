package com.cartethyia.easyorange.payment.adapter.outbound.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.cartethyia.easyorange.common.entity.BaseDO;
import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
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
public class PaymentDO extends BaseDO {

    private String paymentNo;

    private String orderId;

    private String userId;

    private BigDecimal amount;

    private BigDecimal refundedAmount;

    private PaymentMethod paymentMethod;

    private PaymentStatus status;

    private String transactionId;

    private String refundReason;

    private LocalDateTime refundTime;

    private String attach;

    @Version
    private Integer version;
}
