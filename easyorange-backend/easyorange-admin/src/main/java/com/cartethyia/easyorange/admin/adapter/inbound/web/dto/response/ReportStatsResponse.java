package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import lombok.Builder;

@Builder
public record ReportStatsResponse(
    long totalReports,
    long pendingReports,
    long processingReports,
    long resolvedReports,
    long dismissedReports
) {}