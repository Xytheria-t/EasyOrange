package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request;

import java.time.LocalDateTime;

public record AdminRatingQueryRequest(
        Integer pageNum,
        Integer pageSize,
        String productId,
        String userId,
        Integer rating,
        Integer status,
        String keyword,
        LocalDateTime startTime,
        LocalDateTime endTime) {

    public AdminRatingQueryRequest {
        if (pageNum == null) {
            pageNum = 1;
        }
        if (pageSize == null) {
            pageSize = 20;
        }
    }
}
