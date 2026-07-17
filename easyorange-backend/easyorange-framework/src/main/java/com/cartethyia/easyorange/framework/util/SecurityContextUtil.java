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
        return getAuthentication()
                .flatMap(auth -> convertPrincipal(auth.getPrincipal()));
    }

    // ==================== User Context ====================

    public static Optional<AuthUser> getUserContext() {
        return getAuthentication().map(auth -> {
            if (auth.getPrincipal() instanceof AuthUser u) return u;
            return buildAuthUser(auth);
        });
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

    private static Optional<Authentication> getAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated);
    }

    private static Optional<String> convertPrincipal(Object principal) {
        if (principal == null) return Optional.empty();
        return switch (principal) {
            case Long id -> Optional.of(String.valueOf(id));
            case AuthUser user -> Optional.ofNullable(user.userId());
            case String s -> Optional.of(s);
            default -> Optional.empty();
        };
    }

    private static AuthUser buildAuthUser(Authentication auth) {
        var authorities = auth.getAuthorities().stream()
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
        // Principal is never AuthUser here (handled in getUserContext)
        if (auth.getCredentials() instanceof String credentials) return credentials;
        if (auth.getPrincipal() instanceof String s) return s;
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
