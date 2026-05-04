package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.shared.enums.UserResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationDomainService {

    private final UserRepository userRepository;
    private final PasswordDomainService passwordDomainService;
    private final LoginSecurityDomainService loginSecurityDomainService;

    public User authenticateByPassword(String account, String password) {
        loginSecurityDomainService.checkLoginAttempts(account);

        User user = userRepository.findByAccount(account).orElse(null);

        if (user == null || !passwordDomainService.matches(password, user.getPassword())) {
            loginSecurityDomainService.recordFailedAttempt(account);
            logAuthFailure(loginSecurityDomainService.maskAccount(account), "invalid_credentials");
            throw BusinessException.of(UserResultCode.INVALID_CREDENTIALS);
        }

        if (!user.isNormal()) {
            loginSecurityDomainService.recordFailedAttempt(account);
            logAuthFailure(loginSecurityDomainService.maskAccount(account), "user_disabled");
            throw BusinessException.of(UserResultCode.USER_DISABLED);
        }

        loginSecurityDomainService.clearLoginAttempts(account);
        return user;
    }

    public User authenticateBySms(String phone, String verifyCode, SmsCodeDomainService smsCodeDomainService) {
        smsCodeDomainService.verifyCode(phone, verifyCode);

        User user = userRepository.findByPhone(phone).orElse(null);

        if (user == null || !user.isNormal()) {
            logAuthFailure(maskPhone(phone), "invalid_credentials");
            throw BusinessException.of(UserResultCode.INVALID_CREDENTIALS);
        }

        return user;
    }

    public User resetPassword(String phone, String verifyCode, String newPassword, 
                              SmsCodeDomainService smsCodeDomainService) {
        smsCodeDomainService.verifyCode(phone, verifyCode);

        User user = userRepository.findByPhone(phone).orElse(null);

        if (user == null) {
            log.warn("Password reset attempted for non-existent phone: {}", maskPhone(phone));
            return null;
        }

        String encodedPassword = passwordDomainService.encode(newPassword);
        return user.changePassword(encodedPassword, null);
    }

    private void logAuthFailure(String maskedAccount, String reason) {
        log.warn("action=login, method=password, account={}, result=failed, reason={}", maskedAccount, reason);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
