package com.cartethyia.easyorange.user.application.service;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.idgen.UuidV7;
import com.cartethyia.easyorange.framework.auth.TokenService;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.event.UserPasswordChangedEvent;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.service.PasswordManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 凭据（密码）应用服务 — 密码生命周期用例编排。
 * 与 {@link AuthAppService}（认证与会话）分离，各自聚焦单一职责。
 * <p>
 * 取舍：改密规则（新旧不同、编码）全在 {@link PasswordManagementService}，本类只做
 * 「加载 → 委托 → 落库 → 作废旧会话 → 发事件」的跑腿，不复制业务判断。
 * <p>
 * 边界：改密成功即吊销该用户全部会话并发 {@link UserPasswordChangedEvent}（source 区分
 * 自助 / 验证码找回 / 管理端重置）；发布点在落库之后，保证事件载荷里的用户已是新密码态。
 */
@Service
@RequiredArgsConstructor
public class CredentialAppService {

    private static final String SOURCE_SELF = "self";
    private static final String SOURCE_SMS = "sms";

    private final PasswordManagementService passwordManagementService;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String phone, String verifyCode, String newPassword) {
        User updated = passwordManagementService.resetPassword(phone, verifyCode, newPassword);
        userRepository.update(updated);
        String userId = updated.getId();
        // 找回密码不要求登录态，但改完凭证后旧会话同样失去归属（找回场景多为疑似泄露后的止损）
        tokenService.revokeAllUserSessions(userId);
        domainEventPublisher.publish(new UserPasswordChangedEvent(UuidV7.generateId(), userId, SOURCE_SMS));
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(String userId, String oldPassword, String newPassword) {
        User user =
                userRepository.findById(userId).orElseThrow(() -> BusinessException.of(UserResultCode.USER_NOT_FOUND));
        User updated = passwordManagementService.changePassword(user, oldPassword, newPassword);
        userRepository.update(updated);
        tokenService.revokeAllUserSessions(userId);
        domainEventPublisher.publish(new UserPasswordChangedEvent(UuidV7.generateId(), userId, SOURCE_SELF));
    }
}
