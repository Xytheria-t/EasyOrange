package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ReportHandleRequest(
        @NotBlank(message = "处理动作不能为空")
        @Pattern(regexp = "^resolve|dismiss|IGNORE|PRODUCT_OFFLINE|WARN_SENDER|BAN_PRODUCT$", message = "无效的处理动作")
        String action,

        String remark) {}
