package com.cartethyia.easyorange.user.adapter.inbound.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
    @NotBlank(message = "Refresh Token 不能为空")
    String refreshToken
) {}
