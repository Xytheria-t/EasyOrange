package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.port.SmsCodePort;
import com.cartethyia.easyorange.user.domain.port.SmsRateLimitPort;
import com.cartethyia.easyorange.user.domain.shared.enums.UserResultCode;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 短信验证码领域服务
 * <p>
 * 职责：
 * <ul>
 *   <li>验证码生成和发送</li>
 *   <li>验证码校验</li>
 *   <li>发送频率和配额控制</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SmsCodeDomainService {

    private static final int CODE_LENGTH = 6;
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration SEND_INTERVAL = Duration.ofSeconds(60);
    private static final int MAX_DAILY_SEND_COUNT = 10;
    private static final int MAX_VERIFY_ATTEMPTS = 5;

    private final SmsCodePort smsCodePort;
    private final SmsRateLimitPort rateLimitPort;

    /**
     * 发送验证码
     * 
     * @param phone 手机号
     * @throws BusinessException 当发送受限时抛出
     */
    public void sendCode(String phone) {
        if (rateLimitPort.isSendLimited(phone)) {
            throw BusinessException.of(UserResultCode.SMS_CODE_SEND_TOO_FREQUENT);
        }

        long dailyCount = rateLimitPort.incrementDailyCount(phone);
        if (dailyCount == 1) {
            rateLimitPort.expireDailyCount(phone, Duration.ofDays(1));
        }
        if (dailyCount > MAX_DAILY_SEND_COUNT) {
            throw BusinessException.of(UserResultCode.SMS_CODE_SEND_TOO_FREQUENT);
        }

        String code = generateCode();
        smsCodePort.save(phone, code, CODE_TTL);
        rateLimitPort.setSendInterval(phone, SEND_INTERVAL);
    }

    /**
     * 验证验证码
     * 
     * @param phone 手机号
     * @param code 验证码
     * @throws BusinessException 当验证失败时抛出
     */
    public void verifyCode(String phone, String code) {
        if (code == null || code.isBlank()) {
            throw BusinessException.of(UserResultCode.SMS_CODE_INVALID);
        }

        long verifyCount = rateLimitPort.incrementVerifyCount(phone);
        if (verifyCount == 1) {
            rateLimitPort.expireVerifyCount(phone, CODE_TTL);
        }
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
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
