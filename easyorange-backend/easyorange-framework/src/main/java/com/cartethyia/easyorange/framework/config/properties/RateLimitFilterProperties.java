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
            repeatSubmit = new RepeatSubmitConfig(true, 3000L, "不允许重复提交", List.of());
        }
    }

    public record Rule(
            /**
             * Ant 风格路径模式，如 /api/products、/api/**
             */
            @NotBlank String pathPattern,

            /**
             * HTTP 方法列表（不区分大小写），如 GET、POST。
             * 为空表示匹配所有方法。
             */
            List<String> methods,

            /**
             * local（本地内存）或 redis（分布式）
             */
            @DefaultValue("redis") String strategy,

            /**
             * 窗口内最大请求数
             */
            @Min(1) @DefaultValue("100") int maxRequests,

            /**
             * 时间窗口（秒）
             */
            @Min(1) @DefaultValue("60") int windowSeconds,

            /**
             * 限流触发时的提示信息
             */
            @DefaultValue("请求过于频繁，请稍后重试") String message) {

        public Rule {
            methods = methods == null ? List.of() : List.copyOf(methods);
        }
    }

    public record RepeatSubmitConfig(
            @DefaultValue("true") boolean enabled,

            /**
             * 防重间隔（毫秒）
             */
            @Min(1) @DefaultValue("3000") long intervalMs,

            @DefaultValue("不允许重复提交") String message,

            /**
             * 需要防重的 HTTP 方法（不区分大小写）。
             * 为空表示所有写操作方法（POST/PUT/DELETE/PATCH）。
             */
            List<String> methods) {

        public RepeatSubmitConfig {
            methods = methods == null ? List.of() : List.copyOf(methods);
        }
    }
}
