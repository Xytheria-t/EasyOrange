package com.cartethyia.easyorange.framework.config.security;

import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.security.AuthUser;
import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import com.cartethyia.easyorange.framework.config.properties.SecurityProperties;
import com.cartethyia.easyorange.framework.web.filter.RateLimitFilter;
import com.cartethyia.easyorange.framework.web.filter.TokenRevocationFilter;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@AutoConfiguration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final long CORS_MAX_AGE_SECONDS = 3600L;
    private static final long HSTS_MAX_AGE_SECONDS = 31536000L;
    private static final String[] CORS_ALLOWED_METHODS = {"GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"};
    private static final String[] CORS_EXPOSED_HEADERS = {"Authorization", "Content-Disposition"};

    private final RateLimitFilter rateLimitFilter;
    private final TokenRevocationFilter tokenRevocationFilter;
    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;

    @Bean
    @Order(1)
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((_, response, _) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    objectMapper.writeValue(response.getOutputStream(),
                            Result.error(ResultCode.UNAUTHORIZED, "认证失败，请重新登录"));
                })
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(securityProperties.getIgnorePaths().toArray(String[]::new)).permitAll()
                .requestMatchers(HttpMethod.GET, securityProperties.getProductPaths().toArray(String[]::new)).permitAll()
                .requestMatchers(securityProperties.getStaticPaths().toArray(String[]::new)).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
            .addFilterBefore(rateLimitFilter, AnonymousAuthenticationFilter.class)
            .addFilterBefore(tokenRevocationFilter, AnonymousAuthenticationFilter.class)
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                .contentTypeOptions(Customizer.withDefaults())
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'none'; base-uri 'none'; form-action 'none'")
                )
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(HSTS_MAX_AGE_SECONDS)
                )
            )
            .build();
    }

    @Bean
    public KeyPair rsaKeyPair(JwtProperties properties) {
        String privateKeyLocation = properties.getPrivateKeyLocation();
        String publicKeyLocation = properties.getPublicKeyLocation();

        if (!privateKeyLocation.isBlank() && !publicKeyLocation.isBlank()) {
            try {
                return readKeyPair(privateKeyLocation, publicKeyLocation);
            } catch (Exception e) {
                throw new RuntimeException("无法加载 RSA 密钥对，请检查 jwt.private-key-location 和 jwt.public-key-location", e);
            }
        }

        // 开发环境自动生成 2048 位 RSA 密钥对
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            log.warn("RSA 密钥对已自动生成（仅限开发环境），重启后历史 Token 将失效");
            return keyPair;
        } catch (Exception e) {
            throw new RuntimeException("RSA 密钥对自动生成失败", e);
        }
    }

    @Bean
    public JwtDecoder jwtDecoder(KeyPair keyPair, JwtProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.getIssuer()));
        return decoder;
    }

    @Bean
    public JwtEncoder jwtEncoder(KeyPair keyPair) {
        JWK jwk = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .build();
        JWKSource jwkSource = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = securityProperties.getAllowedOrigins();
        boolean allowAllOrigins = origins.contains("*");

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(allowAllOrigins ? List.of("*") : null);
        config.setAllowedOrigins(allowAllOrigins ? null : origins);
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of(CORS_ALLOWED_METHODS));
        config.setAllowCredentials(true);
        config.setMaxAge(CORS_MAX_AGE_SECONDS);
        config.setExposedHeaders(List.of(CORS_EXPOSED_HEADERS));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(securityProperties.getPasswordEncoderStrength());
    }

    private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return jwt -> {
            // Reject refresh tokens for API access
            if ("refresh".equals(jwt.getClaimAsString("type"))) {
                throw new BadJwtException("Refresh token not allowed for API access");
            }

            List<String> authorityStrings = jwt.getClaimAsStringList("authorities");
            if (authorityStrings == null) {
                authorityStrings = List.of();
            }
            var authorities = authorityStrings.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();
            var roles = authorityStrings.stream()
                    .filter(a -> a.startsWith("ROLE_"))
                    .map(a -> a.substring(5))
                    .collect(Collectors.toSet());

            var user = AuthUser.builder()
                    .userId(jwt.getSubject())
                    .username(jwt.getClaimAsString("username"))
                    .roles(roles)
                    .build();

            var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
            authentication.setDetails(jwt);
            return authentication;
        };
    }

    /**
     * 从 PEM 文件加载 RSA 密钥对。
     * <p>
     * 支持 file: 和 classpath: 前缀，由 Spring ResourceLoader 解析。
     */
    private static KeyPair readKeyPair(String privateKeyLocation, String publicKeyLocation) throws Exception {
        var factory = KeyFactory.getInstance("RSA");

        byte[] privateKeyBytes;
        byte[] publicKeyBytes;

        try (var input = new java.io.FileInputStream(privateKeyLocation)) {
            privateKeyBytes = readAllBytes(input);
        }
        try (var input = new java.io.FileInputStream(publicKeyLocation)) {
            publicKeyBytes = readAllBytes(input);
        }

        var privateKey = factory.generatePrivate(
                new PKCS8EncodedKeySpec(decodePem(privateKeyBytes)));
        var publicKey = factory.generatePublic(
                new X509EncodedKeySpec(decodePem(publicKeyBytes)));

        return new KeyPair(publicKey, privateKey);
    }

    private static byte[] decodePem(byte[] pemBytes) {
        var pem = new String(pemBytes, java.nio.charset.StandardCharsets.UTF_8);
        var base64 = pem.replaceAll("-----BEGIN [A-Z ]+-----", "")
                         .replaceAll("-----END [A-Z ]+-----", "")
                         .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }

    private static byte[] readAllBytes(InputStream input) throws Exception {
        var buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int bytesRead;
        while ((bytesRead = input.read(chunk)) != -1) {
            buffer.write(chunk, 0, bytesRead);
        }
        return buffer.toByteArray();
    }
}
