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
    private String id;

    /**
     * 认领方ID
     */
    private String buyerId;

    /**
     * 资产方ID
     */
    private String sellerId;

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