package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AdminOrderQueryRequest(
        String orderNo,
        String buyerId,
        String sellerId,
        String status,
        String paymentStatus,
        String startTime,
        String endTime,
        @Min(value = 1, message = "页码最小为1") Integer pageNum,

        @Min(value = 1, message = "每页条数最小为1") @Max(value = 100, message = "每页条数最大为100")
        Integer pageSize) {

    public AdminOrderQueryRequest {
        if (pageNum == null) {
            pageNum = 1;
        }
        if (pageSize == null) {
            pageSize = 20;
        }
    }
}
