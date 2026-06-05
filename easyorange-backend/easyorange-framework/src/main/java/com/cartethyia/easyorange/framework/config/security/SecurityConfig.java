package com.cartethyia.easyorange.framework.config.security;

import com.cartethyia.easyorange.common.dto.AuthUser;
import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import com.cartethyia.easyorange.framework.config.properties.SecurityProperties;
import com.cartethyia.easyorange.framework.config.constant.LoginCacheConstants;
import com.cartethyia.easyorange.framework.web.filter.RateLimitFilter;
import com.cartethyia.easyorange.framework.web.filter.XssFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final long CORS_MAX_AGE_SECONDS = 3600L;
    private static final long HSTS_MAX_AGE_SECONDS = 31536000L;
    private static final String[] CORS_ALLOWED_METHODS = {"GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"};
    private static final String[] CORS_EXPOSED_HEADERS = {"Authorization", "Content-Disposition"};

    private final RateLimitFilter rateLimitFilter;
    private final XssFilter xssFilter;
    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;

    @Bean
    @Order(1)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
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
            .addFilterBefore(xssFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(rateLimitFilter, XssFilter.class)
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                .contentTypeOptions(Customizer.withDefaults())
                .xssProtection(HeadersConfigurer.XXssConfig::disable)
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(HSTS_MAX_AGE_SECONDS)
                )
            )
            .userDetailsService(userDetailsService())
            .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(JwtProperties properties,
                                 org.springframework.data.redis.core.StringRedisTemplate redis) {
        var key = new SecretKeySpec(
                properties.getSecretKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtDecoder nimbus = NimbusJwtDecoder.withSecretKey(key).build();
        nimbus.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.getIssuer()));

        return token -> {
            Jwt jwt = nimbus.decode(token);

            // Check blacklist
            String jti = jwt.getId();
            if (jti != null && Boolean.TRUE.equals(
                    redis.hasKey(LoginCacheConstants.TOKEN_BLACKLIST_KEY + jti))) {
                throw new BadJwtException("Token has been revoked");
            }

            // Check force logout
            var forceLogoutKey = LoginCacheConstants.FORCE_LOGOUT_KEY + jwt.getSubject();
            String forceLogoutTime = redis.opsForValue().get(forceLogoutKey);
            if (forceLogoutTime != null) {
                Instant iat = jwt.getIssuedAt();
                if (iat != null && iat.toEpochMilli() < Long.parseLong(forceLogoutTime)) {
                    throw new BadJwtException("Token revoked by force logout");
                }
            }

            return jwt;
        };
    }

    @Bean
    public JwtEncoder jwtEncoder(JwtProperties properties) {
        var key = new SecretKeySpec(
                properties.getSecretKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var jwk = new OctetSequenceKey.Builder(key).algorithm(JWSAlgorithm.HS256).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
    }

    private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return jwt -> {
            // Reject refresh tokens for API access
            if ("refresh".equals(jwt.getClaimAsString("type"))) {
                throw new BadJwtException("Refresh token not allowed for API access");
            }

            String userType = jwt.getClaimAsString("userType");
            List<SimpleGrantedAuthority> authorities;
            Set<String> roles;
            if ("00".equals(userType) || "02".equals(userType)) {
                authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_USER")
                );
                roles = Set.of("ADMIN", "USER");
            } else {
                authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
                roles = Set.of("USER");
            }

            var user = AuthUser.builder()
                    .userId(Long.valueOf(jwt.getSubject()))
                    .username(jwt.getClaimAsString("username"))
                    .roles(roles)
                    .build();

            var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
            authentication.setDetails(jwt);
            return authentication;
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = securityProperties.getAllowedOrigins();
        boolean allowAllOrigins = origins.contains("*");

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(allowAllOrigins ? List.of("*") : null);
        config.setAllowedOrigins(allowAllOrigins ? null : origins);
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(Stream.of(CORS_ALLOWED_METHODS).toList());
        config.setAllowCredentials(true);
        config.setMaxAge(CORS_MAX_AGE_SECONDS);
        config.setExposedHeaders(Stream.of(CORS_EXPOSED_HEADERS).toList());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(securityProperties.getPasswordEncoderStrength());
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("User details not available. Use JWT authentication instead.");
        };
    }
}
