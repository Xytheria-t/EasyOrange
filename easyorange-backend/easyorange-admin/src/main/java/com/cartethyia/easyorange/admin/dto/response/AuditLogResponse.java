package com.cartethyia.easyorange.admin.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record AuditLogResponse(
    Long id,
    Long productId,
    Long operatorId,
    String operatorName,
    Integer action,
    String actionDesc,
    String reason,
    List<String> dimensions,
    Integer beforeStatus,
    String beforeStatusDesc,
    Integer afterStatus,
    String afterStatusDesc,
    String remark,
    LocalDateTime createTime
) {}