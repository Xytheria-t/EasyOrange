package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record AdminReportResponse(
        String reportId,
        String productId,
        String productName,
        String productImage,
        String reporterId,
        String reporterName,
        Integer reasonType,
        String reasonTypeDesc,
        String reason,
        Integer status,
        String statusDesc,
        String handleResult,
        String handleRemark,
        LocalDateTime createTime,
        LocalDateTime handleTime) {}
