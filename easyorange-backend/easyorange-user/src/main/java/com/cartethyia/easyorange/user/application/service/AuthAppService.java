package com.cartethyia.easyorange.user.application.service;

import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.common.util.RequestUtil;
import com.cartethyia.easyorange.framework.service.TokenService;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.user.application.assembler.UserAssembler;
import com.cartethyia.easyorange.user.common.enums.LoginMethod;
import com.cartethyia.easyorange.user.common.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.model.User;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.service.PasswordDomainService;
import com.cartethyia.easyorange.user.domain.service.LoginSecurityDomainService;
import com.cartethyia.easyorange.user.domain.service.SmsCodeService;
import com.cartethyia.easyorange.user.dto.request.ForgotPasswordRequest;
import com.cartethyia.easyorange.user.dto.request.LoginRequest;
import com.cartethyia.easyorange.user.dto.request.RegisterRequest;
import com.cartethyia.easyorange.user.dto.response.LoginResponse;
import com.cartethyia.easyorange.user.infrastructure.event.UserEventPublisher;
import com.cartethyia.easyorange.user.util.NicknameGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthAppService {

    private final UserRepository userRepository;
    private final PasswordDomainService passwordDomainService;
    private final LoginSecurityDomainService loginSecurityDomainService;
    private final SmsCodeService smsCodeService;
    private final TokenService tokenService;
    private final UserAssembler userAssembler;
    private final UserEventPublisher userEventPublisher;
    private final NicknameGenerator nicknameGenerator;

    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterRequest request) {
        userRepository.findByUsername(request.getUsername())
            .ifPresent(u -> { throw BusinessException.of(UserResultCode.USERNAME_EXISTS); });

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            userRepository.findByPhone(request.getPhone())
                .ifPresent(u -> { throw BusinessException.of(UserResultCode.PHONE_EXISTS); });
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            userRepository.findByEmail(request.getEmail())
                .ifPresent(u -> { throw BusinessException.of(UserResultCode.EMAIL_EXISTS); });
        }

        String nickname = nicknameGenerator.generate();
        User user = User.register(request.getUsername(), passwordDomainService.encode(request.getPassword()), nickname);

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.updateInfo(request.getEmail(), request.getPhone(), null);
        } else if (request.getEmail() != null && !request.getEmail().isBlank()) {
            user.updateInfo(request.getEmail(), null, null);
        }

        User saved = userRepository.save(user);

        userEventPublisher.publishUserRegistered(saved.getId(), saved.getUsername());

        return saved.getId();
    }

    public LoginResponse login(LoginRequest loginRequest) {
        LoginMethod loginMethod = loginRequest.getEffectiveLoginMethod();

        return switch (loginMethod) {
            case PASSWORD -> loginByPassword(loginRequest);
            case SMS -> loginBySms(loginRequest);
        };
    }

    private LoginResponse loginByPassword(LoginRequest loginRequest) {
        String account = loginRequest.getAccount();
        String password = loginRequest.getPassword();

        BizRequire.notBlank(account, "账号不能为空");
        BizRequire.notBlank(password, "密码不能为空");

        loginSecurityDomainService.checkLoginAttempts(account);

        User user = userRepository.findByAccount(account).orElse(null);

        if (user == null || !user.isNormal() || !passwordDomainService.matches(password, user.getPassword())) {
            loginSecurityDomainService.recordFailedAttempt(account);
            throw BusinessException.of(UserResultCode.PASSWORD_ERROR, "账号或密码错误");
        }

        loginSecurityDomainService.clearLoginAttempts(account);

        log.info("action=login, method=password, account={}, userId={}, result=success",
            loginSecurityDomainService.maskAccount(account), user.getId());

        return buildLoginResponse(user);
    }

    private LoginResponse loginBySms(LoginRequest loginRequest) {
        String phone = loginRequest.getAccount();
        String verifyCode = loginRequest.getPassword();

        BizRequire.notBlank(phone, "手机号不能为空");
        BizRequire.notBlank(verifyCode, "验证码不能为空");

        smsCodeService.verifyCode(phone, verifyCode);

        User user = userRepository.findByPhone(phone)
            .orElseThrow(() -> BusinessException.of(UserResultCode.USER_NOT_FOUND, "该手机号未注册"));

        if (!user.isNormal()) {
            throw BusinessException.of(UserResultCode.USER_DISABLED);
        }

        log.info("action=login, method=sms, phone={}, userId={}, result=success",
            loginSecurityDomainService.maskAccount(phone), user.getId());

        return buildLoginResponse(user);
    }

    private LoginResponse buildLoginResponse(User user) {
        String accessToken = tokenService.createAccessToken(user.getId(), user.getUsername(),
            user.getUserType() != null ? user.getUserType().getCode() : null);
        String refreshToken = tokenService.createRefreshToken(user.getId(), user.getUsername(),
            user.getUserType() != null ? user.getUserType().getCode() : null);

        userRepository.updateLoginInfo(user.getId(), RequestUtil.getClientIp());

        return userAssembler.toLoginResponse(user, accessToken, refreshToken);
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
        smsCodeService.sendCode(phone);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long forgotPassword(ForgotPasswordRequest request) {
        smsCodeService.verifyCode(request.getPhone(), request.getVerifyCode());

        User user = userRepository.findByPhone(request.getPhone())
            .orElseThrow(() -> BusinessException.of(UserResultCode.USER_NOT_FOUND, "该手机号未注册"));

        String encodedPassword = passwordDomainService.encode(request.getNewPassword());
        boolean updated = userRepository.updatePassword(user.getId(), encodedPassword);

        BizRequire.requireTrue(updated, "重置密码失败，请稍后重试");

        userEventPublisher.publishForgotPassword(user.getId(), request.getPhone());

        return user.getId();
    }
}
