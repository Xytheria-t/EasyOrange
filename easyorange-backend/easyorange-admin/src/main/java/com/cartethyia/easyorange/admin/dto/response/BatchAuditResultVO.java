package com.cartethyia.easyorange.admin.dto.response;

import java.util.List;

public record BatchAuditResultVO(
    int total,
    int success,
    int failed,
    List<String> errors
) {}
