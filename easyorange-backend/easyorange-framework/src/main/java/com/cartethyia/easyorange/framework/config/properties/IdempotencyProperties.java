package com.cartethyia.easyorange.framework.config.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Idempotency-Key 幂等配置。
 * <p>
 * 控制 {@code @Idempotent} 注解的默认行为。
 * </p>
 * <pre>{@code
 * idempotency:
 *   enabled: true
 *   key-prefix: "eo:idempotency"
 *   default-ttl-seconds: 86400
 * }</pre>
 */
@Getter
@Setter
@ToString
@ConfigurationProperties(prefix = "idempotency")
public class IdempotencyProperties {

    /**
     * 是否启用 Idempotency-Key 幂等保护。
     */
    private boolean enabled = true;

    /**
     * Redis key 前缀。
     */
    private String keyPrefix = "eo:idempotency";

    /**
     * 默认缓存 TTL（秒），24 小时。
     */
    private long defaultTtlSeconds = 86400;
}
