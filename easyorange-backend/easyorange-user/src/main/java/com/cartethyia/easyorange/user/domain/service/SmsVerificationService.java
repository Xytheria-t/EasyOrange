package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.port.SmsCodePort;
import lombok.RequiredArgsConstructor;

/**
 * 短信验证码校验领域服务 — 短信登录与密码重置共享的验证码校验逻辑。
 * 将 {@link SmsCodePort#verify} 的结果映射为领域业务异常，避免在多个调用方重复。
 */
@RequiredArgsConstructor
public class SmsVerificationService {

    private final SmsCodePort smsCodePort;

    public void verifyCodeOrThrow(String phone, String verifyCode) {
        throwIfInvalid(smsCodePort.verify(phone, verifyCode));
    }

    /** 预检验证码（不消费）— 通过后最终仍由 {@link #verifyCodeOrThrow} 消费。 */
    public void checkCodeOrThrow(String phone, String verifyCode) {
        throwIfInvalid(smsCodePort.check(phone, verifyCode));
    }

    private void throwIfInvalid(SmsCodePort.VerifyResult result) {
        switch (result) {
            case TOO_MANY_ATTEMPTS -> throw BusinessException.of(UserResultCode.SMS_CODE_VERIFY_TOO_FREQUENT);
            case NOT_FOUND -> throw BusinessException.of(UserResultCode.SMS_CODE_INVALID);
            case OK -> {}
        }
    }
}
