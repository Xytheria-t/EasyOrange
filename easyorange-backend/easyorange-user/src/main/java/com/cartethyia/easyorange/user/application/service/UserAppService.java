package com.cartethyia.easyorange.user.application.service;

import com.cartethyia.easyorange.common.dto.AuthUser;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.exception.FileSizeLimitExceededException;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.user.application.assembler.UserAssembler;
import com.cartethyia.easyorange.user.common.enums.Sex;
import com.cartethyia.easyorange.user.common.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.model.User;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.service.PasswordDomainService;
import com.cartethyia.easyorange.user.dto.request.ChangePasswordRequest;
import com.cartethyia.easyorange.user.dto.request.UpdateUserRequest;
import com.cartethyia.easyorange.user.dto.vo.UserProfileVO;
import com.cartethyia.easyorange.user.dto.vo.UserVO;
import com.cartethyia.easyorange.user.infrastructure.event.UserEventPublisher;
import com.cartethyia.easyorange.user.infrastructure.storage.FileStorageAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAppService {

    private static final long AVATAR_MAX_SIZE = 5 * 1024 * 1024;

    private final UserRepository userRepository;
    private final PasswordDomainService passwordDomainService;
    private final FileStorageAdapter fileStorageAdapter;
    private final UserAssembler userAssembler;
    private final UserEventPublisher userEventPublisher;

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

        currentUser.updateInfo(request.getEmail(), request.getPhone(),
            Sex.fromCode(request.getGender()));
        BizRequire.requireTrue(userRepository.update(currentUser), "更新用户信息失败");
        log.info("action=updateUserInfo success userId={}", currentUser.getId());
        return userAssembler.toVo(currentUser);
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUserOrThrow();

        passwordDomainService.validateDifferentPassword(request.getOldPassword(), request.getNewPassword());

        BizRequire.requireTrue(
            passwordDomainService.matches(request.getOldPassword(), user.getPassword()),
            UserResultCode.PASSWORD_ERROR
        );

        String encodedNewPassword = passwordDomainService.encode(request.getNewPassword());
        boolean updated = userRepository.updatePassword(user.getId(), encodedNewPassword);

        BizRequire.requireTrue(updated, "修改密码失败，请稍后重试");

        userEventPublisher.publishPasswordChanged(user.getId());

        log.info("action=changePassword success userId={}", user.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public UserVO uploadAvatar(MultipartFile avatar) {
        BizRequire.notNull(avatar, "头像不能为空");
        BizRequire.requireTrue(!avatar.isEmpty(), "头像不能为空");

        if (avatar.getSize() > AVATAR_MAX_SIZE) {
            throw new FileSizeLimitExceededException(AVATAR_MAX_SIZE, avatar.getSize());
        }

        User currentUser = getCurrentUserOrThrow();

        fileStorageAdapter.deleteIfExists(currentUser.getAvatar());

        String avatarUrl = fileStorageAdapter.uploadAvatar(avatar, currentUser.getId());

        currentUser.changeAvatar(avatarUrl);
        BizRequire.requireTrue(userRepository.update(currentUser), "更新头像失败");

        log.info("action=uploadAvatar success userId={}, avatarUrl={}", currentUser.getId(), avatarUrl);

        return userRepository.findById(currentUser.getId())
            .map(userAssembler::toVo)
            .orElseThrow(() -> BusinessException.of(UserResultCode.USER_NOT_FOUND));
    }

    private boolean hasAnyUpdate(UpdateUserRequest request) {
        return request.getEmail() != null && !request.getEmail().isBlank()
            || request.getPhone() != null && !request.getPhone().isBlank()
            || request.getGender() != null;
    }

    private User getCurrentUserOrThrow() {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return userRepository.findById(userId)
            .orElseThrow(() -> BusinessException.of(UserResultCode.USER_NOT_FOUND));
    }
}
