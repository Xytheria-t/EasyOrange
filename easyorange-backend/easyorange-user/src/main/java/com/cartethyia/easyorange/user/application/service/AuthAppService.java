package com.cartethyia.easyorange.user.application.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.auth.TokenRefreshResult;
import com.cartethyia.easyorange.framework.auth.TokenService;
import com.cartethyia.easyorange.framework.util.RequestUtil;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.port.SmsCodePort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final SmsCodePort smsCodePort;

    @Transactional(rollbackFor = Exception.class)
    public Long register(String username, String password) {
        return registrationService.registerNewUser(username, password).getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public LoginContext login(LoginCredential credential) {
        User user = authenticationService.authenticate(credential, RequestUtil.getClientIp());
        String accessToken = tokenService.createAccessToken(user.getId(), user.getUsername(), user.getUserType().getCode());
        String refreshToken = tokenService.createRefreshToken(user.getId(), user.getUsername(), user.getUserType().getCode());
        return new LoginContext(user, accessToken, refreshToken);
    }

    public void logout(String refreshToken) {
        if (refreshToken != null) {
            tokenService.invalidateToken(refreshToken);
        }
        SecurityContextUtil.clearContext();
    }

    public void sendSmsCode(String phone) {
        if (!smsCodePort.send(phone)) {
            throw BusinessException.of(UserResultCode.SMS_CODE_SEND_TOO_FREQUENT);
        }
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
        User user = userRepository.findById(userId)
            .orElseThrow(() -> BusinessException.of(UserResultCode.USER_NOT_FOUND));
        authenticationService.resetPassword(user.getContactInfo().phone(), verifyCode, newPassword);
    }

    public record LoginContext(User user, String accessToken, String refreshToken) {}
}
