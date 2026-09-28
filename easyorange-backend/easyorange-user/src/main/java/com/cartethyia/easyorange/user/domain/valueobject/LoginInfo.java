package com.cartethyia.easyorange.user.domain.valueobject;

import com.cartethyia.easyorange.common.exception.BusinessException;
import java.time.LocalDateTime;

public record LoginInfo(String loginIp, LocalDateTime loginDate, LocalDateTime pwdUpdateDate) {
    public LoginInfo {
        if (loginIp != null && loginIp.isBlank()) {
            throw BusinessException.of("登录 IP 不能为空");
        }
    }

    public static LoginInfo empty() {
        return new LoginInfo(null, null, null);
    }

    public LoginInfo recordLogin(String ip) {
        if (ip == null || ip.isBlank()) {
            throw BusinessException.of("登录 IP 不能为空");
        }
        return new LoginInfo(ip, LocalDateTime.now(), pwdUpdateDate);
    }

    public LoginInfo updatePasswordTime() {
        return new LoginInfo(loginIp, loginDate, LocalDateTime.now());
    }
}
