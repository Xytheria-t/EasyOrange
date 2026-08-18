package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.port.PasswordEncoderPort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 密码生命周期领域服务 — 修改密码与重置密码的业务规则。
 * 与 {@link AuthenticationService}（登录认证）分离，各自聚焦单一职责。
 */
@Component
@RequiredArgsConstructor
public class PasswordManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final SmsVerificationService smsVerificationService;

    /**
     * 修改密码（已登录）：校验旧密码后更新。
     */
    public User changePassword(User user, String oldPassword, String newPassword) {
        Objects.requireNonNull(user, "用户不能为空");
        Objects.requireNonNull(oldPassword, "旧密码不能为空");
        Objects.requireNonNull(newPassword, "新密码不能为空");

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw BusinessException.of(UserResultCode.INVALID_CREDENTIALS);
        }

        return doChangePassword(user, newPassword);
    }

    /**
     * 重置密码（忘记密码）：校验短信验证码后更新。
     */
    public User resetPassword(String phone, String verifyCode, String newPassword) {
        Objects.requireNonNull(newPassword, "新密码不能为空");
        smsVerificationService.verifyCodeOrThrow(phone, verifyCode);

        User user = userRepository
                .findByPhone(phone)
                .orElseThrow(() -> BusinessException.of(UserResultCode.USER_NOT_FOUND));

        return doChangePassword(user, newPassword);
    }

    private User doChangePassword(User user, String newPassword) {
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw BusinessException.of(UserResultCode.PASSWORD_SAME_AS_OLD);
        }

        return user.changePassword(passwordEncoder.encode(newPassword), user.getId());
    }
}
