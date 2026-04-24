package com.cartethyia.easyorange.framework.filter;

import com.cartethyia.easyorange.common.constant.CacheConstants;
import com.cartethyia.easyorange.common.dto.AuthUser;
import com.cartethyia.easyorange.common.util.RequestUtil;
import com.cartethyia.easyorange.framework.config.JwtProperties;
import com.cartethyia.easyorange.framework.config.properties.SecurityProperties;
import com.cartethyia.easyorange.framework.service.TokenService;
import com.cartethyia.easyorange.framework.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String ADMIN_USER_TYPE = "00";

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final TokenService tokenService;
    private final SecurityProperties securityProperties;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return isPublicPath(path);
    }

    private boolean isPublicPath(String path) {
        return matchesAnyPattern(path, securityProperties.getIgnorePaths())
                || matchesAnyPattern(path, securityProperties.getProductPaths())
                || matchesAnyPattern(path, securityProperties.getStaticPaths());
    }

    private boolean matchesAnyPattern(String path, List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return patterns.stream().anyMatch(pattern -> {
            String normalizedPattern = pattern.startsWith("/") ? pattern : "/" + pattern;
            if (normalizedPattern.endsWith("/**")) {
                String prefix = normalizedPattern.substring(0, normalizedPattern.length() - 2);
                return normalizedPath.startsWith(prefix);
            }
            return normalizedPath.equals(normalizedPattern) || normalizedPath.startsWith(normalizedPattern + "/");
        });
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token == null) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Optional<Claims> claimsOpt = jwtUtil.parseToken(token);
            if (claimsOpt.isEmpty()) {
                log.warn("Invalid JWT token received from IP: {}, path: {}",
                    RequestUtil.getClientIp(request), request.getRequestURI());
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            Claims claims = claimsOpt.get();
            String uuid = claims.get("uuid", String.class);
            if (uuid == null || !Boolean.TRUE.equals(stringRedisTemplate.hasKey(getTokenKey(uuid)))) {
                log.warn("Invalid or revoked JWT token received from IP: {}, path: {}",
                    RequestUtil.getClientIp(request), request.getRequestURI());
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                Long userId = Long.parseLong(claims.getSubject());
                setAuthentication(request, response, token, userId, claims);
            }
        } catch (Exception e) {
            log.error("action=auth_error, error={}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private void setAuthentication(HttpServletRequest request, HttpServletResponse response, String token, Long userId, Claims claims) {
        List<SimpleGrantedAuthority> authorities = resolveAuthorities(claims);
        Set<String> roles = authorities.stream()
                .filter(a -> a.getAuthority().startsWith("ROLE_"))
                .map(a -> a.getAuthority().substring("ROLE_".length()))
                .collect(Collectors.toSet());

        AuthUser authUser = AuthUser.builder()
                .userId(userId)
                .username(claims.get("username", String.class))
                .roles(roles)
                .build();

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(authUser, null, authorities);

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        jwtUtil.renewTokenIfNeeded(token).ifPresent(newToken -> {
            response.setHeader("Authorization-New", jwtProperties.getTokenPrefix() + newToken);
        });
    }

    private List<SimpleGrantedAuthority> resolveAuthorities(Claims claims) {
        String userType = claims.get("userType", String.class);
        if (ADMIN_USER_TYPE.equals(userType)) {
            return List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_USER")
            );
        }
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        String prefix = jwtProperties.getTokenPrefix();
        if (bearerToken == null || !bearerToken.startsWith(prefix)) {
            return null;
        }
        return bearerToken.substring(prefix.length());
    }

    private String getTokenKey(String uuid) {
        return CacheConstants.Login.TOKEN_KEY + uuid;
    }
}
