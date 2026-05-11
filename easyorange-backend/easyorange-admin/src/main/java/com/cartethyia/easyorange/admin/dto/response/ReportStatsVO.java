package com.cartethyia.easyorange.admin.dto.response;

import lombok.Builder;

@Builder
public record ReportStatsVO(
    long totalReports,
    long pendingReports,
    long resolvedReports,
    long dismissedReports
) {}
