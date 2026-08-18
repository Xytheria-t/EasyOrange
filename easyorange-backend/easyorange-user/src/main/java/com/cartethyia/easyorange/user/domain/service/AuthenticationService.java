package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.port.PasswordEncoderPort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.valueobject.LoginCredential;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 登录认证领域服务 — 校验登录凭据（密码 / 短信）并返回认证用户。
 * 密码生命周期（修改/重置）见 {@link PasswordManagementService}。
 */
@Component
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final LoginSecurityService loginSecurityService;
    private final SmsVerificationService smsVerificationService;

    public User authenticate(LoginCredential credential) {
        return switch (credential) {
            case LoginCredential.Password(String identifier, String password) ->
                authenticateByPassword(identifier, password);
            case LoginCredential.Sms(String phone, String verifyCode) -> authenticateBySms(phone, verifyCode);
        };
    }

    private User authenticateByPassword(String identifier, String password) {
        loginSecurityService.checkAndThrowIfLocked(identifier);

        var userOpt = userRepository.findByLoginIdentifier(identifier);
        if (userOpt.isEmpty()
                || !passwordEncoder.matches(password, userOpt.get().getPassword())) {
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
        smsVerificationService.verifyCodeOrThrow(phone, verifyCode);

        User user = userRepository
                .findByPhone(phone)
                .orElseThrow(() -> BusinessException.of(UserResultCode.INVALID_CREDENTIALS));

        if (!user.isEnabled()) {
            throw BusinessException.of(UserResultCode.USER_DISABLED);
        }

        return user;
    }
}
