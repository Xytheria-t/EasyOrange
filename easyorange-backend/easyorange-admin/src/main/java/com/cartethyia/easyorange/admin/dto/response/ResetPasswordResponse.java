package com.cartethyia.easyorange.admin.dto.response;

import lombok.Builder;

@Builder
public record ResetPasswordResponse(
    String newPassword,
    String message
) {}