package com.cartethyia.easyorange.admin.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ReportHandleHistoryResponse(
    Long id,
    Long reportId,
    String operatorName,
    String action,
    String actionDesc,
    String remark,
    LocalDateTime createTime
) {}