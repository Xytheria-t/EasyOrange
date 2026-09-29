package com.cartethyia.easyorange.framework.config.properties;

import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Idempotency-Key 幂等配置 — 由 {@link com.cartethyia.easyorange.framework.web.filter.IdempotencyKeyFilter} 消费，
 * 路径模式 + 写方法约定式启用，零注解覆盖。
 *
 * @param pathPatterns 启用幂等的路径模式（Ant 风格，如 {@code /api/orders}）；空列表视为不启用
 * @param methods 启用幂等的 HTTP 方法；未配置时默认 POST/PUT/PATCH
 * @param defaultTtlSeconds 缓存 TTL（秒），默认 86400 即 24 小时
 * @param lockTtlSeconds 处理锁 TTL（秒）；超时仍未完成视为持有者崩溃，允许其它请求重新执行
 * @param lockPollIntervalMs 输家轮询等待赢家结果的间隔（毫秒）
 */
@Validated
@ConfigurationProperties(prefix = "idempotency")
public record IdempotencyProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("Idempotency-Key") String headerName,
        List<String> pathPatterns,
        Set<String> methods,
        @DefaultValue("eo:idempotency") String keyPrefix,
        @Min(1) @DefaultValue("86400") long defaultTtlSeconds,
        @Min(1) @DefaultValue("30") long lockTtlSeconds,
        @Min(1) @DefaultValue("100") long lockPollIntervalMs) {

    public IdempotencyProperties {
        pathPatterns = pathPatterns == null ? List.of() : List.copyOf(pathPatterns);
        methods = methods == null ? Set.of("POST", "PUT", "PATCH") : Set.copyOf(methods);
    }
}
