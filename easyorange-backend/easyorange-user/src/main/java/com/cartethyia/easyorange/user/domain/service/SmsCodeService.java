package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.port.SmsCodePort;
import com.cartethyia.easyorange.user.domain.port.SmsRateLimitPort;
import com.cartethyia.easyorange.user.domain.port.SmsSenderPort;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@RequiredArgsConstructor
public class SmsCodeService {

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration SEND_INTERVAL = Duration.ofSeconds(60);
    private static final int MAX_DAILY_SEND_COUNT = 10;
    private static final int MAX_VERIFY_ATTEMPTS = 5;

    private final SmsCodePort smsCodePort;
    private final SmsRateLimitPort rateLimitPort;
    private final SmsSenderPort smsSenderPort;

    public void sendCode(String phone) {
        if (rateLimitPort.isSendLimited(phone)) {
            throw BusinessException.of(UserResultCode.SMS_CODE_SEND_TOO_FREQUENT);
        }

        long dailyCount = rateLimitPort.incrementDailyCount(phone);
        if (dailyCount > MAX_DAILY_SEND_COUNT) {
            throw BusinessException.of(UserResultCode.SMS_CODE_SEND_TOO_FREQUENT);
        }

        String code = generateCode();
        smsCodePort.save(phone, code, CODE_TTL);
        rateLimitPort.setSendInterval(phone, SEND_INTERVAL);

        // 通过 SmsSenderPort 将验证码投递到用户手机
        // 当前使用 MockSmsSenderAdapter（日志输出），生产环境替换为真实短信服务商实现
        smsSenderPort.send(phone, code);
    }

    public void verifyCode(String phone, String code) {
        if (code == null || code.isBlank()) {
            throw BusinessException.of(UserResultCode.SMS_CODE_INVALID);
        }

        long verifyCount = rateLimitPort.incrementVerifyCount(phone);
        if (verifyCount > MAX_VERIFY_ATTEMPTS) {
            smsCodePort.delete(phone);
            throw BusinessException.of(UserResultCode.SMS_CODE_VERIFY_TOO_FREQUENT);
        }

        String storedCode = smsCodePort.get(phone);

        if (storedCode == null || !storedCode.equals(code)) {
            throw BusinessException.of(UserResultCode.SMS_CODE_INVALID);
        }

        smsCodePort.delete(phone);
    }

    private String generateCode() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }
}
