package com.cartethyia.easyorange.product.domain.event;

import java.time.LocalDateTime;

public record ReportProcessedEvent(
    Long reportId,
    Long reporterId,
    Long productId,
    boolean approved,
    String remark,
    LocalDateTime processedTime
) {}
