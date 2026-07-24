package com.cartethyia.easyorange.product.adapter.inbound.web.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReportRequest(
        @NotNull(message = "举报类型不能为空") String reasonType,
        @NotBlank(message = "举报描述不能为空") @Size(max = 500, message = "举报描述不能超过500字") String reason
) {}
