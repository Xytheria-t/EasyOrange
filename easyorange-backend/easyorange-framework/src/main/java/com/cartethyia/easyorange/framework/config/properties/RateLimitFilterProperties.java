package com.cartethyia.easyorange.framework.config.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

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
@Setter
@Getter
@ToString
@ConfigurationProperties(prefix = "rate-limit-filter")
public class RateLimitFilterProperties {

    private boolean enabled = true;

    private List<Rule> rules = new ArrayList<>();

    private RepeatSubmitConfig repeatSubmit = new RepeatSubmitConfig();

    @Setter
    @Getter
    @ToString
    public static class Rule {

        /**
         * Ant 风格路径模式，如 /api/products、/api/**
         */
        private String pathPattern;

        /**
         * HTTP 方法列表（不区分大小写），如 GET、POST。
         * 为空表示匹配所有方法。
         */
        private List<String> methods = new ArrayList<>();

        /**
         * local（本地内存）或 redis（分布式）
         */
        private String strategy = "redis";

        /**
         * 窗口内最大请求数
         */
        private int maxRequests = 100;

        /**
         * 时间窗口（秒）
         */
        private int windowSeconds = 60;

        /**
         * 限流触发时的提示信息
         */
        private String message = "请求过于频繁，请稍后重试";
    }

    @Setter
    @Getter
    @ToString
    public static class RepeatSubmitConfig {

        private boolean enabled = true;

        /**
         * 防重间隔（毫秒）
         */
        private long intervalMs = 3000;

        private String message = "不允许重复提交";

        /**
         * 需要防重的 HTTP 方法（不区分大小写）。
         * 为空表示所有写操作方法（POST/PUT/DELETE/PATCH）。
         */
        private List<String> methods = new ArrayList<>();
    }
}
