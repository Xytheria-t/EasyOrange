package com.cartethyia.easyorange.admin.dto.response;

import lombok.Builder;

@Builder
public record ResetPasswordVO(
    String newPassword,
    String message
) {}
