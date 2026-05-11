package com.cartethyia.easyorange.admin.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AdminReportVO(
    Long reportId,
    Long productId,
    String productName,
    String productImage,
    Long reporterId,
    String reporterName,
    String reason,
    Integer status,
    String statusDesc,
    String handleResult,
    String handleRemark,
    LocalDateTime createTime,
    LocalDateTime handleTime
) {}
