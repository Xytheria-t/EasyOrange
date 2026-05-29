package com.cartethyia.easyorange.user.application.service.profile;

import com.cartethyia.easyorange.common.dto.AuthUser;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.user.application.assembler.UserAssembler;
import com.cartethyia.easyorange.user.application.command.UpdateUserCommand;
import com.cartethyia.easyorange.user.application.dto.UserProfileVO;
import com.cartethyia.easyorange.user.application.dto.UserVO;
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
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final UserAssembler userAssembler;

    @Transactional(readOnly = true)
    public UserProfileVO getUserInfo() {
        AuthUser authUser = SecurityContextUtil.getUserContextOrThrow();
        User user = userRepository.findById(authUser.userId())
            .orElseThrow(() -> BusinessException.of(UserResultCode.USER_NOT_FOUND));
        return userAssembler.toProfileVo(user, authUser.roles(), authUser.permissions(), authUser.loginTime());
    }

    @Transactional(rollbackFor = Exception.class)
    public UserVO updateUserInfo(UpdateUserCommand command) {
        User currentUser = getCurrentUserOrThrow();

        BizRequire.requireTrue(hasAnyUpdate(command), "没有需要更新的字段");

        validateUniqueFieldsIfChanged(command, currentUser);

        User updatedUser = currentUser;
        Long operatorId = currentUser.getId();

        boolean contactChanged = (command.getEmail() != null && !command.getEmail().isBlank())
            || (command.getPhone() != null && !command.getPhone().isBlank());
        if (contactChanged) {
            updatedUser = updatedUser.updateContactInfo(command.getEmail(), command.getPhone(), operatorId);
        }

        boolean personalChanged = (command.getRealName() != null && !command.getRealName().isBlank())
            || (command.getNickname() != null && !command.getNickname().isBlank())
            || (command.getGender() != null)
            || (command.getStudentId() != null && !command.getStudentId().isBlank());
        if (personalChanged) {
            updatedUser = updatedUser.updatePersonalInfo(
                command.getRealName(), command.getNickname(),
                command.getGender() != null ? Sex.fromCode(String.valueOf(command.getGender())) : null,
                command.getStudentId(), operatorId);
        }

        BizRequire.requireTrue(userRepository.update(updatedUser), "更新用户信息失败");
        return userAssembler.toVo(updatedUser);
    }

    @Transactional(rollbackFor = Exception.class)
    public UserVO uploadAvatar(byte[] content, String contentType, String filename) {
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
            BizRequire.requireTrue(userRepository.update(updatedUser), "更新头像失败");

            return userRepository.findById(updatedUser.getId())
                .map(userAssembler::toVo)
                .orElseThrow(() -> BusinessException.of(UserResultCode.USER_NOT_FOUND));
        } catch (Exception e) {
            throw BusinessException.of("头像上传失败", e);
        }
    }

    private boolean hasAnyUpdate(UpdateUserCommand command) {
        String requestStudentId = command.getStudentId();
        return command.getNickname() != null && !command.getNickname().isBlank()
            || command.getEmail() != null && !command.getEmail().isBlank()
            || command.getPhone() != null && !command.getPhone().isBlank()
            || command.getGender() != null
            || command.getRealName() != null && !command.getRealName().isBlank()
            || requestStudentId != null && !requestStudentId.isBlank();
    }

    private void validateUniqueFieldsIfChanged(UpdateUserCommand command, User currentUser) {
        var contactInfo = currentUser.getContactInfo();
        String currentEmail = contactInfo != null ? contactInfo.email() : null;
        if (command.getEmail() != null && !command.getEmail().isBlank() && !command.getEmail().equals(currentEmail)) {
            if (userRepository.findByEmail(command.getEmail()).isPresent()) {
                throw BusinessException.of(UserResultCode.EMAIL_EXISTS);
            }
        }

        String currentPhone = contactInfo != null ? contactInfo.phone() : null;
        if (command.getPhone() != null && !command.getPhone().isBlank() && !command.getPhone().equals(currentPhone)) {
            if (userRepository.findByPhone(command.getPhone()).isPresent()) {
                throw BusinessException.of(UserResultCode.PHONE_EXISTS);
            }
        }

        var personalInfo = currentUser.getPersonalInfo();
        String currentStudentId = personalInfo != null ? personalInfo.studentId() : null;
        String requestStudentId = command.getStudentId();
        if (requestStudentId != null && !requestStudentId.isBlank() && !requestStudentId.equals(currentStudentId)) {
            if (userRepository.findByStudentId(requestStudentId).isPresent()) {
                throw BusinessException.of(UserResultCode.STUDENT_ID_EXISTS);
            }
        }
    }

    private User getCurrentUserOrThrow() {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return userRepository.findById(userId)
            .orElseThrow(() -> BusinessException.of(UserResultCode.USER_NOT_FOUND));
    }
}
