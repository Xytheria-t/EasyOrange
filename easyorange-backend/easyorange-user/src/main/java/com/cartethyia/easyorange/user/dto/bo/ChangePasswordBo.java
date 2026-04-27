package com.cartethyia.easyorange.user.dto.bo;

import com.cartethyia.easyorange.common.exception.BusinessException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

/**
 * 修改密码业务对象
 * 职责：封装密码修改业务逻辑，包括验证规则、密码加密
 */
public record ChangePasswordBo(
        String oldPassword,
        String newPassword
) {

    /**
     * 业务规则验证：新密码不能与旧密码相同
     */
    public void validateDifferentPassword() {
        if (oldPassword.equals(newPassword)) {
            throw BusinessException.of("新密码不能与旧密码相同");
        }
    }

    /**
     * 验证旧密码是否匹配
     */
    public boolean verifyOldPassword(PasswordEncoder encoder, String storedPassword) {
        return encoder.matches(oldPassword, storedPassword);
    }

    /**
     * 加密新密码
     */
    public String encodeNewPassword(PasswordEncoder encoder) {
        return encoder.encode(newPassword);
    }

    /**
     * 获取密码更新时间
     */
    public LocalDateTime getPasswordUpdateTime() {
        return LocalDateTime.now();
    }
}
