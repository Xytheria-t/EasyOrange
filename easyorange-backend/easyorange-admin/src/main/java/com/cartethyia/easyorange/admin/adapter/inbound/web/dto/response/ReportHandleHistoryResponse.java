package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record ReportHandleHistoryResponse(
        String id,
        String reportId,
        String operatorName,
        String action,
        String actionDesc,
        String remark,
        LocalDateTime createTime) {}
