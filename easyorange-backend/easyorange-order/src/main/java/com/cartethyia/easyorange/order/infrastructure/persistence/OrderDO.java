package com.cartethyia.easyorange.order.infrastructure.persistence;

import com.cartethyia.easyorange.common.entity.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDO extends BaseDO {

    private Long id;
    private String orderNo;
    private Long buyerId;
    private Long sellerId;
    private Long productId;
    private BigDecimal amount;
    private Integer status;
    private Integer paymentStatus;
    private String address;
    private String phone;
    private String remark;
    private String cancelReason;
    private LocalDateTime cancelTime;
}
