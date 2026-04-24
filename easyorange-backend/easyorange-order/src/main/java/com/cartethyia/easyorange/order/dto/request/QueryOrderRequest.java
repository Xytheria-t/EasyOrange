package com.cartethyia.easyorange.order.dto.request;

import com.cartethyia.easyorange.common.dto.PageRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 查询订单请求
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QueryOrderRequest extends PageRequest {

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 订单状态
     */
    private Integer status;

    /**
     * 买家 ID
     */
    private Long buyerId;

    /**
     * 卖家 ID
     */
    private Long sellerId;

    /**
     * 商品 ID
     */
    private Long productId;

    /**
     * 返回规范化后的新实例（不可变模式）
     * <p>
     * 将 null 或非法值替换为默认值，返回新的 QueryOrderRequest 实例，不修改原对象。
     * </p>
     *
     * @return 规范化后的新 QueryOrderRequest 实例
     */
    @Override
    public QueryOrderRequest normalized() {
        PageRequest base = super.normalized();
        return QueryOrderRequest.builder()
                .pageNum(base.getPageNum())
                .pageSize(base.getPageSize())
                .sortField(base.getSortField())
                .sortDirection(base.getSortDirection())
                .orderNo(this.orderNo)
                .status(this.status)
                .buyerId(this.buyerId)
                .sellerId(this.sellerId)
                .productId(this.productId)
                .build();
    }
}
