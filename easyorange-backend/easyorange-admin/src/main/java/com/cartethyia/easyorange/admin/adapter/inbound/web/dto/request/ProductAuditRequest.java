package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProductAuditRequest(
    @NotNull(message = "审核动作不能为空") Integer action,
    @Size(max = 500, message = "原因最长500字符") String reason,
    List<String> dimensions,
    @Size(max = 500, message = "备注最长500字符") String remark
) {}