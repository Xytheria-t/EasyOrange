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
 */
@Service
@RequiredArgsConstructor
public class CredentialAppService {

    private final PasswordManagementService passwordManagementService;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String phone, String verifyCode, String newPassword) {
        User updated = passwordManagementService.resetPassword(phone, verifyCode, newPassword);
        userRepository.update(updated);
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(String userId, String oldPassword, String newPassword) {
        User user =
                userRepository.findById(userId).orElseThrow(() -> BusinessException.of(UserResultCode.USER_NOT_FOUND));
        User updated = passwordManagementService.changePassword(user, oldPassword, newPassword);
        userRepository.update(updated);
        tokenService.revokeAllUserSessions(userId);
        domainEventPublisher.publish(new UserPasswordChangedEvent(UuidV7.generateId(), userId));
    }
}
