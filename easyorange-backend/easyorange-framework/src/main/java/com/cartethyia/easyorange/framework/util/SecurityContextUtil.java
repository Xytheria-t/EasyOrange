package com.cartethyia.easyorange.framework.util;

import com.cartethyia.easyorange.common.dto.AuthUser;
import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.exception.BusinessException;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@UtilityClass
public class SecurityContextUtil {

    private static final String ROLE_PREFIX = "ROLE_";

    // ==================== Authentication ====================

    public static Optional<Authentication> getAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }

    public static boolean isAuthenticated() {
        return getAuthentication().filter(Authentication::isAuthenticated).isPresent();
    }

    // ==================== Current User ID ====================

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

    // ==================== User Context ====================

    public static Optional<AuthUser> getUserContext() {
        return getAuthentication()
            .filter(Authentication::isAuthenticated)
            .map(auth -> {
                Object principal = auth.getPrincipal();
                if (principal instanceof AuthUser authUser) {
                    return authUser;
                }
                return buildAuthUser(auth);
            });
    }

    public static AuthUser getUserContextOrThrow() {
        return getUserContext()
            .orElseThrow(() -> BusinessException.of(ResultCode.UNAUTHORIZED, "用户未登录"));
    }

    // ==================== Role & Authority Check ====================

    public static boolean hasRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        String normalizedRole = role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role;
        return hasMatchingAuthority(normalizedRole, roleToCheck -> roleToCheck.equals(normalizedRole));
    }

    public static boolean hasAuthority(String authority) {
        return authority != null && hasMatchingAuthority(authority, ignored -> true);
    }

    // ==================== Context Management ====================

    public static void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ==================== Private Helper Methods ====================

    private static boolean hasMatchingAuthority(String target, Predicate<String> filter) {
        return getAuthentication()
            .filter(Authentication::isAuthenticated)
            .map(auth -> auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(filter)
                .anyMatch(target::equals))
            .orElse(false);
    }

    private static AuthUser buildAuthUser(Authentication auth) {
        Set<String> authorities = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());

        return AuthUser.builder()
            .userId(convertPrincipal(auth.getPrincipal()).orElse(null))
            .username(extractUsername(auth))
            .roles(extractRoles(authorities))
            .permissions(extractPermissions(authorities))
            .build();
    }

    private static Optional<Long> convertPrincipal(Object principal) {
        if (principal == null) {
            return Optional.empty();
        }
        return switch (principal) {
            case Long id -> Optional.of(id);
            case AuthUser authUser -> Optional.ofNullable(authUser.userId());
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

    private static String extractUsername(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof AuthUser authUser) {
            return authUser.username();
        }
        if (principal instanceof String username) {
            return username;
        }
        if (auth.getCredentials() instanceof String credentials) {
            return credentials;
        }
        return auth.getName();
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
}
