package com.cartethyia.easyorange.admin.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class BatchAuditRequest {

    @NotEmpty(message = "审核列表不能为空")
    @Size(max = 50, message = "单次最多审核50条")
    private List<AuditItem> items;

    public record AuditItem(
        @NotNull Long productId,
        @NotNull Integer status,
        String reason
    ) {}
}
