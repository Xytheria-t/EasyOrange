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
import com.cartethyia.easyorange.user.domain.service.SmsVerificationService;
import com.cartethyia.easyorange.user.domain.valueobject.LoginCredential;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证应用服务 — 注册 / 登录 / 登出 / 刷新 / 验证码下发 的用例编排。
 * <p>
 * 取舍：注册走「<b>先查重再消费验证码</b>」——用户名或手机号已存在时用户只改字段重试，
 * 不必为一次必然失败的注册再取一次码；代价是多一次用户名校验查询，换来失败路径上省掉短信费用与等待。
 * 登录成功后同事务补记登录痕迹（IP + 时间），角色由 userType 直接映射，不重算。
 * <p>
 * 边界：刷新令牌经框架完成 refresh 轮换与复用检测；轮换后重验用户状态，账号已删除或禁用即
 * <b>吊销该用户全部会话</b>并抛 401（凭据已作废，不能只让这一次刷新失败）；
 * 登出 access / refresh 各自吊销，缺失的令牌静默跳过（登出必须幂等）。
 */
@Service
@RequiredArgsConstructor
public class AuthAppService {

    private final AuthenticationService authenticationService;
    private final RegistrationService registrationService;
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final SmsCodePort smsCodePort;
    private final SmsVerificationService smsVerificationService;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional(rollbackFor = Exception.class)
    public String register(String username, String password, String phone, String verifyCode) {
        // 先查重再消费验证码：用户名/手机号已存在时用户只需改字段重试，不用重新取码
        registrationService.validateRegisterable(username, phone);
        smsVerificationService.verifyCodeOrThrow(phone, verifyCode);
        User user = registrationService.registerNewUser(username, password, phone);
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

    /** 预检验证码正确性（不消费）— 忘记密码第二步「下一步」即时反馈。 */
    public void verifySmsCode(String phone, String verifyCode) {
        smsVerificationService.checkCodeOrThrow(phone, verifyCode);
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
