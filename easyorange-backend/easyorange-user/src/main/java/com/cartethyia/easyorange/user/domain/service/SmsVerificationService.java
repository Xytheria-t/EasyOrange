package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.port.SmsCodePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 短信验证码校验领域服务 — 短信登录与密码重置共享的验证码校验逻辑。
 * 将 {@link SmsCodePort#verify} 的结果映射为领域业务异常，避免在多个调用方重复。
 */
@Component
@RequiredArgsConstructor
public class SmsVerificationService {

    private final SmsCodePort smsCodePort;

    public void verifyCodeOrThrow(String phone, String verifyCode) {
        switch (smsCodePort.verify(phone, verifyCode)) {
            case TOO_MANY_ATTEMPTS -> throw BusinessException.of(UserResultCode.SMS_CODE_VERIFY_TOO_FREQUENT);
            case NOT_FOUND -> throw BusinessException.of(UserResultCode.SMS_CODE_INVALID);
            case OK -> {}
        }
    }
}
