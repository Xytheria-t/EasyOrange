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
 *
 * @param enabled 是否启用慢 SQL 检测
 * @param thresholdMs 慢 SQL 阈值（毫秒），超过此值的 SQL 会被记录
 * @param logLevel 记录慢 SQL 用的日志级别
 * @param logParameters 是否把参数内联进日志
 * @param metricsEnabled 是否收集 Micrometer 指标
 */
@Validated
@ConfigurationProperties(prefix = "slow-sql")
public record SlowSqlProperties(
        @DefaultValue("true") boolean enabled,
        @Min(1) @DefaultValue("500") long thresholdMs,
        @DefaultValue("warn") LogLevel logLevel,
        @DefaultValue("true") boolean logParameters,
        @DefaultValue("true") boolean metricsEnabled) {

    /**
     * 慢 SQL 日志级别 — 非法取值在绑定期即失败，不再静默退化成 warn。
     */
    public enum LogLevel {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }
}
