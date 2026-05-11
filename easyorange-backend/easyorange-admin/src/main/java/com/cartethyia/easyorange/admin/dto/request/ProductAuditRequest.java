package com.cartethyia.easyorange.admin.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductAuditRequest {

    @NotNull(message = "审核状态不能为空")
    @Pattern(regexp = "^1|-1$", message = "状态值无效（1-通过，-1-拒绝）")
    private String status;

    @Size(max = 200, message = "审核原因最长200个字符")
    private String reason;
}
