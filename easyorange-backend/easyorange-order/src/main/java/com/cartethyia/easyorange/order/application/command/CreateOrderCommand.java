package com.cartethyia.easyorange.order.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建订单命令
 *
 * @author cartethyia
 * @date 2026/04/17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderCommand {

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 收货地址
     */
    private String address;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 备注
     */
    private String remark;

    /**
     * 支付方式
     */
    private Integer paymentMethod;
}