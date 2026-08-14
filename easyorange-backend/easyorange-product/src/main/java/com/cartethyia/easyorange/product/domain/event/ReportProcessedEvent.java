package com.cartethyia.easyorange.product.domain.event;

import java.time.LocalDateTime;

public record ReportProcessedEvent(
        String eventId,
        String reportId,
        String reporterId,
        String productId,
        boolean approved,
        String remark,
        LocalDateTime processedTime)
        implements ProductEvent {}
