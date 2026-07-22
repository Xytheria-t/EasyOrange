package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record AuditLogResponse(
    String id,
    String productId,
    String operatorId,
    String operatorName,
    Integer action,
    String actionDesc,
    String reason,
    List<String> dimensions,
    String beforeStatus,
    String beforeStatusDesc,
    String afterStatus,
    String afterStatusDesc,
    String remark,
    LocalDateTime createTime
) {}