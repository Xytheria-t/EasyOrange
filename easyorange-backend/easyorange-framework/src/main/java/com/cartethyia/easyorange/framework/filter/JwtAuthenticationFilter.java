package com.cartethyia.easyorange.framework.filter;

import com.cartethyia.easyorange.common.dto.AuthUser;
import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import com.cartethyia.easyorange.framework.config.properties.SecurityProperties;
import com.cartethyia.easyorange.framework.service.TokenService;
import com.cartethyia.easyorange.framework.util.JwtUtil;
import com.cartethyia.easyorange.framework.util.RequestUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String SUPER_ADMIN_USER_TYPE = "00";
    private static final String MANAGER_USER_TYPE = "02";
    private static final String ADMIN_PATH_PREFIX = "/api/admin";

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final TokenService tokenService;
    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;

    private static final Set<String> AUTH_REQUIRED_PRODUCT_PATHS = Set.of("/api/products/my");

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if (matchesAnyPattern(path, securityProperties.getIgnorePaths())
                || matchesAnyPattern(path, securityProperties.getStaticPaths())) {
            return true;
        }
        if ("GET".equals(method) && matchesAnyPattern(path, securityProperties.getProductPaths())) {
            return !AUTH_REQUIRED_PRODUCT_PATHS.contains(path);
        }
        return false;
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
                log.warn("JWT 无效 - IP: {}, path: {}",
                    RequestUtil.getClientIp(request), request.getRequestURI());
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            Claims claims = claimsOpt.get();
            String jti = claims.get("jti", String.class);
            String tokenType = claims.get("type", String.class);

            // 校验 jti 未失效（登出黑名单）
            if (jti == null || tokenService.verifyTokenAndGetUserId(token) == null) {
                log.warn("JWT 已失效 - IP: {}, path: {}",
                    RequestUtil.getClientIp(request), request.getRequestURI());
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            // 禁止 refresh token 访问 API
            if ("refresh".equals(tokenType)) {
                log.warn("Refresh token 用于 API 访问 - IP: {}, path: {}",
                    RequestUtil.getClientIp(request), request.getRequestURI());
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                Long userId = Long.parseLong(claims.getSubject());
                setAuthentication(request, userId, claims);
            }

            // 管理端路径权限检查
            String path = request.getRequestURI();
            String userType = claims.get("userType", String.class);
            if (path.startsWith(ADMIN_PATH_PREFIX) && !isAdminUserType(userType)) {
                log.warn("非管理员访问管理端 - path: {}, userType: {}, IP: {}",
                    path, userType, RequestUtil.getClientIp(request));
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                objectMapper.writeValue(response.getOutputStream(), Result.error(ResultCode.FORBIDDEN));
                return;
            }
        } catch (Exception e) {
            log.error("认证过滤器异常: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private void setAuthentication(HttpServletRequest request, Long userId, Claims claims) {
        String userType = claims.get("userType", String.class);
        List<SimpleGrantedAuthority> authorities = resolveAuthorities(userType);
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
    }

    private List<SimpleGrantedAuthority> resolveAuthorities(String userType) {
        if (isAdminUserType(userType)) {
            return List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_USER")
            );
        }
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        String prefix = jwtProperties.getTokenPrefix();
        if (bearerToken == null || !bearerToken.startsWith(prefix)) {
            return null;
        }
        return bearerToken.substring(prefix.length());
    }

    private static boolean isAdminUserType(String userType) {
        return SUPER_ADMIN_USER_TYPE.equals(userType) || MANAGER_USER_TYPE.equals(userType);
    }
}
