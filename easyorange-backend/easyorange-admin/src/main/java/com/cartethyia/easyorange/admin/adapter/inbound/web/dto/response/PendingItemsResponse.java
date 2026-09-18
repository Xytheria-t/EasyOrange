package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PendingItemsResponse {

    private Long pendingOrders;

    private Long pendingProducts;
}
