package com.cartethyia.easyorange.user.application.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.Sex;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.port.AvatarFilePort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
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
        if (!hasAny(cmd)) throw BusinessException.of("没有需要更新的字段");

        checkUnique(cmd, currentUser);

        var updated = currentUser
            .updateContactInfo(cmd.email(), cmd.phone(), currentUser.getId())
            .updatePersonalInfo(cmd.realName(), cmd.nickname(),
                cmd.gender() != null ? Sex.fromCode(String.valueOf(cmd.gender())) : null,
                cmd.studentId(), currentUser.getId());

        userRepository.update(updated);
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
            return updated;
        } catch (Exception e) {
            throw BusinessException.of("头像上传失败", e);
        }
    }

    private void checkUnique(UpdateCommand cmd, User currentUser) {
        var contact = currentUser.getContactInfo();
        if (isPresent(cmd.email()) && !cmd.email().equals(contact != null ? contact.email() : null)
            && userRepository.findByEmail(cmd.email()).isPresent())
            throw BusinessException.of(UserResultCode.EMAIL_EXISTS);
        if (isPresent(cmd.phone()) && !cmd.phone().equals(contact != null ? contact.phone() : null)
            && userRepository.findByPhone(cmd.phone()).isPresent())
            throw BusinessException.of(UserResultCode.PHONE_EXISTS);

        var personal = currentUser.getPersonalInfo();
        if (isPresent(cmd.studentId()) && !cmd.studentId().equals(personal != null ? personal.studentId() : null)
            && userRepository.findByStudentId(cmd.studentId()).isPresent())
            throw BusinessException.of(UserResultCode.STUDENT_ID_EXISTS);
    }

    private static boolean hasAny(UpdateCommand cmd) {
        return isPresent(cmd.nickname()) || isPresent(cmd.email()) || isPresent(cmd.phone())
            || cmd.gender() != null || isPresent(cmd.realName()) || isPresent(cmd.studentId());
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
