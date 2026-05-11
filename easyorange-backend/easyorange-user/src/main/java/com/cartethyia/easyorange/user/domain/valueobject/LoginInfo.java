package com.cartethyia.easyorange.user.domain.valueobject;

import java.time.LocalDateTime;

public record LoginInfo(
    String loginIp,
    LocalDateTime loginDate,
    LocalDateTime pwdUpdateDate
) {
    public LoginInfo {
        if (loginIp != null && loginIp.isBlank()) {
            throw new IllegalArgumentException("loginIp must not be blank");
        }
    }

    public static LoginInfo initial() {
        return new LoginInfo(null, null, null);
    }

    public LoginInfo recordLogin(String ip) {
        if (ip == null || ip.isBlank()) {
            throw new IllegalArgumentException("login ip must not be blank");
        }
        return new LoginInfo(ip, LocalDateTime.now(), pwdUpdateDate);
    }

    public LoginInfo updatePasswordTime() {
        return new LoginInfo(loginIp, loginDate, LocalDateTime.now());
    }
}
