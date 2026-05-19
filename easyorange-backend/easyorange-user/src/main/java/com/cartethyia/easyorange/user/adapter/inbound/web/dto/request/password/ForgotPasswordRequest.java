package com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.password;

import com.cartethyia.easyorange.user.adapter.inbound.web.validation.Password;
import com.cartethyia.easyorange.user.adapter.inbound.web.validation.Phone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(
    @NotBlank(message = "手机号不能为空")
    @Phone
    String phone,

    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码长度必须为6位")
    String verifyCode,

    @NotBlank(message = "新密码不能为空")
    @Password
    String newPassword
) {}