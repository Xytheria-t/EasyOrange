package com.cartethyia.easyorange.framework.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性
 * <p>
 * 配置项说明：
 * <ul>
 *   <li>secret-key: JWT 密钥，生产环境必须通过环境变量 JWT_SECRET_KEY 设置</li>
 *   <li>access-token-expiration: Access Token 过期时间（分钟）</li>
 *   <li>refresh-token-expiration: Refresh Token 过期时间（天）</li>
 *   <li>token-prefix: Token 前缀，默认 "Bearer "</li>
 *   <li>issuer: Token 发行者标识</li>
 * </ul>
 * </p>
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@Data
@Slf4j
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * JWT 密钥（至少32字符，256位）
     * <p>
     * 生产环境必须通过环境变量 JWT_SECRET_KEY 设置，禁止使用默认值
     * </p>
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
     * <p>
     * 当 token 剩余有效期小于此值时，自动生成新 token
     * </p>
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
            throw new IllegalStateException("JWT 密钥长度必须至少32字符，请检查配置项 jwt.secret-key 或环境变量 JWT_SECRET_KEY");
        }
        log.info("JWT 配置加载完成 - 发行者: {}, Access Token 过期时间: {}分钟", issuer, accessTokenExpiration);
    }
}
