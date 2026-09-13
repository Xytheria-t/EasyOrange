package com.cartethyia.easyorange.framework.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.web.server.Cookie;
import org.springframework.validation.annotation.Validated;

/**
 * JWT 配置属性。
 *
 * @param privateKeyLocation RSA 私钥 PEM 文件路径（开发环境不配置则自动生成）
 * @param publicKeyLocation RSA 公钥 PEM 文件路径（开发环境不配置则自动生成）
 * @param accessTokenExpiration Access Token 过期时间（分钟）
 * @param refreshTokenExpiration Refresh Token 过期时间（天）
 * @param issuer JWT 发行者
 * @param refreshCookieName Refresh Token HttpOnly Cookie 名称
 * @param refreshCookiePath Refresh Token Cookie 生效路径（建议收窄到 auth 端点）
 * @param refreshCookieSecure Refresh Token Cookie 是否带 Secure 标志（生产必须 true；本地 http 开发设 false）
 * @param refreshCookieSameSite Refresh Token Cookie SameSite 策略，{@code OMITTED} 表示不输出该属性
 * @author cartethyia
 */
@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        @DefaultValue("") String privateKeyLocation,
        @DefaultValue("") String publicKeyLocation,

        @Min(value = 1, message = "Access Token 过期时间必须为正值") @DefaultValue("30")
        long accessTokenExpiration,

        @Min(value = 1, message = "Refresh Token 过期时间必须为正值") @DefaultValue("7")
        long refreshTokenExpiration,

        @NotBlank(message = "JWT 发行者 (issuer) 不能为空") @DefaultValue("easyorange")
        String issuer,

        @NotBlank(message = "refresh cookie 名称不能为空") @DefaultValue("eo_refresh_token")
        String refreshCookieName,

        @DefaultValue("/api/auth") String refreshCookiePath,
        @DefaultValue("true") boolean refreshCookieSecure,
        @DefaultValue("Lax") Cookie.SameSite refreshCookieSameSite) {}
