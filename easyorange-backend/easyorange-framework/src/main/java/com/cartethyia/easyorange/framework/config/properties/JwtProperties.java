package com.cartethyia.easyorange.framework.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * JWT 配置属性
 *
 * @author cartethyia
 */
@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        /**
         * RSA 私钥 PEM 文件路径（开发环境不配置则自动生成）
         */
        @DefaultValue("") String privateKeyLocation,

        /**
         * RSA 公钥 PEM 文件路径（开发环境不配置则自动生成）
         */
        @DefaultValue("") String publicKeyLocation,

        /**
         * Access Token 过期时间（分钟）
         */
        @Min(value = 1, message = "Access Token 过期时间必须为正值") @DefaultValue("30")
        long accessTokenExpiration,

        /**
         * Refresh Token 过期时间（天）
         */
        @Min(value = 1, message = "Refresh Token 过期时间必须为正值") @DefaultValue("7")
        long refreshTokenExpiration,

        /**
         * JWT 发行者
         */
        @NotBlank(message = "JWT 发行者 (issuer) 不能为空") @DefaultValue("easyorange")
        String issuer,

        /**
         * Refresh Token HttpOnly Cookie 名称
         */
        @NotBlank(message = "refresh cookie 名称不能为空") @DefaultValue("eo_refresh_token")
        String refreshCookieName,

        /**
         * Refresh Token Cookie 生效路径（建议收窄到 auth 端点）
         */
        @DefaultValue("/api/auth") String refreshCookiePath,

        /**
         * Refresh Token Cookie 是否带 Secure 标志（生产必须 true；本地 http 开发设 false）
         */
        @DefaultValue("true") boolean refreshCookieSecure,

        /**
         * Refresh Token Cookie SameSite 策略
         */
        @DefaultValue("Lax") String refreshCookieSameSite) {}
