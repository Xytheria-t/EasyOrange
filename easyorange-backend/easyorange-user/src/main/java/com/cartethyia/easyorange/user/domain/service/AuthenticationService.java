package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.port.PasswordEncoderPort;
import com.cartethyia.easyorange.user.domain.port.SmsCodePort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.valueobject.LoginCredential;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final LoginSecurityService loginSecurityService;
    private final SmsCodePort smsCodePort;

    // ========== 登录认证 ==========

    public User authenticate(LoginCredential credential) {
        return switch (credential) {
            case LoginCredential.Password(String identifier, String password) ->
                authenticateByPassword(identifier, password);
            case LoginCredential.Sms(String phone, String verifyCode) ->
                authenticateBySms(phone, verifyCode);
        };
    }

    private User authenticateByPassword(String identifier, String password) {
        loginSecurityService.checkAndThrowIfLocked(identifier);

        var userOpt = userRepository.findByLoginIdentifier(identifier);
        if (userOpt.isEmpty() || !passwordEncoder.matches(password, userOpt.get().getPassword())) {
            loginSecurityService.incrementAndCheck(identifier);
            throw BusinessException.of(UserResultCode.INVALID_CREDENTIALS);
        }

        User user = userOpt.get();
        if (!user.isEnabled()) {
            throw BusinessException.of(UserResultCode.USER_DISABLED);
        }

        loginSecurityService.clear(identifier);
        return user;
    }

    private User authenticateBySms(String phone, String verifyCode) {
        verifyCodeOrThrow(phone, verifyCode);

        User user = userRepository.findByPhone(phone)
            .orElseThrow(() -> BusinessException.of(UserResultCode.INVALID_CREDENTIALS));

        if (!user.isEnabled()) {
            throw BusinessException.of(UserResultCode.USER_DISABLED);
        }

        return user;
    }

    // ========== 密码管理 ==========

    public User changePassword(User user, String oldPassword, String newPassword) {
        Objects.requireNonNull(user, "用户不能为空");
        Objects.requireNonNull(oldPassword, "旧密码不能为空");
        Objects.requireNonNull(newPassword, "新密码不能为空");

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw BusinessException.of(UserResultCode.INVALID_CREDENTIALS);
        }

        return doChangePassword(user, newPassword);
    }

    public User resetPassword(String phone, String verifyCode, String newPassword) {
        Objects.requireNonNull(newPassword, "新密码不能为空");
        verifyCodeOrThrow(phone, verifyCode);

        User user = userRepository.findByPhone(phone)
            .orElseThrow(() -> BusinessException.of(UserResultCode.USER_NOT_FOUND));

        return doChangePassword(user, newPassword);
    }

    private void verifyCodeOrThrow(String phone, String verifyCode) {
        switch (smsCodePort.verify(phone, verifyCode)) {
            case TOO_MANY_ATTEMPTS -> throw BusinessException.of(UserResultCode.SMS_CODE_VERIFY_TOO_FREQUENT);
            case NOT_FOUND -> throw BusinessException.of(UserResultCode.SMS_CODE_INVALID);
            case OK -> {}
        }
    }

    private User doChangePassword(User user, String newPassword) {
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw BusinessException.of(UserResultCode.PASSWORD_SAME_AS_OLD);
        }

        return user.changePassword(passwordEncoder.encode(newPassword), user.getId());
    }
}
