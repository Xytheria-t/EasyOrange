package com.cartethyia.easyorange.user.application.service;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.idgen.UuidV7;
import com.cartethyia.easyorange.user.application.dto.UserView;
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
import com.cartethyia.easyorange.user.domain.valueobject.Avatar;
import com.cartethyia.easyorange.user.domain.valueobject.PersonalInfo;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileAppService {

    private final UserRepository userRepository;
    private final AvatarFilePort avatarFilePort;
    private final DomainEventPublisher domainEventPublisher;
    private final ProfileUpdateService profileUpdateService;

    public record UpdateCommand(
            String nickname, String email, String phone, String gender, String realName, String studentId) {}

    @Transactional(readOnly = true)
    public UserView getCurrentUser(String userId) {
        return UserView.from(findUserOrThrow(userId));
    }

    @Transactional(rollbackFor = Exception.class)
    public UserView updateUserInfo(String userId, UpdateCommand cmd) {
        User currentUser = findUserOrThrow(userId);
        if (!hasAny(cmd)) throw BusinessException.of("没有需要更新的字段");

        profileUpdateService.validateUniqueContact(cmd.email(), cmd.phone(), cmd.studentId(), currentUser);

        var updated = currentUser
                .updateContactInfo(new ContactUpdateSpec(cmd.email(), cmd.phone()), currentUser.getId())
                .updatePersonalInfo(
                        new PersonalUpdateSpec(
                                cmd.realName(),
                                cmd.nickname(),
                                cmd.gender() != null ? Sex.fromCode(cmd.gender()) : null,
                                cmd.studentId()),
                        currentUser.getId());

        userRepository.update(updated);
        domainEventPublisher.publish(new UserProfileUpdatedEvent(UuidV7.generateId(), currentUser.getId()));
        return UserView.from(updated);
    }

    @Transactional(rollbackFor = Exception.class)
    public UserView uploadAvatar(String userId, byte[] content, String contentType, String filename) {
        Avatar.validate(content);

        User currentUser = findUserOrThrow(userId);
        var currentAvatar = Optional.ofNullable(currentUser.getPersonalInfo())
                .map(PersonalInfo::avatar)
                .orElse(null);
        avatarFilePort.deleteIfExists(currentAvatar);

        try {
            var avatarUrl = avatarFilePort.upload(content, contentType, filename, currentUser.getId());
            Avatar avatar = Avatar.uploaded(avatarUrl, content, contentType);
            User updated = currentUser.changeAvatar(avatar, currentUser.getId());
            userRepository.update(updated);
            domainEventPublisher.publish(new UserAvatarChangedEvent(UuidV7.generateId(), currentUser.getId()));
            return UserView.from(updated);
        } catch (Exception e) {
            throw BusinessException.of("头像上传失败", e);
        }
    }

    /** 更新命令是否携带任何需要应用的字段（为空则无事可做，直接拒绝）。 */
    private boolean hasAny(UpdateCommand cmd) {
        return isPresent(cmd.nickname())
                || isPresent(cmd.email())
                || isPresent(cmd.phone())
                || cmd.gender() != null
                || isPresent(cmd.realName())
                || isPresent(cmd.studentId());
    }

    private User findUserOrThrow(String userId) {
        return userRepository.findById(userId).orElseThrow(() -> BusinessException.of(UserResultCode.USER_NOT_FOUND));
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
