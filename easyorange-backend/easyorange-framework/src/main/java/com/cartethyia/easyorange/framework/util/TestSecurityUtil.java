package com.cartethyia.easyorange.framework.util;

import org.springframework.security.core.context.SecurityContextHolder;

public final class TestSecurityUtil {

    private TestSecurityUtil() {}

    public static void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}
