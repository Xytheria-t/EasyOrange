package com.cartethyia.easyorange.user.application.service.profile;

import com.cartethyia.easyorange.common.dto.AuthUser;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.profile.UpdateUserRequest;
import com.cartethyia.easyorange.user.application.dto.UserProfileVO;
import com.cartethyia.easyorange.user.application.dto.UserVO;
import com.cartethyia.easyorange.user.application.assembler.UserAssembler;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.Sex;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.port.output.AvatarFilePort;
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
    public UserVO updateUserInfo(UpdateUserRequest request) {
        User currentUser = getCurrentUserOrThrow();

        BizRequire.requireTrue(hasAnyUpdate(request), "没有需要更新的字段");

        validateUniqueFieldsIfChanged(request, currentUser);

        User updatedUser = currentUser;
        Long operatorId = currentUser.getId();

        boolean contactChanged = (request.email() != null && !request.email().isBlank())
            || (request.phone() != null && !request.phone().isBlank());
        if (contactChanged) {
            updatedUser = updatedUser.updateContactInfo(request.email(), request.phone(), operatorId);
        }

        boolean personalChanged = (request.realName() != null && !request.realName().isBlank())
            || (request.nickname() != null && !request.nickname().isBlank())
            || (request.gender() != null)
            || (request.studentId() != null && !request.studentId().isBlank());
        if (personalChanged) {
            updatedUser = updatedUser.updatePersonalInfo(
                request.realName(), request.nickname(),
                request.gender() != null ? Sex.fromCode(request.gender()) : null,
                request.studentId(), operatorId);
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

    private boolean hasAnyUpdate(UpdateUserRequest request) {
        String requestStudentId = request.studentId();
        return request.nickname() != null && !request.nickname().isBlank()
            || request.email() != null && !request.email().isBlank()
            || request.phone() != null && !request.phone().isBlank()
            || request.gender() != null
            || request.realName() != null && !request.realName().isBlank()
            || requestStudentId != null && !requestStudentId.isBlank();
    }

    private void validateUniqueFieldsIfChanged(UpdateUserRequest request, User currentUser) {
        var contactInfo = currentUser.getContactInfo();
        String currentEmail = contactInfo != null ? contactInfo.email() : null;
        if (request.email() != null && !request.email().isBlank() && !request.email().equals(currentEmail)) {
            userRepository.findByEmail(request.email())
                .ifPresent(_ -> { throw BusinessException.of(UserResultCode.EMAIL_EXISTS); });
        }

        String currentPhone = contactInfo != null ? contactInfo.phone() : null;
        if (request.phone() != null && !request.phone().isBlank() && !request.phone().equals(currentPhone)) {
            userRepository.findByPhone(request.phone())
                .ifPresent(_ -> { throw BusinessException.of(UserResultCode.PHONE_EXISTS); });
        }

        var personalInfo = currentUser.getPersonalInfo();
        String currentStudentId = personalInfo != null ? personalInfo.studentId() : null;
        String requestStudentId = request.studentId();
        if (requestStudentId != null && !requestStudentId.isBlank() && !requestStudentId.equals(currentStudentId)) {
            userRepository.findByStudentId(requestStudentId)
                .ifPresent(_ -> { throw BusinessException.of(UserResultCode.STUDENT_ID_EXISTS); });
        }
    }

    private User getCurrentUserOrThrow() {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return userRepository.findById(userId)
            .orElseThrow(() -> BusinessException.of(UserResultCode.USER_NOT_FOUND));
    }
}