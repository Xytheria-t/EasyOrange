package com.cartethyia.easyorange.user.dto.bo;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

/**
 * 忘记密码业务对象
 * 职责：封装忘记密码重置逻辑
 */
public record ForgotPasswordBo(
        String phone,
        String newPassword
) {

    /**
     * 加密新密码
     */
    public String encodePassword(PasswordEncoder encoder) {
        return encoder.encode(newPassword);
    }

    /**
     * 获取密码更新时间
     */
    public LocalDateTime getPasswordUpdateTime() {
        return LocalDateTime.now();
    }
}
