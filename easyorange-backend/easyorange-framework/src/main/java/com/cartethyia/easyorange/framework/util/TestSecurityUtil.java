package com.cartethyia.easyorange.framework.util;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

public final class TestSecurityUtil {

    private TestSecurityUtil() {
    }

    public static void setSecurityContext(Long userId) {
        setSecurityContext(userId != null ? String.valueOf(userId) : null);
    }

    public static void setSecurityContext(Long userId, String... roles) {
        setSecurityContext(userId != null ? String.valueOf(userId) : null, roles);
    }

    public static void setSecurityContext(String userId) {
        var auth = new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    public static void setSecurityContext(String userId, String... roles) {
        var authorities = List.of(roles).stream()
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .toList();
        var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    public static void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}