package com.cartethyia.easyorange.framework.config.properties;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置属性
 *
 * @author cartethyia
 */
@Data
@Slf4j
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * RSA 私钥 PEM 文件路径（开发环境不配置则自动生成）
     */
    private String privateKeyLocation = "";

    /**
     * RSA 公钥 PEM 文件路径（开发环境不配置则自动生成）
     */
    private String publicKeyLocation = "";

    /**
     * Access Token 过期时间（分钟）
     */
    private long accessTokenExpiration = 30;

    /**
     * Refresh Token 过期时间（天）
     */
    private long refreshTokenExpiration = 7;

    /**
     * JWT 发行者
     */
    private String issuer = "easyorange";

    /**
     * 应用启动时验证 JWT 配置
     */
    @PostConstruct
    public void validate() {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException("JWT 发行者 (issuer) 不能为空");
        }
        if (accessTokenExpiration <= 0) {
            throw new IllegalStateException("JWT accessTokenExpiration 必须为正值");
        }
        if (refreshTokenExpiration <= 0) {
            throw new IllegalStateException("JWT refreshTokenExpiration 必须为正值");
        }
        log.info("JWT 配置加载完成 - 发行者：{}, Access Token 过期时间：{}分钟, RSA 密钥：{}",
                issuer, accessTokenExpiration,
                !privateKeyLocation.isBlank() ? "已配置" : "自动生成（仅限开发环境）");
    }
}
