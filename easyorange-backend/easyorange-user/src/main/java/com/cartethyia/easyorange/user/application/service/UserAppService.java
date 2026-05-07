package com.cartethyia.easyorange.user.application.service;

import com.cartethyia.easyorange.common.dto.AuthUser;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.exception.FileSizeLimitExceededException;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.ChangePasswordRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.UpdateUserRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.UserProfileVO;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.UserVO;
import com.cartethyia.easyorange.user.application.assembler.UserAssembler;
import com.cartethyia.easyorange.user.domain.constant.Sex;
import com.cartethyia.easyorange.user.domain.constant.UserResultCode;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.service.PasswordDomainService;
import com.cartethyia.easyorange.user.domain.port.AvatarFilePort;
import com.cartethyia.easyorange.user.domain.event.PasswordChangedEvent;
import com.cartethyia.easyorange.user.domain.port.UserEventPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserAppService {

    private static final long AVATAR_MAX_SIZE = 5 * 1024 * 1024;

    private final UserRepository userRepository;
    private final PasswordDomainService passwordDomainService;
    private final AvatarFilePort avatarFilePort;
    private final UserAssembler userAssembler;
    private final UserEventPort userEventPort;

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

        User updatedUser = currentUser.updateProfile(request.email(), request.phone(),
            request.gender() != null ? Sex.fromCode(request.gender()) : null, request.realName(), request.nickname(), request.studentId(), currentUser.getId());
        BizRequire.requireTrue(userRepository.update(updatedUser), "更新用户信息失败");
        return userAssembler.toVo(updatedUser);
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUserOrThrow();

        passwordDomainService.validateDifferentPassword(request.oldPassword(), request.newPassword());

        BizRequire.requireTrue(
            passwordDomainService.matches(request.oldPassword(), user.getPassword()),
            UserResultCode.PASSWORD_ERROR
        );

        String encodedNewPassword = passwordDomainService.encode(request.newPassword());
        User updatedUser = user.changePassword(encodedNewPassword, user.getId());
        boolean updated = userRepository.update(updatedUser);

        BizRequire.requireTrue(updated, "修改密码失败，请稍后重试");

        userEventPort.publish(new PasswordChangedEvent(user.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public UserVO uploadAvatar(MultipartFile avatar) {
        BizRequire.notNull(avatar, "头像不能为空");
        BizRequire.requireTrue(!avatar.isEmpty(), "头像不能为空");

        if (avatar.getSize() > AVATAR_MAX_SIZE) {
            throw new FileSizeLimitExceededException(AVATAR_MAX_SIZE, avatar.getSize());
        }

        User currentUser = getCurrentUserOrThrow();

        String currentAvatar = currentUser.getProfile() != null ? currentUser.getProfile().avatar() : null;
        avatarFilePort.deleteIfExists(currentAvatar);

        try {
            byte[] content = avatar.getBytes();
            String contentType = avatar.getContentType();
            String originalFilename = avatar.getOriginalFilename();
            
            String avatarUrl = avatarFilePort.upload(content, contentType, originalFilename, currentUser.getId());

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
        return request.nickname() != null && !request.nickname().isBlank()
            || request.email() != null && !request.email().isBlank()
            || request.phone() != null && !request.phone().isBlank()
            || request.gender() != null
            || request.realName() != null && !request.realName().isBlank()
            || request.studentId() != null && !request.studentId().isBlank();
    }

    private void validateUniqueFieldsIfChanged(UpdateUserRequest request, User currentUser) {
        String currentEmail = currentUser.getProfile() != null ? currentUser.getProfile().email() : null;
        if (request.email() != null && !request.email().isBlank() && !request.email().equals(currentEmail)) {
            userRepository.findByEmail(request.email())
                .ifPresent(_ -> { throw BusinessException.of(UserResultCode.EMAIL_EXISTS); });
        }

        String currentPhone = currentUser.getProfile() != null ? currentUser.getProfile().phone() : null;
        if (request.phone() != null && !request.phone().isBlank() && !request.phone().equals(currentPhone)) {
            userRepository.findByPhone(request.phone())
                .ifPresent(_ -> { throw BusinessException.of(UserResultCode.PHONE_EXISTS); });
        }

        if (request.studentId() != null && !request.studentId().isBlank() && !request.studentId().equals(currentUser.getStudentId())) {
            userRepository.findByStudentId(request.studentId())
                .ifPresent(_ -> { throw BusinessException.of(UserResultCode.STUDENT_ID_EXISTS); });
        }
    }

    private User getCurrentUserOrThrow() {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return userRepository.findById(userId)
            .orElseThrow(() -> BusinessException.of(UserResultCode.USER_NOT_FOUND));
    }
}
