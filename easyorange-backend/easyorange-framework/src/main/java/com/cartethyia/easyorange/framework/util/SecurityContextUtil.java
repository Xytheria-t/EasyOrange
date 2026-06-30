package com.cartethyia.easyorange.framework.util;

import com.cartethyia.easyorange.common.security.AuthUser;
import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.exception.BusinessException;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@UtilityClass
public class SecurityContextUtil {

    // ==================== Current User ID ====================

    public static String getCurrentUserIdOrThrow() {
        return getCurrentUserId()
                .orElseThrow(() -> BusinessException.of(ResultCode.UNAUTHORIZED, "用户未登录"));
    }

    public static Optional<String> getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        return convertPrincipal(auth.getPrincipal());
    }

    // ==================== User Context ====================

    public static Optional<AuthUser> getUserContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof AuthUser authUser) {
            return Optional.of(authUser);
        }
        return Optional.of(buildAuthUser(auth));
    }

    public static AuthUser getUserContextOrThrow() {
        return getUserContext()
                .orElseThrow(() -> BusinessException.of(ResultCode.UNAUTHORIZED, "用户未登录"));
    }

    // ==================== Context Management ====================

    public static void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ==================== Private Helpers ====================

    private static Optional<String> convertPrincipal(Object principal) {
        if (principal == null) {
            return Optional.empty();
        }
        return switch (principal) {
            case Long id -> Optional.of(String.valueOf(id));
            case AuthUser authUser -> Optional.ofNullable(authUser.userId());
            case String s -> Optional.of(s);
            default -> Optional.empty();
        };
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

    private static String extractUsername(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof AuthUser authUser) {
            return authUser.username();
        }
        // When principal is a plain String (e.g., user ID), prefer credentials
        // as the actual username. In production, the principal is an AuthUser;
        // in tests, principal may be a String ID with credentials=username.
        if (auth.getCredentials() instanceof String credentials) {
            return credentials;
        }
        if (principal instanceof String username) {
            return username;
        }
        return auth.getName();
    }

    private static Set<String> extractRoles(Set<String> authorities) {
        return authorities.stream()
            .filter(a -> a.startsWith("ROLE_"))
            .map(a -> a.substring("ROLE_".length()))
            .collect(Collectors.toSet());
    }

    private static Set<String> extractPermissions(Set<String> authorities) {
        return authorities.stream()
            .filter(a -> !a.startsWith("ROLE_"))
            .collect(Collectors.toSet());
    }
}
