package com.cartethyia.easyorange.framework.config.properties;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Validated
@ConfigurationProperties(prefix = "security")
public record SecurityProperties(
        @NotNull List<String> ignorePaths,

        @NotNull List<String> productPaths,

        @NotNull List<String> staticPaths,

        @NotNull List<String> allowedOrigins,

        /**
         * 需自定义头部（X-Client-Type）防护的端点（CSRF 纵深防御）。
         * 默认覆盖 refresh / logout——跨站请求无法伪造自定义头。
         */
        @NotNull List<String> csrfProtectedPaths,

        @DefaultValue("/api/auth/logout") String logoutUrl,

        /** 范围由 {@link #validate()} 运行时校验（启动即失败，含中文错误提示）。 */
        @DefaultValue("10") int passwordEncoderStrength) {

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

    @PostConstruct
    public void validate() {
        if (allowedOrigins.contains("*")) {
            log.warn("⚠️ 警告：CORS 允许所有源 (*) - 此配置仅限开发环境！");
        }
        if (passwordEncoderStrength < 4 || passwordEncoderStrength > 31) {
            throw new IllegalStateException("密码加密强度必须在 4-31 之间，当前值：" + passwordEncoderStrength);
        }
        if (passwordEncoderStrength < 10) {
            log.warn("⚠️ 警告：密码加密强度 {} 低于推荐值 10", passwordEncoderStrength);
        } else if (passwordEncoderStrength > 14) {
            log.warn("⚠️ 警告：密码加密强度 {} 较高，可能登录性能受影响", passwordEncoderStrength);
        }
    }
}
