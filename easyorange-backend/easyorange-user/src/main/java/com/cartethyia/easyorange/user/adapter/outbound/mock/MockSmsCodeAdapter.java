package com.cartethyia.easyorange.user.adapter.outbound.mock;

import com.cartethyia.easyorange.user.domain.port.SmsCodePort;
import com.cartethyia.easyorange.user.domain.port.SmsRateLimitPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存实现的验证码存储和限流适配器（开发/测试环境）。
 * <p>
 * 不依赖 Redis，所有数据存储在内存中。应用重启后数据丢失（符合开发预期）。
 * TTL 通过记录创建时间 + 懒过期检查实现。
 * <p>
 * 当存在 {@code redisSmsCodeAdapter} Bean 时自动跳过（{@link ConditionalOnMissingBean}），
 * 适用于生产环境切换为 Redis 实现。
 */
@Component
@ConditionalOnMissingBean(name = "redisSmsCodeAdapter")
public class MockSmsCodeAdapter implements SmsCodePort, SmsRateLimitPort {

    private static final Logger log = LoggerFactory.getLogger(MockSmsCodeAdapter.class);

    private final ConcurrentHashMap<String, CodeEntry> codes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> sendLimits = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> dailyCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> verifyCounts = new ConcurrentHashMap<>();

    // ========== SmsCodePort ==========

    @Override
    public void save(String phone, String code, Duration ttl) {
        codes.put(phone, new CodeEntry(code, Instant.now(), ttl));
    }

    @Override
    public String get(String phone) {
        CodeEntry entry = codes.get(phone);
        if (entry == null) {
            return null;
        }
        if (Instant.now().isAfter(entry.createdAt.plus(entry.ttl))) {
            codes.remove(phone);
            return null;
        }
        return entry.code;
    }

    @Override
    public void delete(String phone) {
        codes.remove(phone);
        clearVerifyCount(phone);
    }

    // ========== SmsRateLimitPort ==========

    @Override
    public boolean isSendLimited(String phone) {
        Instant blockedUntil = sendLimits.get(phone);
        if (blockedUntil == null) {
            return false;
        }
        if (Instant.now().isBefore(blockedUntil)) {
            return true;
        }
        sendLimits.remove(phone);
        return false;
    }

    @Override
    public void setSendInterval(String phone, Duration interval) {
        sendLimits.put(phone, Instant.now().plus(interval));
    }

    @Override
    public long incrementDailyCount(String phone) {
        return dailyCounts.merge(phone, 1L, Long::sum);
    }

    @Override
    public long incrementVerifyCount(String phone) {
        return verifyCounts.merge(phone, 1L, Long::sum);
    }

    @Override
    public void clearVerifyCount(String phone) {
        verifyCounts.remove(phone);
    }

    private record CodeEntry(String code, Instant createdAt, Duration ttl) {}
}
