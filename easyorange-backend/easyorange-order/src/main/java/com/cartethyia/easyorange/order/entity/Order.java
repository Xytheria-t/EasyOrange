package com.cartethyia.easyorange.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * 订单实体
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("eo_order")
public class Order extends BaseDO {

    private String orderNo;

    private Long buyerId;

    private Long sellerId;

    private Long productId;

    private BigDecimal amount;

    /**
     * 订单状态：0-待付款 1-待发货 2-待收货 3-已完成 4-已取消 5-已退款
     */
    private Integer status;

    /**
     * 支付状态：0-未支付 1-已支付 2-已退款
     */
    private Integer paymentStatus;

    private String address;

    private String phone;

    private String remark;
}
