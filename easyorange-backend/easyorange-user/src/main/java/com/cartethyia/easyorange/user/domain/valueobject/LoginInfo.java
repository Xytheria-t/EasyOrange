package com.cartethyia.easyorange.user.domain.valueobject;

import java.time.LocalDateTime;

public record LoginInfo(
    String loginIp,
    LocalDateTime loginDate,
    LocalDateTime pwdUpdateDate
) {
    public static LoginInfo initial() {
        return new LoginInfo(null, null, null);
    }

    public LoginInfo recordLogin(String ip) {
        return new LoginInfo(ip, LocalDateTime.now(), pwdUpdateDate);
    }

    public LoginInfo updatePasswordTime() {
        return new LoginInfo(loginIp, loginDate, LocalDateTime.now());
    }
}
