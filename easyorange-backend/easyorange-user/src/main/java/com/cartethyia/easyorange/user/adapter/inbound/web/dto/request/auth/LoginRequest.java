package com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.user.domain.constant.UserConstant;
import com.cartethyia.easyorange.user.domain.enums.LoginMethod;
import com.cartethyia.easyorange.user.domain.valueobject.LoginCommand;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "登录方式不能为空")
        String loginMethod,
        @NotBlank(message = "账号不能为空")
        String account,
        @JsonProperty("password") String credential
) {
    private static void requirePhoneFormat(String phone) {
        BizRequire.require(
                UserConstant.PHONE_PATTERN.matcher(phone).matches(),
                "手机号格式不正确"
        );
    }

    public LoginMethod getEffectiveLoginMethod() {
        return LoginMethod.fromCode(loginMethod);
    }

    public LoginCommand toCommand() {
        return switch (getEffectiveLoginMethod()) {
            case PASSWORD -> new LoginCommand.PasswordLogin(account, credential);
            case SMS -> {
                requirePhoneFormat(account);
                yield new LoginCommand.SmsLogin(account, credential);
            }
        };
    }
}