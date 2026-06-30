package com.cartethyia.easyorange.user.application.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.Sex;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.port.AvatarFilePort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileAppService {

    private static final long AVATAR_MAX_SIZE = 5 * 1024 * 1024;

    private final UserRepository userRepository;
    private final AvatarFilePort avatarFilePort;

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return userRepository.findById(userId)
            .orElseThrow(() -> BusinessException.of(UserResultCode.USER_NOT_FOUND));
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(String nickname, String email, String phone,
                                Integer gender, String realName, String studentId) {
        User currentUser = getCurrentUserOrThrow();

        BizRequire.requireTrue(hasAnyChanges(nickname, email, phone, gender, realName, studentId), "没有需要更新的字段");

        validateUniqueFieldsIfChanged(email, phone, studentId, currentUser);

        User updatedUser = currentUser;
        String operatorId = currentUser.getId();

        boolean contactChanged = (email != null && !email.isBlank())
            || (phone != null && !phone.isBlank());
        if (contactChanged) {
            updatedUser = updatedUser.updateContactInfo(email, phone, operatorId);
        }

        boolean personalChanged = (realName != null && !realName.isBlank())
            || (nickname != null && !nickname.isBlank())
            || (gender != null)
            || (studentId != null && !studentId.isBlank());
        if (personalChanged) {
            updatedUser = updatedUser.updatePersonalInfo(
                realName, nickname,
                gender != null ? Sex.fromCode(String.valueOf(gender)) : null,
                studentId, operatorId);
        }

        userRepository.update(updatedUser);
    }

    @Transactional(rollbackFor = Exception.class)
    public void uploadAvatar(byte[] content, String contentType, String filename) {
        BizRequire.notNull(content, "头像不能为空");
        BizRequire.requireTrue(content.length > 0, "头像不能为空");

        if (content.length > AVATAR_MAX_SIZE) {
            throw BusinessException.of("头像大小超过限制，最大允许5MB");
        }

        User currentUser = getCurrentUserOrThrow();

        String currentAvatar = currentUser.getPersonalInfo() != null ? currentUser.getPersonalInfo().avatar() : null;
        avatarFilePort.deleteIfExists(currentAvatar);

        try {
            String avatarUrl = avatarFilePort.upload(content, contentType, filename, currentUser.getId());

            User updatedUser = currentUser.changeAvatar(avatarUrl, currentUser.getId());
            userRepository.update(updatedUser);
        } catch (Exception e) {
            throw BusinessException.of("头像上传失败", e);
        }
    }

    private static boolean hasAnyChanges(String nickname, String email, String phone,
                                          Integer gender, String realName, String studentId) {
        return nickname != null && !nickname.isBlank()
            || email != null && !email.isBlank()
            || phone != null && !phone.isBlank()
            || gender != null
            || realName != null && !realName.isBlank()
            || studentId != null && !studentId.isBlank();
    }

    private void validateUniqueFieldsIfChanged(String email, String phone, String studentId, User currentUser) {
        var contactInfo = currentUser.getContactInfo();
        String currentEmail = contactInfo != null ? contactInfo.email() : null;
        if (email != null && !email.isBlank() && !email.equals(currentEmail)) {
            if (userRepository.findByEmail(email).isPresent()) {
                throw BusinessException.of(UserResultCode.EMAIL_EXISTS);
            }
        }

        String currentPhone = contactInfo != null ? contactInfo.phone() : null;
        if (phone != null && !phone.isBlank() && !phone.equals(currentPhone)) {
            if (userRepository.findByPhone(phone).isPresent()) {
                throw BusinessException.of(UserResultCode.PHONE_EXISTS);
            }
        }

        var personalInfo = currentUser.getPersonalInfo();
        String currentStudentId = personalInfo != null ? personalInfo.studentId() : null;
        if (studentId != null && !studentId.isBlank() && !studentId.equals(currentStudentId)) {
            if (userRepository.findByStudentId(studentId).isPresent()) {
                throw BusinessException.of(UserResultCode.STUDENT_ID_EXISTS);
            }
        }
    }

    private User getCurrentUserOrThrow() {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return userRepository.findById(userId)
            .orElseThrow(() -> BusinessException.of(UserResultCode.USER_NOT_FOUND));
    }
}
