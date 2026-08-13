package com.cartethyia.easyorange.framework.config.security;

import com.cartethyia.easyorange.common.enums.IResultCode;
import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.security.AuthUser;
import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import com.cartethyia.easyorange.framework.config.properties.SecurityProperties;
import com.cartethyia.easyorange.framework.web.ErrorResponseWriter;
import com.cartethyia.easyorange.framework.web.filter.IdempotencyKeyFilter;
import com.cartethyia.easyorange.framework.web.filter.RateLimitFilter;
import com.cartethyia.easyorange.framework.web.filter.RefreshCsrfFilter;
import com.cartethyia.easyorange.framework.web.filter.TokenRevocationFilter;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jwt.proc.ExpiredJWTException;
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
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
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

@Slf4j
@AutoConfiguration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // ========== Constants ==========

    private static final long CORS_MAX_AGE_SECONDS = 3600L;
    private static final long HSTS_MAX_AGE_SECONDS = 31536000L;
    private static final String[] CORS_ALLOWED_METHODS = {"GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"};
    private static final String[] CORS_EXPOSED_HEADERS = {"Authorization", "Content-Disposition"};

    // ========== Dependencies ==========

    private final IdempotencyKeyFilter idempotencyKeyFilter;
    private final RateLimitFilter rateLimitFilter;
    private final TokenRevocationFilter tokenRevocationFilter;
    private final RefreshCsrfFilter refreshCsrfFilter;
    private final SecurityProperties securityProperties;
    private final ErrorResponseWriter errorResponseWriter;

    // ========== Security Filter Chain ==========

    @Bean
    @Order(1)
    public SecurityFilterChain filterChain(HttpSecurity http) {
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
                        .requestMatchers(securityProperties.getIgnorePaths().toArray(String[]::new))
                        .permitAll()
                        // 精确匹配优先于 product-paths 前缀放行：/api/products/my 需登录（CLAUDE.md product-paths 陷阱）
                        .requestMatchers(HttpMethod.GET, "/api/products/my/**")
                        .authenticated()
                        // 管理后台仅 ADMIN/MANAGER 可访问（UserType#getDefaultRoles 均含 ROLE_ADMIN）
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.GET,
                                securityProperties.getProductPaths().toArray(String[]::new))
                        .permitAll()
                        .requestMatchers(securityProperties.getStaticPaths().toArray(String[]::new))
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

    // ========== JWT Key & Codec ==========

    @Bean
    public KeyPair rsaKeyPair(JwtProperties properties) {
        var privateKeyLocation = properties.getPrivateKeyLocation();
        var publicKeyLocation = properties.getPublicKeyLocation();

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
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.getIssuer()));
        return decoder;
    }

    @Bean
    public JwtEncoder jwtEncoder(KeyPair keyPair) {
        var jwk = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
    }

    // ========== CORS ==========

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var origins = securityProperties.getAllowedOrigins();
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

    // ========== Password Encoding ==========

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(securityProperties.getPasswordEncoderStrength());
    }

    // ========== Private Helpers ==========

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
