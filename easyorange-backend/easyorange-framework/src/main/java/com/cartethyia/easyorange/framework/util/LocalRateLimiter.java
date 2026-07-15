package com.cartethyia.easyorange.framework.util;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongArray;

@Component
public class LocalRateLimiter {

    private static final long CLEAN_INTERVAL_SECONDS = 120;

    private final ConcurrentHashMap<String, AtomicLongArray> windows = new ConcurrentHashMap<>();

    @PostConstruct
    void startCleaner() {
        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "local-ratelimit-cleaner");
            t.setDaemon(true);
            return t;
        });
        cleaner.scheduleAtFixedRate(this::evictExpired, CLEAN_INTERVAL_SECONDS, CLEAN_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 尝试获取许可
     *
     * @param key         限流 key
     * @param limit       窗口内最大请求数
     * @param windowSizeMs 窗口大小（毫秒）
     * @return true 未超过限制，false 已触发限流
     */
    public boolean tryAcquire(String key, int limit, long windowSizeMs) {
        long now = System.currentTimeMillis();
        AtomicLongArray window = windows.compute(key, (k, v) -> {
            if (v == null || isExpired(v, windowSizeMs, now)) {
                return new AtomicLongArray(new long[]{now, 1});
            }
            v.getAndIncrement(1);
            return v;
        });
        return window.get(1) <= limit;
    }

    private boolean isExpired(AtomicLongArray window, long windowSizeMs, long now) {
        return now - window.get(0) >= windowSizeMs;
    }

    private void evictExpired() {
        var threshold = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(CLEAN_INTERVAL_SECONDS);
        windows.entrySet().removeIf(e -> e.getValue().get(0) < threshold);
    }

    public void clear() {
        windows.clear();
    }
}