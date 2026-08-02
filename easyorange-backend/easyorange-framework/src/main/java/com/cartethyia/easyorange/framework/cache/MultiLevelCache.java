package com.cartethyia.easyorange.framework.cache;

import com.github.benmanes.caffeine.cache.Cache;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.EnumMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 多级缓存 — L1（Caffeine 本地）+ L2（Redis 共享）。
 * <p>
 * 工程化要点：
 * <ul>
 *   <li><b>缓存击穿防护</b>：回源走 {@code Caffeine.get(key, mappingFunction)} 原子单飞，同节点同 key
 *       仅一个线程回源；配置了 {@link RedissonClient} 时再加跨节点分布式锁（锁超时/异常 fail-open）。</li>
 *   <li><b>缓存穿透防护（负缓存）</b>：回源结果为 {@code null} 时以 {@link NullValue} 哨兵写入 L1+L2
 *       （短 TTL），后续请求直接命中哨兵返回 null，避免热点空 key 反复打库/打 LLM。</li>
 *   <li><b>L1/L2 TTL 配平</b>：{@link Config} 强制 {@code l1Ttl <= l2Ttl}，L1 先于 L2 过期，
 *       杜绝 L2 已过期而 L1 仍返回陈旧值的窗口。</li>
 *   <li><b>可观测性</b>：Micrometer 指标 {@code easyorange.cache.requests{result}}（l1_hit/l2_hit/l2_negative/load）
 *       + {@code easyorange.cache.load} 回源耗时 Timer。</li>
 *   <li><b>跨节点 L1 一致性</b>：{@link #evict(String)} / {@link #put(String, Object)} 经
 *       {@link CacheInvalidationListener} 发布 Redis Pub/Sub 失效消息，其他节点收到后失效本地 L1。</li>
 * </ul>
 * <p>
 * {@link #get(String, Class, Supplier)} 触发的回源填充<b>不</b>发布失效消息——
 * 因为回源填充的是新值，不涉及其他节点 L1 的陈旧数据。
 */
public class MultiLevelCache {

    private static final String LOCK_PREFIX = "eo:cache:lock:";

    /** 缓存配置 — 单一值对象，替代伸缩构造器。l1Ttl/negativeTtl 默认值在紧凑构造器中补齐并校验。 */
    public record Config(
            String keyPrefix,
            Duration l1Ttl,
            Duration l2Ttl,
            Duration negativeTtl,
            Duration lockWait,
            Duration lockLease) {

        public Config {
            if (keyPrefix == null || keyPrefix.isBlank()) {
                throw new IllegalArgumentException("keyPrefix must not be blank");
            }
            if (l2Ttl == null) {
                throw new IllegalArgumentException("l2Ttl must not be null");
            }
            l1Ttl = l1Ttl == null ? l2Ttl : l1Ttl;
            negativeTtl = negativeTtl == null ? Duration.ofSeconds(30) : negativeTtl;
            lockWait = lockWait == null ? Duration.ofSeconds(2) : lockWait;
            lockLease = lockLease == null ? Duration.ofSeconds(10) : lockLease;
            if (l1Ttl.compareTo(l2Ttl) > 0) {
                throw new IllegalArgumentException("l1Ttl must be <= l2Ttl, got l1=" + l1Ttl + ", l2=" + l2Ttl);
            }
            if (negativeTtl.compareTo(l2Ttl) > 0) {
                throw new IllegalArgumentException("negativeTtl must be <= l2Ttl, got negative=" + negativeTtl + ", l2=" + l2Ttl);
            }
        }

        /** 便捷工厂 — 仅指定 key 前缀与 L2 TTL，其余取默认值（l1Ttl=l2Ttl）。 */
        public static Config of(String keyPrefix, Duration l2Ttl) {
            return new Config(keyPrefix, null, l2Ttl, null, null, null);
        }

        /** 便捷工厂 — 指定 L1/L2/负缓存 TTL，锁参数取默认值（lockWait=2s、lockLease=10s）。 */
        public static Config of(String keyPrefix, Duration l1Ttl, Duration l2Ttl, Duration negativeTtl) {
            return new Config(keyPrefix, l1Ttl, l2Ttl, negativeTtl, null, null);
        }
    }

    /** 负缓存哨兵 — 代表「null 被缓存」。经 Redis 反序列化后是独立实例，判断须用 instanceof。 */
    public static final class NullValue {
        public NullValue() {
        }

        @Override
        public String toString() {
            return "NULL_VALUE";
        }
    }

    private enum Result {
        L1_HIT("l1_hit"), L2_HIT("l2_hit"), L2_NEGATIVE("l2_negative"), LOAD("load");

        private final String tag;

        Result(String tag) {
            this.tag = tag;
        }

        String tag() {
            return tag;
        }
    }

    private final Cache<String, Object> l1Cache;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final Config config;
    /** 跨节点 L1 失效广播，可为 null（测试场景或单节点部署） */
    private final CacheInvalidationListener invalidationListener;
    /** 跨节点回源单飞锁，可为 null（测试场景 / 无 Redisson 时降级为纯 JVM 单飞） */
    private final RedissonClient redissonClient;
    private final EnumMap<Result, Counter> requestCounters;
    private final Timer loadTimer;

    /** 便捷构造器 — 无广播 / 无分布式锁 / 无指标（测试场景或单节点部署）。 */
    public MultiLevelCache(Cache<String, Object> l1Cache, RedisTemplate<Object, Object> redisTemplate, Config config) {
        this(l1Cache, redisTemplate, config, null, null, null);
    }

    /**
     * 完整构造器。
     *
     * @param invalidationListener 跨节点失效广播器，null 表示不参与广播（单节点 / 测试场景）
     * @param redissonClient       跨节点回源单飞锁，null 表示仅 JVM 内单飞
     * @param meterRegistry        指标注册表，null 表示不记录指标
     */
    public MultiLevelCache(
            Cache<String, Object> l1Cache,
            RedisTemplate<Object, Object> redisTemplate,
            Config config,
            CacheInvalidationListener invalidationListener,
            RedissonClient redissonClient,
            MeterRegistry meterRegistry) {
        this.l1Cache = l1Cache;
        this.redisTemplate = redisTemplate;
        this.config = config;
        this.invalidationListener = invalidationListener;
        this.redissonClient = redissonClient;
        this.requestCounters = new EnumMap<>(Result.class);
        if (meterRegistry != null) {
            for (var r : Result.values()) {
                requestCounters.put(r, Counter.builder("easyorange.cache.requests")
                        .tag("result", r.tag()).register(meterRegistry));
            }
        }
        this.loadTimer = meterRegistry == null ? null
                : Timer.builder("easyorange.cache.load")
                        .description("Source load duration on cache miss")
                        .publishPercentiles(0.5, 0.95, 0.99)
                        .register(meterRegistry);
        if (invalidationListener != null) {
            invalidationListener.register(config.keyPrefix(), l1Cache);
        }
    }

    /**
     * 读取：L1 → L2 → 回源。
     * <p>
     * L1 未命中时通过 {@link Cache#get(Object, java.util.function.Function)} 原子单飞回源；
     * 回源为 null 时以哨兵负缓存（见 {@link NullValue}）。
     */
    public <T> T get(String key, Class<T> type, Supplier<T> loader) {
        if (key == null) {
            return null;
        }
        Object l1Value = l1Cache.getIfPresent(key);
        if (l1Value != null) {
            count(Result.L1_HIT);
            return unwrap(l1Value, type);
        }
        Object value = l1Cache.get(key, _ -> load(key, type, loader));
        return unwrap(value, type);
    }

    /**
     * 显式写入 L1 + L2，并广播到其他节点失效它们的 L1（避免其他节点继续返回陈旧值）。
     */
    public <T> void put(String key, T value) {
        if (key == null || value == null) {
            return;
        }
        l1Cache.put(key, value);
        redisTemplate.opsForValue().set(buildL2Key(key), value, config.l2Ttl().toMillis(), TimeUnit.MILLISECONDS);
        publishInvalidation(key);
    }

    /**
     * 失效 L1 + L2，并广播到其他节点失效它们的 L1。
     */
    public void evict(String key) {
        if (key == null) {
            return;
        }
        l1Cache.invalidate(key);
        redisTemplate.delete(buildL2Key(key));
        publishInvalidation(key);
    }

    /**
     * L1 未命中（Caffeine 单飞内）后的回源填充逻辑。
     *
     * @return 缓存值或 {@link NullValue} 哨兵（Caffeine 不缓存 null，哨兵即负缓存载体）
     */
    private <T> Object load(String key, Class<T> type, Supplier<T> loader) {
        Object raw = redisGet(key);
        if (raw instanceof NullValue) {
            count(Result.L2_NEGATIVE);
            return raw;
        }
        if (raw != null) {
            count(Result.L2_HIT);
            return CacheUtils.cast(raw, type);
        }
        T source = loadWithSingleFlight(key, type, loader);
        return source != null ? source : new NullValue();
    }

    private Object redisGet(String key) {
        return redisTemplate.opsForValue().get(buildL2Key(key));
    }

    private <T> T loadWithSingleFlight(String key, Class<T> type, Supplier<T> loader) {
        if (redissonClient == null) {
            return loadAndWrite(key, loader);
        }
        RLock lock = redissonClient.getLock(LOCK_PREFIX + buildL2Key(key));
        boolean acquired;
        try {
            acquired = lock.tryLock(config.lockWait().toMillis(), config.lockLease().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            acquired = false;
        }
        if (!acquired) {
            // 锁超时 fail-open：允许并发回源，保证可用性优先于严格单飞
            return loadAndWrite(key, loader);
        }
        try {
            // 双重检查：持锁期间其他节点可能已回填 L2
            Object raw = redisGet(key);
            if (raw instanceof NullValue) {
                return null;
            }
            if (raw != null) {
                return CacheUtils.cast(raw, type);
            }
            return loadAndWrite(key, loader);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private <T> T loadAndWrite(String key, Supplier<T> loader) {
        T source = loadAndRecord(loader);
        writeBack(key, source);
        return source;
    }

    private <T> T loadAndRecord(Supplier<T> loader) {
        count(Result.LOAD);
        return loadTimer == null ? loader.get() : loadTimer.record(loader::get);
    }

    private <T> void writeBack(String key, T source) {
        if (source == null) {
            redisTemplate.opsForValue().set(buildL2Key(key), new NullValue(),
                    config.negativeTtl().toMillis(), TimeUnit.MILLISECONDS);
        } else {
            redisTemplate.opsForValue().set(buildL2Key(key), source,
                    config.l2Ttl().toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    private <T> T unwrap(Object value, Class<T> type) {
        if (value instanceof NullValue) {
            return null;
        }
        return CacheUtils.cast(value, type);
    }

    private void publishInvalidation(String key) {
        if (invalidationListener != null) {
            invalidationListener.publishInvalidation(config.keyPrefix(), key);
        }
    }

    private void count(Result result) {
        var counter = requestCounters.get(result);
        if (counter != null) {
            counter.increment();
        }
    }

    private String buildL2Key(String key) {
        return config.keyPrefix() + key;
    }
}
