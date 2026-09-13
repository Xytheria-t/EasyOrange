package com.cartethyia.easyorange.framework.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Filter-based 限流配置属性
 * <p>
 * 通过 path-pattern + method 组合规则实现约定式限流，替代 AOP 切面方式。
 * 未命中任何规则的请求不限流。
 * </p>
 * 配置示例：
 * <pre>{@code
 * rate-limit-filter:
 *   enabled: true
 *   rules:
 *     - path-pattern: /api/products
 *       method: GET
 *       strategy: local
 *       max-requests: 200
 *       window-seconds: 60
 *     - path-pattern: /api/**
 *       method: [POST, PUT, DELETE, PATCH]
 *       strategy: redis
 *       max-requests: 30
 *       window-seconds: 60
 *   repeat-submit:
 *     enabled: true
 *     interval-ms: 3000
 *     message: "不允许重复提交"
 *     methods: [POST, PUT, DELETE, PATCH]
 * }</pre>
 *
 * @param enabled 是否启用限流过滤器
 * @param rules 限流规则列表
 * @param repeatSubmit 重复提交防护配置
 */
@Validated
@ConfigurationProperties(prefix = "rate-limit-filter")
public record RateLimitFilterProperties(
        @DefaultValue("true") boolean enabled,
        @Valid List<Rule> rules,
        @Valid RepeatSubmitConfig repeatSubmit) {

    public RateLimitFilterProperties {
        rules = rules == null ? List.of() : List.copyOf(rules);
        if (repeatSubmit == null) {
            repeatSubmit = new RepeatSubmitConfig(
                    RepeatSubmitConfig.DEFAULT_ENABLED,
                    RepeatSubmitConfig.DEFAULT_INTERVAL_MS,
                    RepeatSubmitConfig.DEFAULT_MESSAGE,
                    List.of());
        }
    }

    /**
     * 限流策略 — 决定计数落在本地内存还是 Redis。
     */
    public enum Strategy {

        /** 本地内存计数，单实例生效，零网络开销。 */
        LOCAL,

        /** Redis 计数，多实例共享配额。 */
        REDIS
    }

    /**
     * 单条限流规则。
     *
     * @param pathPattern Ant 风格路径模式，如 /api/products、/api/**
     * @param methods HTTP 方法列表（不区分大小写），如 GET、POST；为空表示匹配所有方法
     * @param strategy 限流策略
     * @param maxRequests 窗口内最大请求数
     * @param windowSeconds 时间窗口（秒）
     * @param message 限流触发时的提示信息
     */
    public record Rule(
            @NotBlank String pathPattern,
            List<String> methods,
            @DefaultValue("redis") Strategy strategy,
            @Min(1) @DefaultValue("100") int maxRequests,
            @Min(1) @DefaultValue("60") int windowSeconds,
            @DefaultValue("请求过于频繁，请稍后重试") String message) {

        public Rule {
            methods = methods == null ? List.of() : List.copyOf(methods);
        }
    }

    /**
     * 重复提交防护配置。
     *
     * @param enabled 是否启用
     * @param intervalMs 防重间隔（毫秒）
     * @param message 触发时的提示信息
     * @param methods 需要防重的 HTTP 方法（不区分大小写）；为空表示所有写操作方法（POST/PUT/DELETE/PATCH）
     */
    public record RepeatSubmitConfig(
            @DefaultValue(DEFAULT_ENABLED + "") boolean enabled,
            @Min(1) @DefaultValue(DEFAULT_INTERVAL_MS + "") long intervalMs,
            @DefaultValue(DEFAULT_MESSAGE) String message,
            List<String> methods) {

        private static final boolean DEFAULT_ENABLED = true;
        private static final long DEFAULT_INTERVAL_MS = 3000L;
        private static final String DEFAULT_MESSAGE = "不允许重复提交";

        public RepeatSubmitConfig {
            methods = methods == null ? List.of() : List.copyOf(methods);
        }
    }
}
