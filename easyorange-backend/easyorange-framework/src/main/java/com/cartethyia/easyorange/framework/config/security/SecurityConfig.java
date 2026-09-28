package com.cartethyia.easyorange.framework.config.security;

import com.cartethyia.easyorange.common.enums.IResultCode;
import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.security.AuthUser;
import com.cartethyia.easyorange.framework.config.properties.IdempotencyProperties;
import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import com.cartethyia.easyorange.framework.config.properties.RateLimitFilterProperties;
import com.cartethyia.easyorange.framework.config.properties.SecurityProperties;
import com.cartethyia.easyorange.framework.util.DistributedRateLimiter;
import com.cartethyia.easyorange.framework.util.LocalRateLimiter;
import com.cartethyia.easyorange.framework.web.ErrorResponseWriter;
import com.cartethyia.easyorange.framework.web.filter.IdempotencyKeyFilter;
import com.cartethyia.easyorange.framework.web.filter.RateLimitFilter;
import com.cartethyia.easyorange.framework.web.filter.RefreshCsrfFilter;
import com.cartethyia.easyorange.framework.web.filter.TokenRevocationFilter;
import com.cartethyia.easyorange.framework.web.idempotency.IdempotencyService;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jwt.proc.ExpiredJWTException;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.header.writers.ContentSecurityPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerMapping;

@Slf4j
@AutoConfiguration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // ── 常量 ──

    private static final long CORS_MAX_AGE_SECONDS = 3600L;
    private static final long HSTS_MAX_AGE_SECONDS = 31536000L;
    private static final String[] CORS_ALLOWED_METHODS = {"GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"};
    private static final String[] CORS_EXPOSED_HEADERS = {"Authorization", "Content-Disposition"};

    // ── 依赖注入 ──

    private final SecurityProperties securityProperties;
    private final ErrorResponseWriter errorResponseWriter;

    // ── 安全过滤链 ──

    /**
     * 四个业务过滤器<b>只在 Security 链里注册一次</b>。
     * <p>
     * 两处曾经重复：{@code @Component}（Spring Boot 把容器里任何 Filter bean 自动注册进 servlet 容器链）
     * 与 {@code addFilterBefore}。{@code OncePerRequestFilter} 的标记让第二次执行被跳过，于是实际顺序由
     * Security 链决定，容器链上的 {@code @Order} 是死配置 —— 注释表达的却是反过来。
     * <p>
     * 现在：{@code @Component} 去掉，{@code addFilterBefore} 保留，并配 {@code FilterRegistrationBean(enabled=false)}
     * 把它们挡在容器链之外。执行顺序 = {@code addFilterBefore} 的声明顺序。
     * <p>
     * {@link RateLimitFilter} 必须收 {@code ObjectProvider<List<HandlerMapping>>}（延迟注入），
     * 直接注入会经 WebSocket 配置链形成循环依赖，别改回构造器直连。
     */
    @Bean
    public RateLimitFilter rateLimitFilter(
            RateLimitFilterProperties rateLimitProperties,
            RedisTemplate<Object, Object> redisTemplate,
            LocalRateLimiter localRateLimiter,
            DistributedRateLimiter distributedRateLimiter,
            ObjectProvider<List<HandlerMapping>> handlerMappingsProvider) {
        return new RateLimitFilter(
                rateLimitProperties,
                redisTemplate,
                localRateLimiter,
                distributedRateLimiter,
                errorResponseWriter,
                handlerMappingsProvider);
    }

    @Bean
    public RefreshCsrfFilter refreshCsrfFilter() {
        return new RefreshCsrfFilter(securityProperties, errorResponseWriter);
    }

    @Bean
    public IdempotencyKeyFilter idempotencyKeyFilter(
            IdempotencyService idempotencyService, IdempotencyProperties idempotencyProperties) {
        return new IdempotencyKeyFilter(idempotencyService, idempotencyProperties);
    }

    @Bean
    public TokenRevocationFilter tokenRevocationFilter(StringRedisTemplate redis, MeterRegistry meterRegistry) {
        return new TokenRevocationFilter(redis, errorResponseWriter, meterRegistry);
    }

    // 去掉 @Component 还不算完：ServletContextInitializerBeans 会把容器里<b>任何</b> Filter bean
    // （@Bean 定义的也一样）自动注册进 servlet 容器链。靠 FilterRegistrationBean(enabled=false)
    // 显式声明「不进容器链」，否则与 addFilterBefore 构成第二次注册，@Order 又变成两套事实。
    @Bean
    FilterRegistrationBean<RateLimitFilter> rateLimitFilterContainerRegistration(RateLimitFilter rateLimitFilter) {
        return notInContainerChain(rateLimitFilter);
    }

    @Bean
    FilterRegistrationBean<RefreshCsrfFilter> refreshCsrfFilterContainerRegistration(RefreshCsrfFilter filter) {
        return notInContainerChain(filter);
    }

    @Bean
    FilterRegistrationBean<IdempotencyKeyFilter> idempotencyKeyFilterContainerRegistration(
            IdempotencyKeyFilter idempotencyKeyFilter) {
        return notInContainerChain(idempotencyKeyFilter);
    }

    @Bean
    FilterRegistrationBean<TokenRevocationFilter> tokenRevocationFilterContainerRegistration(
            TokenRevocationFilter tokenRevocationFilter) {
        return notInContainerChain(tokenRevocationFilter);
    }

    private static <T extends Filter> FilterRegistrationBean<T> notInContainerChain(T filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            IdempotencyKeyFilter idempotencyKeyFilter,
            RateLimitFilter rateLimitFilter,
            RefreshCsrfFilter refreshCsrfFilter,
            TokenRevocationFilter tokenRevocationFilter) {
        return http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((_, response, ex) -> {
                            var code = resolveAuthFailureCode(ex);
                            errorResponseWriter.write(
                                    response, HttpServletResponse.SC_UNAUTHORIZED, code, code.getMessage());
                        })
                        .accessDeniedHandler((_, response, _) -> errorResponseWriter.write(
                                response, HttpServletResponse.SC_FORBIDDEN, ResultCode.FORBIDDEN, "权限不足")))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()
                        .requestMatchers(securityProperties.ignorePaths().toArray(String[]::new))
                        .permitAll()
                        // 声明顺序即优先级：本条排在 product-paths 之前，/api/products/** 万一被误加进
                        // product-paths 也不会把「我的商品」放行；且 /api/products 是精确匹配
                        // （PathPattern 未带 ** 不做前缀放行），本就覆盖不到 /api/products/my，故显式声明
                        .requestMatchers(HttpMethod.GET, "/api/products/my/**")
                        .authenticated()
                        // 管理后台仅 ADMIN/MANAGER 可访问（UserType#getDefaultRoles 均含 ROLE_ADMIN）
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.GET,
                                securityProperties.productPaths().toArray(String[]::new))
                        .permitAll()
                        .requestMatchers(securityProperties.staticPaths().toArray(String[]::new))
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(
                        oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .addFilterBefore(idempotencyKeyFilter, AnonymousAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, AnonymousAuthenticationFilter.class)
                .addFilterBefore(refreshCsrfFilter, AnonymousAuthenticationFilter.class)
                .addFilterBefore(tokenRevocationFilter, AnonymousAuthenticationFilter.class)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .addHeaderWriter(new ContentSecurityPolicyHeaderWriter(
                                "default-src 'none'; base-uri 'none'; form-action 'none'"))
                        .httpStrictTransportSecurity(
                                hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(HSTS_MAX_AGE_SECONDS)))
                .build();
    }

    // ── JWT 密钥与编解码 ──

    @Bean
    public KeyPair rsaKeyPair(JwtProperties properties) {
        var privateKeyLocation = properties.privateKeyLocation();
        var publicKeyLocation = properties.publicKeyLocation();

        if (!privateKeyLocation.isBlank() && !publicKeyLocation.isBlank()) {
            try {
                RSAPrivateKey privateKey;
                RSAPublicKey publicKey;
                try (var in = Files.newInputStream(Path.of(privateKeyLocation))) {
                    privateKey = RsaKeyConverters.pkcs8().convert(in);
                }
                try (var in = Files.newInputStream(Path.of(publicKeyLocation))) {
                    publicKey = RsaKeyConverters.x509().convert(in);
                }
                return new KeyPair(publicKey, privateKey);
            } catch (Exception e) {
                throw new RuntimeException("无法加载 RSA 密钥对，请检查 jwt.private-key-location 和 jwt.public-key-location", e);
            }
        }

        // 开发环境自动生成 2048 位 RSA 密钥对
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            var keyPair = generator.generateKeyPair();
            log.warn("RSA 密钥对已自动生成（仅限开发环境），重启后历史 Token 将失效");
            return keyPair;
        } catch (Exception e) {
            throw new RuntimeException("RSA 密钥对自动生成失败", e);
        }
    }

    @Bean
    public JwtDecoder jwtDecoder(KeyPair keyPair, JwtProperties properties) {
        var decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic())
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        return decoder;
    }

    @Bean
    public JwtEncoder jwtEncoder(KeyPair keyPair) {
        var jwk = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
    }

    // ── CORS 跨域 ──

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var origins = securityProperties.allowedOrigins();
        var config = new CorsConfiguration();
        if (origins.contains("*")) {
            config.setAllowedOriginPatterns(List.of("*"));
        } else {
            config.setAllowedOrigins(origins);
        }
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of(CORS_ALLOWED_METHODS));
        config.setAllowCredentials(true);
        config.setMaxAge(CORS_MAX_AGE_SECONDS);
        config.setExposedHeaders(List.of(CORS_EXPOSED_HEADERS));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // ── 密码编码 ──

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(securityProperties.passwordEncoderStrength());
    }

    // ── 私有辅助方法 ──

    /**
     * 认证失败 → 业务错误码：JWT 过期（Spring {@code BadJwtException("JWT expired at ...")}
     * 消息或 nimbus {@link ExpiredJWTException}，两层 cause 链均检测）→ A04011「登录已过期」，
     * 其余（无效/缺失/吊销/刷新令牌误用）→ A0401「未登录」。
     */
    static IResultCode resolveAuthFailureCode(AuthenticationException ex) {
        return isExpiredToken(ex) ? ResultCode.TOKEN_EXPIRED : ResultCode.UNAUTHORIZED;
    }

    private static boolean isExpiredToken(AuthenticationException ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof ExpiredJWTException) {
                return true;
            }
            if (t instanceof BadJwtException bad
                    && bad.getMessage() != null
                    && bad.getMessage().startsWith("JWT expired")) {
                return true;
            }
        }
        return false;
    }

    private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return jwt -> {
            // Reject refresh tokens for API access
            if ("refresh".equals(jwt.getClaimAsString("type"))) {
                throw new BadJwtException("Refresh token not allowed for API access");
            }

            var authorities = jwt.getClaimAsStringList("authorities");
            var granted = authorities != null
                    ? authorities.stream().map(SimpleGrantedAuthority::new).toList()
                    : List.<SimpleGrantedAuthority>of();
            var user = new AuthUser(jwt.getSubject(), jwt.getClaimAsString("username"));

            var authentication = new UsernamePasswordAuthenticationToken(user, null, granted);
            authentication.setDetails(jwt);
            return authentication;
        };
    }
}
