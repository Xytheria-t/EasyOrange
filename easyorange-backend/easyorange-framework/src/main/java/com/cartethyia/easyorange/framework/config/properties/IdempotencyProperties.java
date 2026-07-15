package com.cartethyia.easyorange.framework.config.properties;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

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
@Data
@Validated
@ConfigurationProperties(prefix = "idempotency")
public class IdempotencyProperties {

    /** 是否启用 Idempotency-Key 幂等保护。 */
    private boolean enabled = true;

    /** Redis key 前缀。 */
    private String keyPrefix = "eo:idempotency";

    /** 默认缓存 TTL（秒），24 小时。 */
    @Min(1)
    private long defaultTtlSeconds = 86400;
}
