package com.cartethyia.easyorange.order.adapter.outbound.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.framework.entity.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@TableName("eo_order")
public class OrderDO extends BaseDO {

    private String id;
    private String orderNo;
    private String buyerId;
    private String sellerId;
    private BigDecimal totalAmount;
    private Integer status;
    private Integer paymentStatus;
    private String address;
    private String phone;
    private String remark;
    private String cancelReason;
    private LocalDateTime cancelTime;
}
