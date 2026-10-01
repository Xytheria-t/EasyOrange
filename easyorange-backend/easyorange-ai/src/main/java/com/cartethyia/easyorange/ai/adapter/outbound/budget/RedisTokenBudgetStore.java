package com.cartethyia.easyorange.ai.adapter.outbound.budget;

import com.cartethyia.easyorange.ai.domain.port.TokenBudgetStorePort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * Redis 版 Token 预算存储 — 多实例部署下的日预算口径（内存版每实例各记各的，日限会被放大 N 倍）。key 按
 * 「场景 + 本地日期」隔离，跨天自然从零开始，TTL 只做回收。
 * <p>
 * 三个写操作（预留 / 释放 / 记账）各是一个 Lua 脚本：读用量 → 判限 → 记增量的窗口在脚本内闭合，并发下
 * 日限不会被「并发数 × maxPerCall」突破，EXPIRE 同脚本执行（无 TTL 永久 key 窗口一并关闭）。用
 * {@link StringRedisTemplate} 是因脚本参数要纯数字字符串，JSON 序列化器会把增量写成带类型信息的 JSON。
 * fail-open：预留放行空句柄、读 empty、记账只告警，失败计数（构造期按 op 全集注册）是放行的唯一统计面。
 */
@Slf4j
public class RedisTokenBudgetStore implements TokenBudgetStorePort {

    /** fail-open 操作维度 — {@link #FAIL_OPEN_METRIC} 的封闭 tag 集（构造期全集注册），tag 值是时序契约：改枚举名不改 tag。 */
    private enum FailOpenOp {
        READ("read"),
        WRITE("write"),
        RESERVE("reserve");

        private final String tag;

        FailOpenOp(String tag) {
            this.tag = tag;
        }

        String tag() {
            return tag;
        }
    }

    static final String KEY_PREFIX = "eo:ai:budget:";
    static final String FIELD_INPUT = "input";
    static final String FIELD_OUTPUT = "output";
    static final String FIELD_RESERVED = "reserved";
    private static final String FAIL_OPEN_METRIC = "easyorange.ai.budget.failopen";
    /** 只做回收：key 名带日期，跨天不再读取；当天最后一次写入后 48h 由 Redis 自动清理。 */
    private static final Duration KEY_TTL = Duration.ofHours(48);

    private static final String TTL_SECONDS = String.valueOf(KEY_TTL.toSeconds());

    /**
     * 预留：used(input+output+reserved) + amount 越限返回 -1，否则 HINCRBY reserved 并刷新 TTL，返回已占用总量。
     * limit &lt;= 0 不限。判定式与 {@code TokenBudgetPolicy.exhausted} 是同一条公式（预留在脚本内才原子），
     * 改动两边必须同步。
     */
    private static final RedisScript<Long> RESERVE_SCRIPT = RedisScript.of("""
            local used = tonumber(redis.call('HGET', KEYS[1], 'input') or '0')
                + tonumber(redis.call('HGET', KEYS[1], 'output') or '0')
                + tonumber(redis.call('HGET', KEYS[1], 'reserved') or '0')
            local amount = tonumber(ARGV[1])
            local limit = tonumber(ARGV[2])
            if limit > 0 and used + amount > limit then
                return -1
            end
            redis.call('HINCRBY', KEYS[1], 'reserved', amount)
            redis.call('EXPIRE', KEYS[1], ARGV[3])
            return used + amount
            """, Long.class);

    /** 释放：HINCRBY reserved 负增量并钳在 0（Redis 重启丢 key 后释放会造出负占用，把判定口径拉低）。 */
    private static final RedisScript<Long> RELEASE_SCRIPT = RedisScript.of("""
            local left = redis.call('HINCRBY', KEYS[1], 'reserved', -tonumber(ARGV[1]))
            if left < 0 then
                redis.call('HSET', KEYS[1], 'reserved', 0)
            end
            redis.call('EXPIRE', KEYS[1], ARGV[2])
            return left
            """, Long.class);

    /** 记账：两个非零增量与 EXPIRE 同脚本，记账路径不再有「input 记了、output 没记 / TTL 没挂上」的半笔窗口。 */
    private static final RedisScript<Long> RECORD_SCRIPT = RedisScript.of("""
            if tonumber(ARGV[1]) > 0 then
                redis.call('HINCRBY', KEYS[1], 'input', ARGV[1])
            end
            if tonumber(ARGV[2]) > 0 then
                redis.call('HINCRBY', KEYS[1], 'output', ARGV[2])
            end
            redis.call('EXPIRE', KEYS[1], ARGV[3])
            return 1
            """, Long.class);

    private final ObjectProvider<StringRedisTemplate> redisProvider;

    private final Map<FailOpenOp, Counter> failOpenCounters;

    public RedisTokenBudgetStore(ObjectProvider<StringRedisTemplate> redisProvider, MeterRegistry meterRegistry) {
        this.redisProvider = redisProvider;
        this.failOpenCounters = new EnumMap<>(FailOpenOp.class);
        for (FailOpenOp op : FailOpenOp.values()) {
            failOpenCounters.put(op, meterRegistry.counter(FAIL_OPEN_METRIC, "op", op.tag()));
        }
        log.info("TokenBudgetStorePort: 使用 Redis 版存储（日预算跨实例共享）");
    }

    @Override
    @Nullable
    public TokenReservation tryReserve(String scenario, int amount, int dailyLimit) {
        var redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return TokenReservation.NOOP;
        }
        try {
            Long granted = redis.execute(
                    RESERVE_SCRIPT,
                    List.of(todayKey(scenario)),
                    String.valueOf(amount),
                    String.valueOf(dailyLimit),
                    TTL_SECONDS);
            if (granted == null || granted < 0) {
                return null;
            }
            return () -> release(redis, todayKey(scenario), amount);
        } catch (Exception e) {
            log.warn("Reserve token budget failed, this call proceeds as unused: {}", e.getMessage());
            failOpenCounters.get(FailOpenOp.RESERVE).increment();
            return TokenReservation.NOOP;
        }
    }

    private void release(StringRedisTemplate redis, String key, int amount) {
        try {
            redis.execute(RELEASE_SCRIPT, List.of(key), String.valueOf(amount), TTL_SECONDS);
        } catch (Exception e) {
            log.warn(
                    "Release token reservation failed, reserved stays elevated until day rollover: {}", e.getMessage());
            failOpenCounters.get(FailOpenOp.WRITE).increment();
        }
    }

    @Override
    public Optional<TokenUsage> getTodayUsage(String scenario) {
        var redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return Optional.empty();
        }
        try {
            Map<Object, Object> counters = redis.opsForHash().entries(todayKey(scenario));
            if (counters.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new TokenUsage(
                    parseInt(counters.get(FIELD_INPUT)),
                    parseInt(counters.get(FIELD_OUTPUT)),
                    parseInt(counters.get(FIELD_RESERVED)),
                    System.currentTimeMillis()));
        } catch (Exception e) {
            log.warn("Read today's token usage failed, budget check proceeds as unused: {}", e.getMessage());
            failOpenCounters.get(FailOpenOp.READ).increment();
            return Optional.empty();
        }
    }

    @Override
    public void recordUsage(String scenario, int inputTokens, int outputTokens) {
        var redis = redisProvider.getIfAvailable();
        if (redis == null || (inputTokens <= 0 && outputTokens <= 0)) {
            return;
        }
        try {
            redis.execute(
                    RECORD_SCRIPT,
                    List.of(todayKey(scenario)),
                    String.valueOf(inputTokens),
                    String.valueOf(outputTokens),
                    TTL_SECONDS);
        } catch (Exception e) {
            log.warn("Record token usage failed, this call is not counted: {}", e.getMessage());
            failOpenCounters.get(FailOpenOp.WRITE).increment();
        }
    }

    private static String todayKey(String scenario) {
        return KEY_PREFIX + scenario + ":" + LocalDate.now();
    }

    private static int parseInt(Object raw) {
        return raw == null ? 0 : Integer.parseInt(raw.toString());
    }
}
