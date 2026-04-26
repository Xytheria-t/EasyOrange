package com.cartethyia.easyorange.framework.config.security;

import com.cartethyia.easyorange.framework.config.properties.SecurityProperties;
import com.cartethyia.easyorange.framework.filter.JwtAuthenticationFilter;
import com.cartethyia.easyorange.framework.filter.XssFilter;
import com.cartethyia.easyorange.framework.handler.JsonAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final long CORS_MAX_AGE_SECONDS = 3600L;
    private static final long HSTS_MAX_AGE_SECONDS = 31536000L;
    private static final String[] CORS_ALLOWED_METHODS = {"GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"};
    private static final String[] CORS_EXPOSED_HEADERS = {"Authorization", "Authorization-New", "Content-Disposition"};

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final XssFilter xssFilter;
    private final JsonAuthenticationEntryPoint authenticationEntryPoint;
    private final JsonLogoutSuccessHandler logoutSuccessHandler;
    private final SecurityProperties securityProperties;

    @Bean
    @Order(1)
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPoint))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(securityProperties.getIgnorePaths().toArray(String[]::new)).permitAll()
                .requestMatchers(securityProperties.getProductPaths().toArray(String[]::new)).permitAll()
                .requestMatchers(securityProperties.getStaticPaths().toArray(String[]::new)).permitAll()
                .anyRequest().authenticated()
            )
            .logout(logout -> logout
                .logoutUrl(securityProperties.getLogoutUrl())
                .logoutSuccessHandler(logoutSuccessHandler)
            )
            .addFilterBefore(xssFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, XssFilter.class)
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                .contentTypeOptions(Customizer.withDefaults())
                .xssProtection(HeadersConfigurer.XXssConfig::disable)
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(HSTS_MAX_AGE_SECONDS)
                )
            )
            .build();
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
}
