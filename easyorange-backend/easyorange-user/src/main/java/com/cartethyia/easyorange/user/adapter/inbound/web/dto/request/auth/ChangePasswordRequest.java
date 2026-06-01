package com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth;

import com.cartethyia.easyorange.user.adapter.inbound.web.validation.Password;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码长度必须为6位")
    String verifyCode,

    @NotBlank(message = "新密码不能为空")
    @Password
    String newPassword
) { }
