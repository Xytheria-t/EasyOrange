package com.cartethyia.easyorange.user.adapter.inbound.web.dto.response;

public record LoginResult(
    String token,
    String refreshToken,
    UserResponse user
) {}
