package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BatchAuditRequest(
        @NotEmpty(message = "审核列表不能为空") @Size(max = 50, message = "单次最多审核50条") @Valid
        List<AuditItem> items) {

    public record AuditItem(
            @NotNull String productId, @NotNull Integer action, String reason, List<String> dimensions) {}
}
