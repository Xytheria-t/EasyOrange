package com.cartethyia.easyorange.framework.config.properties;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 慢 SQL 检测配置。
 * <p>
 * 配置示例：
 * <pre>{@code
 * slow-sql:
 *   enabled: true
 *   threshold-ms: 500
 *   log-level: warn
 * }</pre>
 */
@Data
@Validated
@ConfigurationProperties(prefix = "slow-sql")
public class SlowSqlProperties {

    /** 是否启用慢 SQL 检测 */
    private boolean enabled = true;

    /** 慢 SQL 阈值（毫秒），超过此值的 SQL 会被记录 */
    @Min(1)
    private long thresholdMs = 500;

    /** 日志级别：trace / debug / info / warn / error */
    private String logLevel = "warn";

    /** 是否记录参数到日志 */
    private boolean logParameters = true;

    /** 是否收集 Micrometer 指标 */
    private boolean metricsEnabled = true;
}
