package com.cartethyia.easyorange.product.adapter.inbound.web.dto.response;

import java.time.LocalDateTime;

public record ProductReportDetailResponse(
        String id,
        String productId,
        String productName,
        String reasonType,
        String reasonTypeDesc,
        String reason,
        String status,
        String statusDesc,
        String handleResult,
        LocalDateTime createTime,
        LocalDateTime handleTime) {}
