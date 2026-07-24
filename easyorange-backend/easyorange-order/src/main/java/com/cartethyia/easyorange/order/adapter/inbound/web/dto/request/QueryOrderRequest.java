package com.cartethyia.easyorange.order.adapter.inbound.web.dto.request;

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
    private String buyerId;
    private String sellerId;
    private String status;

}