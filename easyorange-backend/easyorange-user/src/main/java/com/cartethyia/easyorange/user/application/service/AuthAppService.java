package com.cartethyia.easyorange.user.application.service;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.auth.TokenRefreshResult;
import com.cartethyia.easyorange.framework.auth.TokenService;
import com.cartethyia.easyorange.framework.util.RequestUtil;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.event.UserPasswordChangedEvent;
import com.cartethyia.easyorange.user.domain.event.UserRegisteredEvent;
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
    private final DomainEventPublisher domainEventPublisher;

    @Transactional(rollbackFor = Exception.class)
    public String register(String username, String password) {
        User user = registrationService.registerNewUser(username, password);
        domainEventPublisher.publish(new UserRegisteredEvent(user.getId(), username));
        return user.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public LoginContext login(LoginCredential credential) {
        User user = authenticationService.authenticate(credential);
        User loggedIn = user.recordLogin(RequestUtil.getClientIp());
        userRepository.update(loggedIn);
        var roles = loggedIn.getUserType().getDefaultRoles();
        String accessToken = tokenService.createAccessToken(user.getId(), user.getUsername(), roles);
        String refreshToken = tokenService.createRefreshToken(user.getId(), user.getUsername(), roles);
        return new LoginContext(user, accessToken, refreshToken);
    }

    public void logout(String refreshToken) {
        if (refreshToken != null) {
            tokenService.invalidateToken(refreshToken);
        }
        SecurityContextUtil.clearContext();
    }

    @Transactional(readOnly = true)
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
    public void changePassword(String oldPassword, String newPassword) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        User user = userRepository.findById(userId)
            .orElseThrow(() -> BusinessException.of(UserResultCode.USER_NOT_FOUND));
        authenticationService.changePassword(user, oldPassword, newPassword);
        domainEventPublisher.publish(new UserPasswordChangedEvent(userId));
    }

    public record LoginContext(User user, String accessToken, String refreshToken) {}
}
