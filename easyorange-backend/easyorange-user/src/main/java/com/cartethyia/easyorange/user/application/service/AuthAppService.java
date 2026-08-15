package com.cartethyia.easyorange.user.application.service;

import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.idgen.UuidV7;
import com.cartethyia.easyorange.framework.auth.TokenRotation;
import com.cartethyia.easyorange.framework.auth.TokenService;
import com.cartethyia.easyorange.framework.util.RequestUtil;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.user.application.dto.UserView;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
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
        User saved = userRepository.save(user);
        domainEventPublisher.publish(new UserRegisteredEvent(UuidV7.generateId(), saved.getId(), username));
        return saved.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public LoginContext login(LoginCredential credential) {
        User user = authenticationService.authenticate(credential);
        User loggedIn = user.recordLogin(RequestUtil.getClientIp());
        userRepository.update(loggedIn);
        var roles = loggedIn.getUserType().getDefaultRoles();
        String accessToken = tokenService.createAccessToken(user.getId(), user.getUsername(), roles);
        String refreshToken = tokenService.createRefreshToken(user.getId());
        return new LoginContext(UserView.from(loggedIn), accessToken, refreshToken);
    }

    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null) {
            tokenService.revokeAccessToken(accessToken);
        }
        if (refreshToken != null) {
            tokenService.revokeRefreshToken(refreshToken);
        }
        SecurityContextUtil.clearContext();
    }

    public void sendSmsCode(String phone) {
        if (!smsCodePort.send(phone)) {
            throw BusinessException.of(UserResultCode.SMS_CODE_SEND_TOO_FREQUENT);
        }
    }

    /**
     * 刷新令牌：消费旧 refresh 并签发新 access + refresh。
     * 轮换成功后重验用户状态（存在且启用），否则吊销该用户全部会话。
     */
    public RefreshResult refreshToken(String refreshToken) {
        TokenRotation rotation = tokenService.rotateRefreshToken(refreshToken);
        String userId = rotation.userId();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.isEnabled()) {
            tokenService.revokeAllUserSessions(userId);
            throw BusinessException.of(ResultCode.UNAUTHORIZED, "账号不存在或已被禁用，请重新登录");
        }
        var roles = user.getUserType().getDefaultRoles();
        String accessToken = tokenService.createAccessToken(userId, user.getUsername(), roles);
        return new RefreshResult(accessToken, rotation.newToken());
    }

    public record LoginContext(UserView user, String accessToken, String refreshToken) {}

    /** 刷新令牌结果：refresh 供 HttpOnly cookie 装配，access 供响应体。 */
    public record RefreshResult(String accessToken, String refreshToken) {}
}
