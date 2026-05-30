package com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth;

import com.cartethyia.easyorange.user.adapter.inbound.web.validation.Phone;
import com.cartethyia.easyorange.user.domain.valueobject.LoginCredential;
import jakarta.validation.constraints.NotBlank;

public record SmsLoginRequest(
        @Phone
        String phone,
        @NotBlank(message = "验证码不能为空")
        String verifyCode
) {
    public LoginCredential toCredential() {
        return new LoginCredential.Sms(phone, verifyCode);
    }
}
