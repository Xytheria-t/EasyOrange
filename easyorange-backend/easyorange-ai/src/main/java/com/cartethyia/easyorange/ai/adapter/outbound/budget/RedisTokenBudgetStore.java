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
 * Redis 版 Token 预算存储 — 多实例部署下的日预算口径（内存版每实例各记各的，日限会被放大 N 倍）。
 * key 按「场景 + 本地日期」隔离，{@code HINCRBY} 原子累加；判定式 {@code used + maxPerCall > dailyLimit}
 * 在 {@code TokenBudgetAspect} / {@code ToolCallLoop}，本类只负责存取。key 名自带日期，跨天自然
 * 从零开始，TTL 只做回收。
 * <p>
 * <b>fail-open</b>：Redis 不可用或读写异常时读返回 empty、写只告警 —— 记账失败不该让对话不可用，
 * 代价是这段时间按「今日未用量」放行；fail-open 只 log 会隐身，失败计数（meter 构造期按 op 全集
 * 一次注册）是「按未用量放行」的唯一统计面。
 * <p>
 * 记账是 input / output 两次 {@code HINCRBY} 后跟一次 {@code expire}，<b>刻意不包进事务</b>：
 * 崩在两次 HINCRBY 之间会留下「input 记了、output 没记」的半笔账（日预算少计，
 * 方向是少拦不是多拦）。为这点精度给每次 LLM 调用付一次 MULTI/EXEC 的往返不划算 ——
 * 日预算的用途是兜住量级，不是计费对账（计费对账看 {@code eo_ai_call_log}）。
 * <p>
 * 用 {@link StringRedisTemplate}：{@code HINCRBY} 要纯数字字符串，JSON 序列化器会把增量写成带类型
 * 信息的 JSON（限流器曾因序列化器让 Lua ARGV 变二进制）。由 {@code AiConfig} 在
 * {@code easyorange.ai.budget.store=redis} 时注册（默认内存版）。
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
