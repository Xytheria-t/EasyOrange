package com.cartethyia.easyorange.framework.config.properties;

import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Set;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Idempotency-Key 幂等配置（Filter 驱动，替代 {@code @Idempotent} 注解）。
 * <p>
 * 由 {@link com.cartethyia.easyorange.framework.web.filter.IdempotencyKeyFilter} 消费，
 * 通过路径模式 + 写方法约定式启用，零注解覆盖。
 * </p>
 * <pre>{@code
 * idempotency:
 *   enabled: true
 *   header-name: "Idempotency-Key"
 *   path-patterns:
 *     - /api/orders
 *     - /api/products
 *   methods: [POST, PUT, PATCH]
 *   key-prefix: "eo:idempotency"
 *   default-ttl-seconds: 86400
 *   lock-ttl-seconds: 30
 *   lock-poll-interval-ms: 100
 * }</pre>
 */
@Data
@Validated
@ConfigurationProperties(prefix = "idempotency")
public class IdempotencyProperties {

    /** 是否启用 Idempotency-Key 幂等保护。 */
    private boolean enabled = true;

    /** 幂等 key 所在请求头名称。 */
    private String headerName = "Idempotency-Key";

    /** 启用幂等保护的路径模式（Ant 风格，如 {@code /api/orders}）。空列表视为不启用。 */
    private List<String> pathPatterns = List.of();

    /** 启用幂等保护的 HTTP 方法。 */
    private Set<String> methods = Set.of("POST", "PUT", "PATCH");

    /** Redis key 前缀。 */
    private String keyPrefix = "eo:idempotency";

    /** 默认缓存 TTL（秒），24 小时。 */
    @Min(1)
    private long defaultTtlSeconds = 86400;

    /** 处理锁 TTL（秒）。超过该时长仍未完成视为持有者崩溃，允许其它请求重新执行。 */
    @Min(1)
    private long lockTtlSeconds = 30;

    /** 输家轮询等待赢家结果的时间间隔（毫秒）。 */
    @Min(1)
    private long lockPollIntervalMs = 100;
}
