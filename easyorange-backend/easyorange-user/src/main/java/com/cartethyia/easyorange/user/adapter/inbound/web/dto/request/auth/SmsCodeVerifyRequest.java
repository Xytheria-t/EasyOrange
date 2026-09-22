package com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth;

import com.cartethyia.easyorange.user.domain.constant.UserConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 验证码预检请求 — 忘记密码第二步校验正确性，不消费验证码。 */
public record SmsCodeVerifyRequest(
        @NotBlank(message = "手机号不能为空") @Pattern(regexp = UserConstant.PHONE_REGEX, message = "手机号格式不正确")
        String phone,

        @NotBlank(message = "验证码不能为空") @Size(min = 6, max = 6, message = "验证码长度必须为6位")
        String verifyCode) {}
