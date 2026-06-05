package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import lombok.Builder;

@Builder
public record ResetPasswordResponse(
    String newPassword,
    String message
) {}