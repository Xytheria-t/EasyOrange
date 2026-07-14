package com.cartethyia.easyorange.framework.config.properties;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Data
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    private List<String> ignorePaths = new ArrayList<>();
    private List<String> productPaths = new ArrayList<>();
    private List<String> staticPaths = new ArrayList<>();
    private List<String> allowedOrigins = new ArrayList<>();
    private String logoutUrl = "/api/auth/logout";
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
            log.warn("⚠️ 警告：密码加密强度 {} 较高，可能影响登录性能", passwordEncoderStrength);
        }
        log.info("安全配置加载完成 - 登出 URL: {}, 密码加密强度：{}",
                logoutUrl, passwordEncoderStrength);
    }

    private void validatePaths(String name, List<String> paths) {
        if (paths == null) {
            throw new IllegalStateException("安全配置项 '" + name + "' 不能为 null");
        }
    }

    public List<String> getIgnorePaths() { return List.copyOf(ignorePaths); }
    public List<String> getProductPaths() { return List.copyOf(productPaths); }
    public List<String> getStaticPaths() { return List.copyOf(staticPaths); }
    public List<String> getAllowedOrigins() { return List.copyOf(allowedOrigins); }
    
}
