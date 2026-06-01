package com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth;

import com.cartethyia.easyorange.user.domain.valueobject.LoginCredential;
import jakarta.validation.constraints.NotBlank;

public record PasswordLoginRequest(
        @NotBlank(message = "账号不能为空")
        String identifier,
        @NotBlank(message = "密码不能为空")
        String password
) {
    public LoginCredential toCredential() {
        return new LoginCredential.Password(identifier, password);
    }
}
