package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.port.PasswordEncoderPort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.valueobject.LoginCredential;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final LoginSecurityService loginSecurityService;
    private final SmsCodeService smsCodeService;

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
        if (!user.isNormal()) {
            throw BusinessException.of(UserResultCode.USER_DISABLED);
        }

        loginSecurityService.clearLoginAttempts(identifier);
        return finishLogin(user, clientIp);
    }

    private User authenticateBySms(String phone, String verifyCode, String clientIp) {
        smsCodeService.verifyCode(phone, verifyCode);

        User user = userRepository.findByPhone(phone).orElse(null);

        if (user == null || !user.isNormal()) {
            throw BusinessException.of(UserResultCode.INVALID_CREDENTIALS);
        }

        return finishLogin(user, clientIp);
    }

    public Optional<User> resetPassword(String phone, String verifyCode, String newPassword) {
        smsCodeService.verifyCode(phone, verifyCode);

        return userRepository.findByPhone(phone)
            .map(user -> {
                String encoded = passwordEncoder.encode(newPassword);
                User updated = user.changePassword(encoded, null);
                userRepository.update(updated);
                return updated;
            });
    }

    private User finishLogin(User user, String clientIp) {
        User loggedIn = user.recordLogin(clientIp);
        userRepository.update(loggedIn);
        return loggedIn;
    }
}
