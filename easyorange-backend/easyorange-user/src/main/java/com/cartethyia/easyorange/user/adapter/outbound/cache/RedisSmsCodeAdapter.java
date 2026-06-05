package com.cartethyia.easyorange.user.adapter.outbound.cache;

import com.cartethyia.easyorange.common.constant.CommonConstant;
import com.cartethyia.easyorange.framework.cache.RedisCache;
import com.cartethyia.easyorange.user.domain.port.SmsCodePort;
import com.cartethyia.easyorange.user.domain.port.SmsSenderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Redis 实现的短信验证码适配器。
 */
@Component("redisSmsCodeAdapter")
@RequiredArgsConstructor
public class RedisSmsCodeAdapter implements SmsCodePort {

    private static final String SMS_BASE = CommonConstant.APP_PREFIX + "sms:";
    private static final String CODE_KEY = SMS_BASE + "code:";
    private static final String LIMIT_KEY = SMS_BASE + "limit:";
    private static final String DAILY_KEY = SMS_BASE + "daily:";
    private static final String VERIFY_KEY = SMS_BASE + "verify:";

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration SEND_INTERVAL = Duration.ofSeconds(60);
    private static final long MAX_DAILY = 10;
    private static final long MAX_VERIFY_ATTEMPTS = 5;

    private final RedisCache redisCache;
    private final SmsSenderPort smsSenderPort;

    @Override
    public boolean send(String phone) {
        if (Boolean.TRUE.equals(redisCache.hasKey(LIMIT_KEY + phone))) {
            return false;
        }

        Long daily = redisCache.increment(DAILY_KEY + phone);
        if (daily != null && daily == 1) {
            redisCache.expire(DAILY_KEY + phone, 1, TimeUnit.DAYS);
        }
        if (daily != null && daily > MAX_DAILY) {
            return false;
        }

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        redisCache.set(CODE_KEY + phone, code, CODE_TTL.getSeconds(), TimeUnit.SECONDS);
        redisCache.set(LIMIT_KEY + phone, "1", SEND_INTERVAL.getSeconds(), TimeUnit.SECONDS);

        smsSenderPort.send(phone, code);
        return true;
    }

    @Override
    public VerifyResult verify(String phone, String code) {
        if (code == null || code.isBlank()) {
            return VerifyResult.NOT_FOUND;
        }

        Long attempts = redisCache.increment(VERIFY_KEY + phone);
        if (attempts != null && attempts == 1) {
            redisCache.expire(VERIFY_KEY + phone, 10, TimeUnit.MINUTES);
        }
        if (attempts != null && attempts > MAX_VERIFY_ATTEMPTS) {
            redisCache.delete(CODE_KEY + phone);
            redisCache.delete(VERIFY_KEY + phone);
            return VerifyResult.TOO_MANY_ATTEMPTS;
        }

        String stored = redisCache.get(CODE_KEY + phone, String.class);
        if (stored == null || !stored.equals(code)) {
            return VerifyResult.NOT_FOUND;
        }

        redisCache.delete(CODE_KEY + phone);
        redisCache.delete(VERIFY_KEY + phone);
        return VerifyResult.OK;
    }
}
