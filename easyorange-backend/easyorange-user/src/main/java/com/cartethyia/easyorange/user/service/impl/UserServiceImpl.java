package com.cartethyia.easyorange.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.common.util.FileUtils;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.common.dto.AuthUser;
import com.cartethyia.easyorange.user.converter.UserConverter;
import com.cartethyia.easyorange.user.dto.bo.*;
import com.cartethyia.easyorange.user.dto.vo.UserProfileVO;
import com.cartethyia.easyorange.user.dto.vo.UserVO;
import com.cartethyia.easyorange.user.entity.User;
import com.cartethyia.easyorange.user.event.annotation.PublishEvent;
import com.cartethyia.easyorange.user.event.extractor.PasswordChangedEventExtractor;
import com.cartethyia.easyorange.user.event.extractor.UserRegisteredEventExtractor;
import com.cartethyia.easyorange.user.mapper.UserMapper;
import com.cartethyia.easyorange.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserConverter userConverter;
    private final UserRegisteredEventExtractor userRegisteredEventExtractor;
    private final PasswordChangedEventExtractor passwordChangedEventExtractor;

    @Value("${user.avatar.path:./upload/avatar}")
    private String avatarUploadPath;

    @Override
    public UserProfileVO getUserInfo() {
        AuthUser authUser = SecurityContextUtil.getUserContextOrThrow();
        User user = getById(authUser.userId());
        BizRequire.notNull(user, "用户不存在");
        return UserProfileVO.from(user, authUser.roles(), authUser.permissions(), authUser.loginTime());
    }

    @Override
    @PublishEvent(type = "UserRegistered", extractor = "userRegisteredEventExtractor")
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterBo bo) {
        User existingUser = lambdaQuery().eq(User::getUsername, bo.username()).one();
        BizRequire.isNull(existingUser, "用户名已存在");

        User user = bo.toEntity(passwordEncoder);
        BizRequire.isTrue(save(user), "注册失败，请稍后重试");

        userRegisteredEventExtractor.setUser(user);
        log.info("action=register success username={}", bo.username());
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forgotPassword(ForgotPasswordBo bo) {
        User user = lambdaQuery().eq(User::getPhone, bo.phone()).one();
        BizRequire.notNull(user, "该手机号未注册");

        boolean updated = lambdaUpdate()
            .eq(User::getId, user.getId())
            .set(User::getPassword, bo.encodePassword(passwordEncoder))
            .set(User::getPwdUpdateDate, bo.getPasswordUpdateTime())
            .update();

        BizRequire.isTrue(updated, "重置密码失败，请稍后重试");
        log.info("action=forgotPassword success phone={}", bo.phone());
    }

    @Override
    @PublishEvent(type = "PasswordChanged", extractor = "passwordChangedEventExtractor")
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordBo bo) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        User user = getById(userId);
        BizRequire.notNull(user, "用户不存在");

        bo.validateDifferentPassword();

        BizRequire.isTrue(
            bo.verifyOldPassword(passwordEncoder, user.getPassword()),
            "旧密码错误"
        );

        boolean updated = lambdaUpdate()
            .eq(User::getId, userId)
            .set(User::getPassword, bo.encodeNewPassword(passwordEncoder))
            .set(User::getPwdUpdateDate, bo.getPasswordUpdateTime())
            .update();

        BizRequire.isTrue(updated, "修改密码失败，请稍后重试");
        passwordChangedEventExtractor.setUserId(userId);
        log.info("action=changePassword success userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateUserInfo(UpdateUserBo bo) {
        User currentUser = getCurrentUserOrThrow();

        BizRequire.isTrue(bo.hasAnyUpdate(), "没有需要更新的字段");

        bo.applyTo(currentUser);
        BizRequire.isTrue(updateById(currentUser), "更新用户信息失败");

        log.info("action=updateUserInfo success userId={}", currentUser.getId());
        return userConverter.toVo(currentUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO uploadAvatar(UploadAvatarBo bo) {
        MultipartFile avatar = bo.avatar();
        BizRequire.notNull(avatar, "头像不能为空");
        BizRequire.isTrue(!avatar.isEmpty(), "头像不能为空");

        User currentUser = getCurrentUserOrThrow();

        try {
            String[] allowedExtensions = new String[]{"jpg", "jpeg", "png", "webp"};
            String avatarPath = FileUtils.upload(avatarUploadPath, avatar, allowedExtensions);
            String avatarUrl = "/api/file/" + avatarPath.replace("\\", "/");

            boolean updated = lambdaUpdate()
                .eq(User::getId, currentUser.getId())
                .set(User::getAvatar, avatarUrl)
                .update();

            BizRequire.isTrue(updated, "更新头像失败");
            log.info("action=uploadAvatar success userId={}, avatarUrl={}", currentUser.getId(), avatarUrl);

            return userConverter.toVo(getById(currentUser.getId()));
        } catch (IOException e) {
            log.error("上传头像失败：userId={}, error={}", currentUser.getId(), e.getMessage());
            throw BusinessException.of("上传头像失败：" + e.getMessage(), e);
        }
    }

    private User getCurrentUserOrThrow() {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        User user = getById(userId);
        BizRequire.notNull(user, "用户不存在");
        return user;
    }
}
