package com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth;

import com.cartethyia.easyorange.user.domain.constant.UserConstant;
import com.cartethyia.easyorange.user.domain.valueobject.LoginCredential;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SmsLoginRequest(
        @Pattern(regexp = UserConstant.PHONE_REGEX, message = "手机号格式不正确")
        String phone,
        @NotBlank(message = "验证码不能为空")
        String verifyCode
) {
    public LoginCredential toCredential() {
        return new LoginCredential.Sms(phone, verifyCode);
    }
}
