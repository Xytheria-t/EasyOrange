package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.constant.CommonConstant;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.redis.RedisCache;
import com.cartethyia.easyorange.user.common.enums.UserResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsCodeService {

    private static final String SMS_CODE_PREFIX = CommonConstant.APP_PREFIX + "sms:code:";
    private static final String SMS_LIMIT_PREFIX = CommonConstant.APP_PREFIX + "sms:limit:";
    private static final int CODE_LENGTH = 6;
    private static final int CODE_EXPIRE_MINUTES = 5;
    private static final int SEND_INTERVAL_SECONDS = 60;
    private static final int MAX_DAILY_SEND_COUNT = 10;
    private static final String DAILY_COUNT_PREFIX = CommonConstant.APP_PREFIX + "sms:daily:";

    private final RedisCache redisCache;

    public void sendCode(String phone) {
        String limitKey = SMS_LIMIT_PREFIX + phone;
        if (Boolean.TRUE.equals(redisCache.hasKey(limitKey))) {
            throw BusinessException.of(UserResultCode.SMS_CODE_SEND_TOO_FREQUENT);
        }

        String dailyCountKey = DAILY_COUNT_PREFIX + phone;
        Long dailyCount = redisCache.increment(dailyCountKey);
        if (dailyCount != null && dailyCount == 1) {
            redisCache.expire(dailyCountKey, 1, TimeUnit.DAYS);
        }
        if (dailyCount != null && dailyCount > MAX_DAILY_SEND_COUNT) {
            throw BusinessException.of("验证码发送次数已达每日上限");
        }

        String code = generateCode();
        String codeKey = SMS_CODE_PREFIX + phone;
        redisCache.set(codeKey, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        redisCache.set(limitKey, "1", SEND_INTERVAL_SECONDS, TimeUnit.SECONDS);

        log.info("action=sendSmsCode, phone={}, code={}", phone, code);
    }

    public void verifyCode(String phone, String code) {
        if (code == null || code.isBlank()) {
            throw BusinessException.of(UserResultCode.SMS_CODE_INVALID);
        }

        String codeKey = SMS_CODE_PREFIX + phone;
        String storedCode = redisCache.get(codeKey, String.class);

        if (storedCode == null || !storedCode.equals(code)) {
            throw BusinessException.of(UserResultCode.SMS_CODE_INVALID);
        }

        redisCache.delete(codeKey);
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
