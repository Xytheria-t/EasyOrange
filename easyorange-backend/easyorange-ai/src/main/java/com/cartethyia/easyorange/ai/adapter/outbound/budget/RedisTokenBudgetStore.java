package com.cartethyia.easyorange.ai.adapter.outbound.budget;

import com.cartethyia.easyorange.ai.domain.port.TokenBudgetStorePort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 版 Token 预算存储 — 多实例部署下的日预算口径（内存版每实例各记各的，日限会被放大 N 倍）。key 按
 * 「场景 + 本地日期」隔离，{@code HINCRBY} 原子累加，跨天自然从零开始，TTL 只做回收；判定式在
 * {@code TokenBudgetAspect} / {@code ToolCallLoop}，本类只负责存取。
 * <p>
 * fail-open：Redis 不可用或读写异常时读返回 empty、写只告警 —— 记账失败不该让对话不可用，代价是这段时间按
 * 「今日未用量」放行。失败计数（meter 构造期按 op 全集一次注册）是这一放行的唯一统计面，只 log 会隐身。
 * <p>
 * 记账刻意不包事务：崩在两次 {@code HINCRBY} 之间只留下「input 记了、output 没记」的半笔账，方向是少拦不是
 * 多拦，而日预算兜的是量级不是计费对账（对账看 {@code eo_ai_call_log}）。用 {@link StringRedisTemplate} 是因
 * {@code HINCRBY} 要纯数字字符串，JSON 序列化器会把增量写成带类型信息的 JSON（限流器的 Lua ARGV 曾因此变二进制）。
 */
@Slf4j
public class RedisTokenBudgetStore implements TokenBudgetStorePort {

    /** fail-open 操作维度 — {@link #FAIL_OPEN_METRIC} 的封闭 tag 集（构造期全集注册），tag 值是时序契约：改枚举名不改 tag。 */
    private enum FailOpenOp {
        READ("read"),
        WRITE("write");

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
    private static final String FAIL_OPEN_METRIC = "easyorange.ai.budget.failopen";
    /** 只做回收：key 名带日期，跨天不再读取；当天最后一次写入后 48h 由 Redis 自动清理。 */
    private static final Duration KEY_TTL = Duration.ofHours(48);

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
            String key = todayKey(scenario);
            if (inputTokens > 0) {
                redis.opsForHash().increment(key, FIELD_INPUT, inputTokens);
            }
            if (outputTokens > 0) {
                redis.opsForHash().increment(key, FIELD_OUTPUT, outputTokens);
            }
            redis.expire(key, KEY_TTL);
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
