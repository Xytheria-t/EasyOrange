package com.cartethyia.easyorange.user.adapter.inbound.web.dto.response;

public record LoginResult(
    String accessToken,
    String refreshToken,
    UserResponse user
) {}
