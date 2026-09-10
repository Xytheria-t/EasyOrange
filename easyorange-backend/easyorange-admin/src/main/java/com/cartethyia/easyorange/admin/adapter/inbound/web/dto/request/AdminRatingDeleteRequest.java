package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AdminRatingDeleteRequest(
        @NotBlank(message = "删除原因不能为空") String reason) {}
