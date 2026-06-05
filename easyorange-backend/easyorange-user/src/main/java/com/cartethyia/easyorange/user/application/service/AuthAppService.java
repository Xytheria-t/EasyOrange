package com.cartethyia.easyorange.user.application.service;

import com.cartethyia.easyorange.framework.auth.TokenRefreshResult;
import com.cartethyia.easyorange.framework.auth.TokenService;
import com.cartethyia.easyorange.framework.util.RequestUtil;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.service.AuthenticationService;
import com.cartethyia.easyorange.user.domain.service.RegistrationService;
import com.cartethyia.easyorange.user.domain.valueobject.LoginCredential;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthAppService {

    private final AuthenticationService authenticationService;
    private final RegistrationService registrationService;
    private final TokenService tokenService;

    @Transactional(rollbackFor = Exception.class)
    public Long register(String username, String password) {
        return registrationService.registerNewUser(username, password).getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public User login(LoginCredential credential) {
        return authenticationService.authenticate(credential, RequestUtil.getClientIp());
    }

    public void logout(String refreshToken) {
        if (refreshToken != null) {
            tokenService.invalidateToken(refreshToken);
        }
        SecurityContextUtil.clearContext();
    }

    public TokenRefreshResult refreshToken(String refreshToken) {
        return tokenService.refreshToken(refreshToken);
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String phone, String verifyCode, String newPassword) {
        authenticationService.resetPassword(phone, verifyCode, newPassword);
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(String verifyCode, String newPassword) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        authenticationService.resetPassword(userId, verifyCode, newPassword);
    }
}
