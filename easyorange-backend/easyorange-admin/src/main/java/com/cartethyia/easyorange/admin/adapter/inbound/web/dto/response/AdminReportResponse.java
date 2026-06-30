package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

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
    LocalDateTime handleTime
) {}