package com.cartethyia.easyorange.user.adapter.inbound.web.dto.request;

import com.cartethyia.easyorange.user.domain.shared.constant.UserConstant;
import com.cartethyia.easyorange.user.domain.shared.enums.ClientType;
import com.cartethyia.easyorange.user.domain.shared.enums.LoginMethod;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    String clientType,
    String loginMethod,
    @Size(min = UserConstant.USERNAME_MIN_LENGTH, max = UserConstant.USERNAME_MAX_LENGTH,
            message = "账号长度必须在" + UserConstant.USERNAME_MIN_LENGTH + "-" + UserConstant.USERNAME_MAX_LENGTH + "位之间")
    String account,
    @Size(min = 6, max = 20, message = "密码长度必须在 6-20 位之间")
    String password
) {
    public LoginMethod getEffectiveLoginMethod() {
        return LoginMethod.fromValue(loginMethod);
    }

    public ClientType getEffectiveClientType() {
        return ClientType.fromCode(clientType);
    }
}
