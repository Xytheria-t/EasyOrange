package com.cartethyia.easyorange.framework.config.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 速率限制器配置属性
 * <p>
 * 用于控制速率限制器的行为，包括是否启用等。
 * </p>
 * 配置示例：
 * <pre>{@code
 * rate-limiter:
 *   enabled: true
 * }</pre>
 */
@Setter
@Getter
@Component
@ToString
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {

    /**
     * 是否启用速率限制
     * <p>
     * 默认为 true，设置为 false 时完全禁用速率限制功能
     * 开发环境可以设置为 false 以便测试
     * </p>
     */
    private boolean enabled = true;
}
