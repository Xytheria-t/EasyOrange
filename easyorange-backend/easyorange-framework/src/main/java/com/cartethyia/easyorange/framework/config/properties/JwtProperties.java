package com.cartethyia.easyorange.framework.config.properties;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * JWT 配置属性
 *
 * @author cartethyia
 */
@Data
@Slf4j
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private static final Set<String> WEAK_KEY_PATTERNS = Set.of("dev-secret", "test-secret", "example", "default");

    /**
     * JWT 密钥（至少 32 字符，256 位），生产环境必须通过环境变量 JWT_SECRET_KEY 设置
     */
    private String secretKey = "";

    /**
     * Access Token 过期时间（分钟）
     */
    private long accessTokenExpiration = 30;

    /**
     * Refresh Token 过期时间（天）
     */
    private long refreshTokenExpiration = 7;

    /**
     * Token 前缀
     */
    private String tokenPrefix = "Bearer ";

    /**
     * JWT 发行者
     */
    private String issuer = "easyorange";

    /**
     * Access Token 自动续期阈值（分钟）
     */
    private long autoRenewThresholdMinutes = 5;

    /**
     * 应用启动时验证 JWT 配置
     */
    @PostConstruct
    public void validate() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("JWT 密钥不能为空，请检查配置项 jwt.secret-key 或环境变量 JWT_SECRET_KEY");
        }
        if (secretKey.length() < 32) {
            throw new IllegalStateException("JWT 密钥长度必须至少 32 字符，请检查配置项 jwt.secret-key 或环境变量 JWT_SECRET_KEY");
        }
        if (WEAK_KEY_PATTERNS.stream().anyMatch(secretKey::contains)) {
            log.warn("警告：检测到弱 JWT 密钥，请确保仅在开发环境使用，生产环境必须使用强密钥");
        }
        log.info("JWT 配置加载完成 - 发行者：{}, Access Token 过期时间：{}分钟", issuer, accessTokenExpiration);
    }
}
