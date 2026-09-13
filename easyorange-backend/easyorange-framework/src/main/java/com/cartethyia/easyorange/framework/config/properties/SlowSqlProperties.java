package com.cartethyia.easyorange.framework.config.properties;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
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
@Validated
@ConfigurationProperties(prefix = "slow-sql")
public record SlowSqlProperties(
        /** 是否启用慢 SQL 检测 */
        @DefaultValue("true") boolean enabled,

        /** 慢 SQL 阈值（毫秒），超过此值的 SQL 会被记录 */
        @Min(1) @DefaultValue("500") long thresholdMs,

        /** 日志级别：trace / debug / info / warn / error */
        @DefaultValue("warn") String logLevel,

        /** 是否记录参数到日志 */
        @DefaultValue("true") boolean logParameters,

        /** 是否收集 Micrometer 指标 */
        @DefaultValue("true") boolean metricsEnabled) {}
