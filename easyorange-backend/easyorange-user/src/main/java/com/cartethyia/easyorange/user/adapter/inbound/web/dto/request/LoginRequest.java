package com.cartethyia.easyorange.user.adapter.inbound.web.dto.request;

import com.cartethyia.easyorange.user.domain.shared.enums.ClientType;
import com.cartethyia.easyorange.user.domain.shared.enums.LoginMethod;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    String clientType,
    String loginMethod,
    @NotBlank(message = "账号不能为空")
    String account,
    @NotBlank(message = "凭证不能为空")
    String password
) {
    public LoginMethod getEffectiveLoginMethod() {
        return LoginMethod.fromValue(loginMethod);
    }

    public ClientType getEffectiveClientType() {
        return ClientType.fromCode(clientType);
    }
}
