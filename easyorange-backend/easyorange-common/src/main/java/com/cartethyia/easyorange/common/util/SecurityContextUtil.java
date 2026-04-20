package com.cartethyia.easyorange.common.util;

import com.cartethyia.easyorange.common.dto.LoginUser;
import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Spring Security 上下文工具类
 */
public final class SecurityContextUtil {

    private static final String ROLE_PREFIX = "ROLE_";

    private SecurityContextUtil() {
        throw new IllegalStateException("Utility class");
    }

    // ==================== Current User ====================

    public static Optional<Authentication> getAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }

    public static Optional<Long> getCurrentUserId() {
        return getAuthentication()
            .filter(Authentication::isAuthenticated)
            .map(Authentication::getPrincipal)
            .flatMap(SecurityContextUtil::convertPrincipal);
    }

    public static Long getCurrentUserIdOrThrow() {
        return getCurrentUserId()
            .orElseThrow(() -> BusinessException.of(ResultCode.UNAUTHORIZED, "用户未登录"));
    }

    public static Optional<String> getCurrentUsername() {
        return getAuthentication()
            .filter(Authentication::isAuthenticated)
            .map(auth -> {
                Object principal = auth.getPrincipal();
                if (principal instanceof LoginUser loginUser) {
                    return loginUser.getUsername();
                }
                return auth.getName();
            });
    }

    public static String getCurrentUsernameOrThrow() {
        return getCurrentUsername()
            .orElseThrow(() -> BusinessException.of(ResultCode.UNAUTHORIZED, "用户未登录"));
    }

    // ==================== Role & Authority Check ====================

    public static boolean isAuthenticated() {
        return getAuthentication().filter(Authentication::isAuthenticated).isPresent();
    }

    public static boolean hasRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        String normalizedRole = role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role;
        return hasMatchingAuthority(normalizedRole, authority -> authority.startsWith(ROLE_PREFIX));
    }

    public static boolean hasAuthority(String authority) {
        return authority != null && hasMatchingAuthority(authority, ignored -> true);
    }

    // ==================== User Context ====================

    public static Optional<LoginUser> getUserContext() {
        return getAuthentication()
            .filter(Authentication::isAuthenticated)
            .map(auth -> {
                Object principal = auth.getPrincipal();
                if (principal instanceof LoginUser loginUser) {
                    return loginUser;
                }
                Set<String> authorities = extractAuthorities(auth);
                return LoginUser.builder()
                    .userId(extractUserId(auth))
                    .username(auth.getName())
                    .roles(extractRoles(authorities))
                    .permissions(extractPermissions(authorities))
                    .build();
            });
    }

    public static LoginUser getUserContextOrThrow() {
        return getUserContext()
            .orElseThrow(() -> BusinessException.of(ResultCode.UNAUTHORIZED, "用户未登录"));
    }

    // ==================== Context Management ====================

    public static void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ==================== Private Methods ====================

    private static boolean hasMatchingAuthority(String target, Predicate<String> filter) {
        return getAuthentication()
            .filter(Authentication::isAuthenticated)
            .map(auth -> auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(filter)
                .anyMatch(target::equals))
            .orElse(false);
    }

    private static Set<String> extractAuthorities(Authentication auth) {
        return auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());
    }

    private static Long extractUserId(Authentication auth) {
        Object principal = auth.getPrincipal();
        return principal != null ? convertPrincipal(principal).orElse(null) : null;
    }

    private static Set<String> extractRoles(Set<String> authorities) {
        return authorities.stream()
            .filter(a -> a.startsWith(ROLE_PREFIX))
            .map(a -> a.substring(ROLE_PREFIX.length()))
            .collect(Collectors.toSet());
    }

    private static Set<String> extractPermissions(Set<String> authorities) {
        return authorities.stream()
            .filter(a -> !a.startsWith(ROLE_PREFIX))
            .collect(Collectors.toSet());
    }

    private static Optional<Long> convertPrincipal(Object principal) {
        return switch (principal) {
            case Long id -> Optional.of(id);
            case LoginUser loginUser -> Optional.ofNullable(loginUser.getUserId());
            case String s -> parseLongSafe(s);
            default -> Optional.empty();
        };
    }

    private static Optional<Long> parseLongSafe(String s) {
        try {
            return Optional.of(Long.parseLong(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
