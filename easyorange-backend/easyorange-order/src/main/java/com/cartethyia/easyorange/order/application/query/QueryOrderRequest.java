package com.cartethyia.easyorange.order.application.query;

import com.cartethyia.easyorange.common.dto.PageRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QueryOrderRequest extends PageRequest {

    private String orderNo;
    private Integer status;
    private Long buyerId;
    private Long sellerId;
    private Long productId;

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
