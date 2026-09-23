package com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth;

import com.cartethyia.easyorange.user.adapter.inbound.web.validation.Password;
import com.cartethyia.easyorange.user.adapter.inbound.web.validation.Username;
import com.cartethyia.easyorange.user.domain.constant.UserConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
        @Username String username,

        @NotBlank(message = "密码不能为空") @Password String password,

        @NotBlank(message = "手机号不能为空") @Pattern(regexp = UserConstant.PHONE_REGEX, message = "手机号格式不正确")
        String phone,

        @NotBlank(message = "验证码不能为空") @Pattern(regexp = "\\d{6}", message = "验证码为6位数字")
        String verifyCode) {}
