package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.port.PasswordEncoderPort;
import com.cartethyia.easyorange.user.domain.port.SmsCodePort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.valueobject.LoginCredential;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final LoginSecurityService loginSecurityService;
    private final SmsCodePort smsCodePort;

    // ========== 登录认证 ==========

    /**
     * Authenticates a user using the provided login credential (password or SMS).
     *
     * @param credential the login credential, either password-based or SMS-based
     * @param clientIp   the client IP address for login tracking
     * @return the authenticated and logged-in user aggregate
     * @throws BusinessException if credentials are invalid or the user is disabled
     */
    public User authenticate(LoginCredential credential, String clientIp) {
        return switch (credential) {
            case LoginCredential.Password(String identifier, String password) ->
                authenticateByPassword(identifier, password, clientIp);
            case LoginCredential.Sms(String phone, String verifyCode) ->
                authenticateBySms(phone, verifyCode, clientIp);
        };
    }

    private User authenticateByPassword(String identifier, String password, String clientIp) {
        loginSecurityService.checkLoginAttempts(identifier);

        User user = userRepository.findByLoginIdentifier(identifier).orElse(null);

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            loginSecurityService.recordFailedAttempt(identifier);
            throw BusinessException.of(UserResultCode.INVALID_CREDENTIALS);
        }
        if (!user.isEnabled()) {
            throw BusinessException.of(UserResultCode.USER_DISABLED);
        }

        loginSecurityService.clearLoginAttempts(identifier);

        User loggedIn = user.recordLogin(clientIp);
        userRepository.update(loggedIn);
        return loggedIn;
    }

    private User authenticateBySms(String phone, String verifyCode, String clientIp) {
        verifyCodeOrThrow(phone, verifyCode);

        User user = userRepository.findByPhone(phone)
            .orElseThrow(() -> BusinessException.of(UserResultCode.INVALID_CREDENTIALS));
        if (!user.isEnabled()) {
            throw BusinessException.of(UserResultCode.INVALID_CREDENTIALS);
        }

        User loggedIn = user.recordLogin(clientIp);
        userRepository.update(loggedIn);
        return loggedIn;
    }

    // ========== 密码管理 ==========

    public User resetPassword(String phone, String verifyCode, String newPassword) {
        verifyCodeOrThrow(phone, verifyCode);

        User user = userRepository.findByPhone(phone)
            .orElseThrow(() -> BusinessException.of(UserResultCode.USER_NOT_FOUND));

        return doChangePassword(user, newPassword);
    }

    public User resetPassword(Long userId, String verifyCode, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> BusinessException.of(UserResultCode.USER_NOT_FOUND));

        verifyCodeOrThrow(user.getContactInfo().phone(), verifyCode);
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

        User updated = user.changePassword(passwordEncoder.encode(newPassword), user.getId());
        userRepository.update(updated);
        return updated;
    }
}
