package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.port.output.PasswordEncoderPort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AuthenticationDomainService {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final LoginSecurityDomainService loginSecurityDomainService;
    private final SmsCodeDomainService smsCodeDomainService;

    public User authenticateByPassword(String account, String password, String clientIp) {
        loginSecurityDomainService.checkLoginAttempts(account);

        User user = userRepository.findByAccount(account).orElse(null);

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            loginSecurityDomainService.recordFailedAttempt(account);
            throw BusinessException.of(UserResultCode.INVALID_CREDENTIALS);
        }

        if (!user.isNormal()) {
            loginSecurityDomainService.recordFailedAttempt(account);
            throw BusinessException.of(UserResultCode.USER_DISABLED);
        }

        loginSecurityDomainService.clearLoginAttempts(account);

        User loggedIn = user.recordLogin(clientIp);
        userRepository.update(loggedIn);

        return loggedIn;
    }

    public User authenticateBySms(String phone, String verifyCode, String clientIp) {
        smsCodeDomainService.verifyCode(phone, verifyCode);

        User user = userRepository.findByPhone(phone).orElse(null);

        if (user == null || !user.isNormal()) {
            throw BusinessException.of(UserResultCode.INVALID_CREDENTIALS);
        }

        User loggedIn = user.recordLogin(clientIp);
        userRepository.update(loggedIn);

        return loggedIn;
    }

    public User resetPassword(String phone, String verifyCode, String newPassword) {
        smsCodeDomainService.verifyCode(phone, verifyCode);

        User user = userRepository.findByPhone(phone).orElse(null);

        if (user == null) {
            return null;
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        User updated = user.changePassword(encodedPassword, null);
        userRepository.update(updated);

        return updated;
    }
}
