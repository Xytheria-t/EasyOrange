package com.cartethyia.easyorange.order.application.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单查询对象
 *
 * @author cartethyia
 * @date 2026/04/17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderQuery {

    /**
     * 订单ID
     */
    private Long id;

    /**
     * 买家ID
     */
    private Long buyerId;

    /**
     * 卖家ID
     */
    private Long sellerId;

    /**
     * 订单状态
     */
    private Integer status;

    /**
     * 页码
     */
    private Integer pageNum;

    /**
     * 每页大小
     */
    private Integer pageSize;
}