package com.cartethyia.easyorange.user.application.service;

import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.common.util.RequestUtil;
import com.cartethyia.easyorange.framework.service.TokenService;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.ForgotPasswordRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.LoginRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.RegisterRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.LoginResponse;
import com.cartethyia.easyorange.user.application.assembler.UserAssembler;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.event.ForgotPasswordEvent;
import com.cartethyia.easyorange.user.domain.event.UserRegisteredEvent;
import com.cartethyia.easyorange.user.domain.port.UserEventPort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.service.AuthenticationDomainService;
import com.cartethyia.easyorange.user.domain.service.SmsCodeDomainService;
import com.cartethyia.easyorange.user.domain.service.UserRegistrationDomainService;
import com.cartethyia.easyorange.user.infrastructure.util.NicknameGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthAppService {

    private final UserRepository userRepository;
    private final SmsCodeDomainService smsCodeDomainService;
    private final TokenService tokenService;
    private final UserAssembler userAssembler;
    private final UserEventPort userEventPort;
    private final NicknameGenerator nicknameGenerator;
    private final AuthenticationDomainService authenticationDomainService;
    private final UserRegistrationDomainService userRegistrationDomainService;

    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterRequest request) {
        String nickname = nicknameGenerator.generate();
        User user = userRegistrationDomainService.register(
            request.username(), 
            request.password(), 
            request.phone(), 
            request.email(), 
            nickname
        );

        User saved = userRepository.save(user);
        userEventPort.publish(new UserRegisteredEvent(saved.getId(), saved.getUsername()));

        return saved.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(LoginRequest loginRequest) {
        return switch (loginRequest.getEffectiveLoginMethod()) {
            case PASSWORD -> loginByPassword(loginRequest);
            case SMS -> loginBySms(loginRequest);
        };
    }

    public void logout(String accessToken, String refreshToken) {
        tokenService.revokeAllTokens(accessToken, refreshToken);
        SecurityContextUtil.clearContext();
    }

    public String refreshToken(String refreshToken) {
        String newToken = tokenService.refreshToken(refreshToken);
        BizRequire.notNull(newToken, ResultCode.UNAUTHORIZED);
        return newToken;
    }

    public void sendSmsCode(String phone) {
        smsCodeDomainService.sendCode(phone);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long forgotPassword(ForgotPasswordRequest request) {
        User user = authenticationDomainService.resetPassword(
            request.phone(), 
            request.verifyCode(), 
            request.newPassword(),
            smsCodeDomainService
        );

        if (user == null) {
            log.info("Password reset processed for phone: {}", maskPhone(request.phone()));
            return null;
        }

        userRepository.update(user);
        userEventPort.publish(new ForgotPasswordEvent(user.getId(), request.phone()));

        return user.getId();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private LoginResponse loginByPassword(LoginRequest loginRequest) {
        User user = authenticationDomainService.authenticateByPassword(
            loginRequest.account(), 
            loginRequest.password()
        );

        String clientIp = RequestUtil.getClientIp();
        userRepository.updateLoginInfo(user.getId(), clientIp);
        User loggedIn = user.recordLogin(clientIp);

        return buildLoginResponse(loggedIn);
    }

    private LoginResponse loginBySms(LoginRequest loginRequest) {
        User user = authenticationDomainService.authenticateBySms(
            loginRequest.account(), 
            loginRequest.password(),
            smsCodeDomainService
        );

        String clientIp = RequestUtil.getClientIp();
        userRepository.updateLoginInfo(user.getId(), clientIp);
        User loggedIn = user.recordLogin(clientIp);

        return buildLoginResponse(loggedIn);
    }

    private LoginResponse buildLoginResponse(User user) {
        String userTypeCode = user.getUserType() != null ? user.getUserType().getCode() : null;
        String accessToken = tokenService.createAccessToken(user.getId(), user.getUsername(), userTypeCode);
        String refreshToken = tokenService.createRefreshToken(user.getId(), user.getUsername(), userTypeCode);

        return userAssembler.toLoginResponse(user, accessToken, refreshToken);
    }
}
