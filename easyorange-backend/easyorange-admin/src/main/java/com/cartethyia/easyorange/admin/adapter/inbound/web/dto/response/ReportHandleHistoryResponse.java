package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ReportHandleHistoryResponse(
    String id,
    String reportId,
    String operatorName,
    String action,
    String actionDesc,
    String remark,
    LocalDateTime createTime
) {}