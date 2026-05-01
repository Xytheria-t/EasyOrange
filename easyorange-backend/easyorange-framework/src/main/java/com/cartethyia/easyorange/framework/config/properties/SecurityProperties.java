package com.cartethyia.easyorange.framework.config.properties;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 安全配置属性
 * <p>
 * 用于配置 Spring Security 的安全相关参数，包括：
 * <ul>
 *   <li>忽略认证的路径 - 无需登录即可访问</li>
 *   <li>产品路径 - 产品相关公开接口</li>
 *   <li>静态资源路径 - 静态资源访问路径</li>
 *   <li>CORS 允许的源 - 跨域请求允许的域名</li>
 *   <li>登出 URL - 用户登出接口路径</li>
 *   <li>密码加密强度 - BCrypt 加密轮数</li>
 * </ul>
 * </p>
 * 配置示例：
 * <pre>{@code
 * security:
 *   ignore-paths:
 *     - /api/auth/login
 *     - /api/auth/register
 *     - /api/public/**
 *   product-paths:
 *     - /api/products/**
 *     - /api/categories/**
 *   static-paths:
 *     - /static/**
 *     - /public/**
 *   allowed-origins:
 *     - https://app.example.com
 *     - https://admin.example.com
 *   logout-url: /api/auth/logout
 *   password-encoder-strength: 12
 * }</pre>
 *
 * @author cartethyia
 */
@Slf4j
@Setter
@Getter
@Component
@ToString
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    /**
     * 忽略认证的路径列表
     * <p>
     * 配置在此列表中的路径无需 authentication 即可访问
     * 通常用于：登录接口、注册接口、公开 API 等
     * </p>
     */
    private List<String> ignorePaths = new ArrayList<>();

    /**
     * 产品路径列表
     * <p>
     * 产品相关的公开访问路径，无需 authentication 即可访问
     * </p>
     */
    private List<String> productPaths = new ArrayList<>();

    /**
     * 静态资源路径列表
     * <p>
     * 静态资源访问路径，如：/static/**, /public/** 等
     * </p>
     */
    private List<String> staticPaths = new ArrayList<>();

    /**
     * CORS 允许的源列表
     * <p>
     * 配置允许跨域请求的域名，支持：
     * </p>
     * <ul>
     *   <li>具体域名：<a href="https://example.com">...</a></li>
     *   <li>通配符：* (允许所有域名，仅限开发环境)</li>
     *   <li>多个域名：["https://a.com", "https://b.com"]</li>
     * </ul>
     * <p>
     * ⚠️ 生产环境禁止使用 "*"，应明确指定允许的域名
     * </p>
     */
    private List<String> allowedOrigins = new ArrayList<>();

    /**
     * 登出 URL 路径
     * <p>
     * 用户登出接口的路径，默认：/api/auth/logout
     * </p>
     */
    private String logoutUrl = "/api/auth/logout";

    /**
     * BCrypt 密码加密强度
     * <p>
     * 取值范围：4-31，默认值：10
     * </p>
     * <p>
     * 说明：
     * </p>
     * <ul>
     *   <li>值越大，加密越安全，但计算时间越长</li>
     *   <li>10 是 Spring Security 的推荐默认值（约 100ms 计算时间）</li>
     *   <li>生产环境建议根据服务器性能调整为 12-14</li>
     * </ul>
     */
    private int passwordEncoderStrength = 10;

    /**
     * 是否启用 XSS 防护
     * <p>
     * 对于纯 JSON API，建议关闭此选项，由前端负责 XSS 防护
     * 对于传统的服务端渲染应用，建议开启此选项
     * </p>
     * <p>
     * 默认值：false（关闭）
     * </p>
     */
    private boolean xssProtectionEnabled = false;

    /**
     * 应用启动时验证安全配置
     */
    @PostConstruct
    public void validate() {
        validatePaths("ignorePaths", ignorePaths);
        validatePaths("productPaths", productPaths);
        validatePaths("staticPaths", staticPaths);
        validateAllowedOrigins();
        validatePasswordEncoderStrength();

        log.info("安全配置加载完成 - 登出 URL: {}, 密码加密强度：{}, XSS 防护：{}",
                logoutUrl, passwordEncoderStrength, xssProtectionEnabled ? "启用" : "禁用");
    }

    /**
     * 验证路径配置
     *
     * @param name 配置项名称
     * @param paths 路径列表
     */
    private void validatePaths(String name, List<String> paths) {
        if (paths == null) {
            throw new IllegalStateException("安全配置项 '" + name + "' 不能为 null");
        }
    }

    /**
     * 验证允许的源配置
     */
    private void validateAllowedOrigins() {
        if (allowedOrigins == null) {
            throw new IllegalStateException("安全配置项 'allowedOrigins' 不能为 null");
        }

        if (allowedOrigins.contains("*")) {
            log.warn("⚠️ 警告：CORS 允许所有源 (*) - 此配置仅限开发环境，生产环境必须指定具体域名！");
        }
    }

    /**
     * 验证密码加密强度
     */
    private void validatePasswordEncoderStrength() {
        if (passwordEncoderStrength < 4 || passwordEncoderStrength > 31) {
            throw new IllegalStateException(
                "密码加密强度必须在 4-31 之间，当前值：" + passwordEncoderStrength
            );
        }
        if (passwordEncoderStrength < 10) {
            log.warn("⚠️ 警告：密码加密强度 {} 低于推荐值 10，安全性可能不足！", passwordEncoderStrength);
        }
        if (passwordEncoderStrength > 14) {
            log.warn("⚠️ 警告：密码加密强度 {} 较高，可能影响登录性能", passwordEncoderStrength);
        }
    }

    /**
     * 获取忽略路径的不可变列表
     *
     * @return 不可变路径列表
     */
    public List<String> getIgnorePaths() {
        return Collections.unmodifiableList(ignorePaths);
    }

    /**
     * 获取产品路径的不可变列表
     *
     * @return 不可变路径列表
     */
    public List<String> getProductPaths() {
        return Collections.unmodifiableList(productPaths);
    }

    /**
     * 获取静态资源路径的不可变列表
     *
     * @return 不可变路径列表
     */
    public List<String> getStaticPaths() {
        return Collections.unmodifiableList(staticPaths);
    }

    /**
     * 获取允许的源的不可变列表
     *
     * @return 不可变源列表
     */
    public List<String> getAllowedOrigins() {
        return Collections.unmodifiableList(allowedOrigins);
    }
}
