package com.cartethyia.easyorange.user.application.service;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.user.domain.aggregate.ContactUpdateSpec;
import com.cartethyia.easyorange.user.domain.aggregate.PersonalUpdateSpec;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.Sex;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.event.UserAvatarChangedEvent;
import com.cartethyia.easyorange.user.domain.event.UserProfileUpdatedEvent;
import com.cartethyia.easyorange.user.domain.port.AvatarFilePort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.service.ProfileUpdateService;
import com.cartethyia.easyorange.user.domain.valueobject.PersonalInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfileAppService {

    private static final long AVATAR_MAX_SIZE = 5 * 1024 * 1024;

    private final UserRepository userRepository;
    private final AvatarFilePort avatarFilePort;
    private final DomainEventPublisher domainEventPublisher;
    private final ProfileUpdateService profileUpdateService;

    public record UpdateCommand(
        String nickname, String email, String phone,
        Integer gender, String realName, String studentId
    ) {}

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        var userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return userRepository.findById(userId)
            .orElseThrow(() -> BusinessException.of(UserResultCode.USER_NOT_FOUND));
    }

    @Transactional(rollbackFor = Exception.class)
    public User updateUserInfo(UpdateCommand cmd) {
        User currentUser = getCurrentUser();
        if (!ProfileUpdateService.hasAny(cmd.nickname(), cmd.email(), cmd.phone(),
            cmd.gender(), cmd.realName(), cmd.studentId()))
            throw BusinessException.of("没有需要更新的字段");

        profileUpdateService.validateUniqueContact(cmd.email(), cmd.phone(), cmd.studentId(), currentUser);

        var updated = currentUser
            .updateContactInfo(new ContactUpdateSpec(cmd.email(), cmd.phone()), currentUser.getId())
            .updatePersonalInfo(new PersonalUpdateSpec(cmd.realName(), cmd.nickname(),
                cmd.gender() != null ? Sex.fromCode(String.valueOf(cmd.gender())) : null,
                cmd.studentId()), currentUser.getId());

        userRepository.update(updated);
        domainEventPublisher.publish(new UserProfileUpdatedEvent(currentUser.getId()));
        return updated;
    }

    @Transactional(rollbackFor = Exception.class)
    public User uploadAvatar(byte[] content, String contentType, String filename) {
        if (content == null || content.length == 0) throw BusinessException.of("头像不能为空");
        if (content.length > AVATAR_MAX_SIZE) throw BusinessException.of("头像大小超过限制，最大允许5MB");

        User currentUser = getCurrentUser();
        var currentAvatar = Optional.ofNullable(currentUser.getPersonalInfo())
            .map(PersonalInfo::avatar).orElse(null);
        avatarFilePort.deleteIfExists(currentAvatar);

        try {
            var avatarUrl = avatarFilePort.upload(content, contentType, filename, currentUser.getId());
            var updated = currentUser.changeAvatar(avatarUrl, currentUser.getId());
            userRepository.update(updated);
            domainEventPublisher.publish(new UserAvatarChangedEvent(currentUser.getId()));
            return updated;
        } catch (Exception e) {
            throw BusinessException.of("头像上传失败", e);
        }
    }

}
