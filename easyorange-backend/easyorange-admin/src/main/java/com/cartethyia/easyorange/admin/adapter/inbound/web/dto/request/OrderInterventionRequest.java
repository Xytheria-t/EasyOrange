package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record OrderInterventionRequest(
        @NotBlank(message = "操作原因不能为空") String reason) {}
