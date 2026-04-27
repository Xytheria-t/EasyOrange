package com.cartethyia.easyorange.order.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付订单命令
 *
 * @author cartethyia
 * @date 2026/04/17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayOrderCommand {

    /**
     * 订单ID
     */
    private Long orderId;
}