package com.cartethyia.easyorange.framework.config.properties;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * 安全与 CORS 配置。
 *
 * @param ignorePaths 免认证路径
 * @param productPaths 商品相关公开路径
 * @param staticPaths 静态资源路径
 * @param allowedOrigins CORS 允许的来源
 * @param csrfProtectedPaths 需自定义头部（X-Client-Type）防护的端点（CSRF 纵深防御）；
 *     默认覆盖 refresh / logout——跨站请求无法伪造自定义头
 * @param logoutUrl 登出端点
 * @param passwordEncoderStrength BCrypt 强度，取值 4~31，低于 10 或高于 14 仅告警不拦启动
 */
@Slf4j
@Validated
@ConfigurationProperties(prefix = "security")
public record SecurityProperties(
        List<String> ignorePaths,
        List<String> productPaths,
        List<String> staticPaths,
        List<String> allowedOrigins,
        List<String> csrfProtectedPaths,
        @DefaultValue("/api/auth/logout") String logoutUrl,

        @Min(value = 4, message = "密码加密强度必须在 4-31 之间")
        @Max(value = 31, message = "密码加密强度必须在 4-31 之间")
        @DefaultValue("10")
        int passwordEncoderStrength) {

    public SecurityProperties {
        ignorePaths = copyOrEmpty(ignorePaths);
        productPaths = copyOrEmpty(productPaths);
        staticPaths = copyOrEmpty(staticPaths);
        allowedOrigins = copyOrEmpty(allowedOrigins);
        csrfProtectedPaths = csrfProtectedPaths == null
                ? List.of("/api/auth/refresh", "/api/auth/logout")
                : List.copyOf(csrfProtectedPaths);
    }

    private static List<String> copyOrEmpty(List<String> paths) {
        return paths == null ? List.of() : List.copyOf(paths);
    }

    /**
     * 不推荐但不致命的取值只告警：越界值由 {@code @Min}/{@code @Max} 在绑定期拦下，这里只负责提示风险。
     */
    @PostConstruct
    private void warnAboutRiskyConfig() {
        if (allowedOrigins.contains("*")) {
            log.warn("⚠️ 警告：CORS 允许所有源 (*) - 此配置仅限开发环境！");
        }
        if (passwordEncoderStrength < 10) {
            log.warn("⚠️ 警告：密码加密强度 {} 低于推荐值 10", passwordEncoderStrength);
        } else if (passwordEncoderStrength > 14) {
            log.warn("⚠️ 警告：密码加密强度 {} 较高，可能登录性能受影响", passwordEncoderStrength);
        }
    }
}
