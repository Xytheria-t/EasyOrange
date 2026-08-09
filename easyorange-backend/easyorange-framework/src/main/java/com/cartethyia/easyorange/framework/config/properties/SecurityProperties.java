package com.cartethyia.easyorange.framework.config.properties;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Data
@Validated
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    @NotNull
    private List<String> ignorePaths = new ArrayList<>();

    @NotNull
    private List<String> productPaths = new ArrayList<>();

    @NotNull
    private List<String> staticPaths = new ArrayList<>();

    @NotNull
    private List<String> allowedOrigins = new ArrayList<>();

    /**
     * 需自定义头部（X-Client-Type）防护的端点（CSRF 纵深防御）。
     * 默认覆盖 refresh / logout——跨站请求无法伪造自定义头。
     */
    @NotNull
    private List<String> csrfProtectedPaths = new ArrayList<>(List.of("/api/auth/refresh", "/api/auth/logout"));

    private String logoutUrl = "/api/auth/logout";

    /** 范围由 {@link #validate()} 运行时校验（启动即失败，含中文错误提示）。 */
    private int passwordEncoderStrength = 10;

    @PostConstruct
    public void validate() {
        validatePaths("ignorePaths", ignorePaths);
        validatePaths("productPaths", productPaths);
        validatePaths("staticPaths", staticPaths);
        if (allowedOrigins == null) {
            throw new IllegalStateException("安全配置项 'allowedOrigins' 不能为 null");
        }
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

    private void validatePaths(String name, List<String> paths) {
        if (paths == null) {
            throw new IllegalStateException("安全配置项 '" + name + "' 不能为 null");
        }
    }

    public List<String> getIgnorePaths() {
        return List.copyOf(ignorePaths);
    }

    public List<String> getProductPaths() {
        return List.copyOf(productPaths);
    }

    public List<String> getStaticPaths() {
        return List.copyOf(staticPaths);
    }

    public List<String> getCsrfProtectedPaths() {
        return List.copyOf(csrfProtectedPaths);
    }

    public List<String> getAllowedOrigins() {
        return List.copyOf(allowedOrigins);
    }
}
