package com.cartethyia.easyorange.user.adapter.outbound.cache;

import com.cartethyia.easyorange.user.domain.constant.UserSecurityConstant;
import com.cartethyia.easyorange.user.domain.port.SmsCodePort;
import com.cartethyia.easyorange.user.domain.port.SmsSenderPort;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 实现的短信验证码适配器（it/prod 环境）。
 * <p>
 * 与 {@code MockSmsCodeAdapter} 按 {@code @Profile} 互斥（dev/test ↔ it/prod）。
 */
@Component("redisSmsCodeAdapter")
@Profile({"it", "prod"})
@RequiredArgsConstructor
public class RedisSmsCodeAdapter implements SmsCodePort {

    private static final String SMS_BASE = "eo:user:sms:";
    private static final String CODE_KEY = SMS_BASE + "code:";
    private static final String LIMIT_KEY = SMS_BASE + "limit:";
    private static final String DAILY_KEY = SMS_BASE + "daily:";
    private static final String VERIFY_KEY = SMS_BASE + "verify:";

    private final RedisTemplate<Object, Object> redisTemplate;
    private final SmsSenderPort smsSenderPort;

    @Override
    public boolean send(String phone) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(LIMIT_KEY + phone))) {
            return false;
        }

        Long daily = redisTemplate.opsForValue().increment(DAILY_KEY + phone);
        if (daily != null) {
            if (daily == 1) {
                redisTemplate.expire(DAILY_KEY + phone, 1, TimeUnit.DAYS);
            }
            if (daily > UserSecurityConstant.SMS_MAX_DAILY) {
                return false;
            }
        }

        String code = SmsCodePort.generateCode();
        // 先投递后落码与间隔锁：发送失败时不残留码、不白锁 60 秒
        // （每日配额在投递前已计，失败尝试照扣防刷）
        smsSenderPort.send(phone, code);
        redisTemplate
                .opsForValue()
                .set(CODE_KEY + phone, code, UserSecurityConstant.SMS_CODE_TTL.getSeconds(), TimeUnit.SECONDS);
        redisTemplate
                .opsForValue()
                .set(LIMIT_KEY + phone, "1", UserSecurityConstant.SMS_SEND_INTERVAL.getSeconds(), TimeUnit.SECONDS);

        return true;
    }

    @Override
    public VerifyResult verify(String phone, String code) {
        return evaluate(phone, code, true);
    }

    @Override
    public VerifyResult check(String phone, String code) {
        return evaluate(phone, code, false);
    }

    private VerifyResult evaluate(String phone, String code, boolean consume) {
        if (code == null || code.isBlank()) {
            return VerifyResult.NOT_FOUND;
        }

        Long attempts = redisTemplate.opsForValue().increment(VERIFY_KEY + phone);
        if (attempts != null) {
            if (attempts == 1) {
                redisTemplate.expire(VERIFY_KEY + phone, 10, TimeUnit.MINUTES);
            }
            if (attempts > UserSecurityConstant.SMS_MAX_VERIFY_ATTEMPTS) {
                redisTemplate.delete(CODE_KEY + phone);
                redisTemplate.delete(VERIFY_KEY + phone);
                return VerifyResult.TOO_MANY_ATTEMPTS;
            }
        }

        // GenericJacksonJsonRedisSerializer 反序列化 JSON 字符串恒为 String，直接强转
        // （原 CacheUtils.cast 对 String 目标类型等价于强转，Number→String 本就会失败）
        String stored = (String) redisTemplate.opsForValue().get(CODE_KEY + phone);
        if (stored == null || !stored.equals(code)) {
            return VerifyResult.NOT_FOUND;
        }

        if (consume) {
            redisTemplate.delete(CODE_KEY + phone);
            redisTemplate.delete(VERIFY_KEY + phone);
        }
        return VerifyResult.OK;
    }
}
