package com.cartethyia.easyorange.user.application.service;

import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.exception.BusinessException;
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
import com.cartethyia.easyorange.user.domain.port.UserEventPort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.service.LoginSecurityDomainService;
import com.cartethyia.easyorange.user.domain.service.PasswordDomainService;
import com.cartethyia.easyorange.user.domain.service.SmsCodeDomainService;
import com.cartethyia.easyorange.user.domain.shared.enums.LoginMethod;
import com.cartethyia.easyorange.user.domain.shared.enums.UserResultCode;
import com.cartethyia.easyorange.user.infrastructure.util.NicknameGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthAppService {

    private final UserRepository userRepository;
    private final PasswordDomainService passwordDomainService;
    private final LoginSecurityDomainService loginSecurityDomainService;
    private final SmsCodeDomainService smsCodeDomainService;
    private final TokenService tokenService;
    private final UserAssembler userAssembler;
    private final UserEventPort userEventPort;
    private final NicknameGenerator nicknameGenerator;

    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterRequest request) {
        validateUsernameNotExists(request.username());
        validateUniqueContactInfo(request.phone(), request.email());

        User user = createUser(request);
        User saved = userRepository.save(user);

        userEventPort.publishUserRegistered(saved.getId(), saved.getUsername());

        return saved.getId();
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest loginRequest) {
        LoginMethod loginMethod = loginRequest.getEffectiveLoginMethod();

        return switch (loginMethod) {
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
        smsCodeDomainService.verifyCode(request.phone(), request.verifyCode());

        User user = userRepository.findByPhone(request.phone())
            .orElseThrow(() -> BusinessException.of(UserResultCode.USER_NOT_FOUND, "该手机号未注册"));

        String encodedPassword = passwordDomainService.encode(request.newPassword());
        boolean updated = userRepository.updatePassword(user.getId(), encodedPassword);

        BizRequire.requireTrue(updated, "重置密码失败，请稍后重试");

        userEventPort.publishForgotPassword(user.getId(), request.phone());

        return user.getId();
    }

    private void validateUsernameNotExists(String username) {
        userRepository.findByUsername(username)
            .ifPresent(ignored -> { throw BusinessException.of(UserResultCode.USERNAME_EXISTS); });
    }

    private void validateUniqueContactInfo(String phone, String email) {
        if (StringUtils.hasText(phone)) {
            userRepository.findByPhone(phone)
                .ifPresent(ignored -> { throw BusinessException.of(UserResultCode.PHONE_EXISTS); });
        }
        if (StringUtils.hasText(email)) {
            userRepository.findByEmail(email)
                .ifPresent(ignored -> { throw BusinessException.of(UserResultCode.EMAIL_EXISTS); });
        }
    }

    private User createUser(RegisterRequest request) {
        String nickname = nicknameGenerator.generate();
        User user = User.register(
            request.username(),
            passwordDomainService.encode(request.password()),
            nickname
        );

        if (StringUtils.hasText(request.phone()) || StringUtils.hasText(request.email())) {
            user = user.updateProfile(request.email(), request.phone(), null, null);
        }

        return user;
    }

    private LoginResponse loginByPassword(LoginRequest loginRequest) {
        String account = loginRequest.account();
        String password = loginRequest.password();

        BizRequire.notBlank(account, "账号不能为空");
        BizRequire.notBlank(password, "密码不能为空");

        loginSecurityDomainService.checkLoginAttempts(account);

        User user = userRepository.findByAccount(account).orElse(null);

        if (user == null || !passwordDomainService.matches(password, user.getPassword())) {
            loginSecurityDomainService.recordFailedAttempt(account);
            logAuthWarn("method", "password", "account", loginSecurityDomainService.maskAccount(account),
                "result", "failed", "reason", "invalid_credentials");
            throw BusinessException.of(UserResultCode.PASSWORD_ERROR, "账号或密码错误");
        }

        if (!user.isNormal()) {
            loginSecurityDomainService.recordFailedAttempt(account);
            logAuthWarn("method", "password", "account", loginSecurityDomainService.maskAccount(account),
                "userId", user.getId(), "result", "failed", "reason", "user_disabled");
            throw BusinessException.of(UserResultCode.USER_DISABLED);
        }

        loginSecurityDomainService.clearLoginAttempts(account);

        return buildLoginResponse(user);
    }

    private LoginResponse loginBySms(LoginRequest loginRequest) {
        String phone = loginRequest.account();
        String verifyCode = loginRequest.password();

        BizRequire.notBlank(phone, "手机号不能为空");
        BizRequire.notBlank(verifyCode, "验证码不能为空");

        smsCodeDomainService.verifyCode(phone, verifyCode);

        User user = userRepository.findByPhone(phone)
            .orElseThrow(() -> BusinessException.of(UserResultCode.USER_NOT_FOUND, "该手机号未注册"));

        if (!user.isNormal()) {
            throw BusinessException.of(UserResultCode.USER_DISABLED);
        }

        return buildLoginResponse(user);
    }

    private void logAuthWarn(Object... kvs) {
        StringBuilder sb = new StringBuilder("action=login");
        for (int i = 0; i < kvs.length; i += 2) {
            sb.append(", ").append(kvs[i]).append("={}");
        }
        log.warn(sb.toString(), kvs);
    }

    private LoginResponse buildLoginResponse(User user) {
        String userTypeCode = user.getUserType() != null ? user.getUserType().getCode() : null;
        String accessToken = tokenService.createAccessToken(user.getId(), user.getUsername(), userTypeCode);
        String refreshToken = tokenService.createRefreshToken(user.getId(), user.getUsername(), userTypeCode);

        userRepository.updateLoginInfo(user.getId(), RequestUtil.getClientIp());

        return userAssembler.toLoginResponse(user, accessToken, refreshToken);
    }
}
