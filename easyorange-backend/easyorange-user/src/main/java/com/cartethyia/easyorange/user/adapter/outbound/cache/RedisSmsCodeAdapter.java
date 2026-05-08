package com.cartethyia.easyorange.user.adapter.outbound.cache;

import com.cartethyia.easyorange.common.constant.CommonConstant;
import com.cartethyia.easyorange.framework.redis.RedisCache;
import com.cartethyia.easyorange.user.domain.port.output.SmsCodePort;
import com.cartethyia.easyorange.user.domain.port.output.SmsRateLimitPort;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Redis 实现的短信验证码和限流适配器
 * <p>
 * 同时实现两个端口接口：
 * <ul>
 *   <li>{@link SmsCodePort} - 验证码存储</li>
 *   <li>{@link SmsRateLimitPort} - 发送限流</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class RedisSmsCodeAdapter implements SmsCodePort, SmsRateLimitPort {

    private static final String SMS_CODE_PREFIX = CommonConstant.APP_PREFIX + "sms:code:";
    private static final String SMS_LIMIT_PREFIX = CommonConstant.APP_PREFIX + "sms:limit:";
    private static final String DAILY_COUNT_PREFIX = CommonConstant.APP_PREFIX + "sms:daily:";
    private static final String VERIFY_COUNT_PREFIX = CommonConstant.APP_PREFIX + "sms:verify:";

    private final RedisCache redisCache;

    // ========== SmsCodePort 实现 ==========

    @Override
    public void save(String phone, String code, Duration ttl) {
        redisCache.set(SMS_CODE_PREFIX + phone, code, ttl.toMinutes(), TimeUnit.MINUTES);
    }

    @Override
    public String get(String phone) {
        return redisCache.get(SMS_CODE_PREFIX + phone, String.class);
    }

    @Override
    public void delete(String phone) {
        redisCache.delete(SMS_CODE_PREFIX + phone);
        clearVerifyCount(phone);
    }

    // ========== SmsRateLimitPort 实现 ==========

    @Override
    public boolean isSendLimited(String phone) {
        return Boolean.TRUE.equals(redisCache.hasKey(SMS_LIMIT_PREFIX + phone));
    }

    @Override
    public void setSendInterval(String phone, Duration interval) {
        redisCache.set(SMS_LIMIT_PREFIX + phone, "1", interval.getSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public long incrementDailyCount(String phone) {
        Long count = redisCache.increment(DAILY_COUNT_PREFIX + phone);
        return count != null ? count : 0;
    }

    @Override
    public void expireDailyCount(String phone, Duration ttl) {
        redisCache.expire(DAILY_COUNT_PREFIX + phone, ttl.toDays(), TimeUnit.DAYS);
    }

    @Override
    public long incrementVerifyCount(String phone) {
        Long count = redisCache.increment(VERIFY_COUNT_PREFIX + phone);
        return count != null ? count : 0;
    }

    @Override
    public void expireVerifyCount(String phone, Duration ttl) {
        redisCache.expire(VERIFY_COUNT_PREFIX + phone, ttl.toMinutes(), TimeUnit.MINUTES);
    }

    @Override
    public void clearVerifyCount(String phone) {
        redisCache.delete(VERIFY_COUNT_PREFIX + phone);
    }
}
