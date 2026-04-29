package com.cartethyia.easyorange.user.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.common.util.FileUtils;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.common.dto.AuthUser;
import com.cartethyia.easyorange.user.converter.UserConverter;
import com.cartethyia.easyorange.user.constant.UserConstant;
import com.cartethyia.easyorange.user.dto.bo.*;
import com.cartethyia.easyorange.user.dto.vo.UserProfileVO;
import com.cartethyia.easyorange.user.dto.vo.UserVO;
import com.cartethyia.easyorange.user.entity.User;
import com.cartethyia.easyorange.user.event.annotation.PublishEvent;
import com.cartethyia.easyorange.user.mapper.UserMapper;
import com.cartethyia.easyorange.user.service.user.UserService;
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
    @PublishEvent(type = "PasswordChanged", extractor = "passwordChangedEventExtractor")
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordBo bo) {
        User user = getCurrentUserOrThrow();

        bo.validateDifferentPassword();

        BizRequire.requireTrue(
            bo.verifyOldPassword(passwordEncoder, user.getPassword()),
            "旧密码错误"
        );

        boolean updated = lambdaUpdate()
            .eq(User::getId, user.getId())
            .set(User::getPassword, bo.encodeNewPassword(passwordEncoder))
            .set(User::getPwdUpdateDate, bo.getPasswordUpdateTime())
            .update();

        BizRequire.requireTrue(updated, "修改密码失败，请稍后重试");
        log.info("action=changePassword success userId={}", user.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateUserInfo(UpdateUserBo bo) {
        User currentUser = getCurrentUserOrThrow();

        BizRequire.requireTrue(bo.hasAnyUpdate(), "没有需要更新的字段");

        bo.applyTo(currentUser);
        BizRequire.requireTrue(updateById(currentUser), "更新用户信息失败");

        log.info("action=updateUserInfo success userId={}", currentUser.getId());
        return userConverter.toVo(currentUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO uploadAvatar(UploadAvatarBo bo) {
        MultipartFile avatar = bo.avatar();
        BizRequire.notNull(avatar, "头像不能为空");
        BizRequire.requireTrue(!avatar.isEmpty(), "头像不能为空");

        User currentUser = getCurrentUserOrThrow();

        try {
            String[] allowedExtensions = new String[]{"jpg", "jpeg", "png", "webp"};
            String avatarPath = FileUtils.upload(avatarUploadPath, avatar, allowedExtensions);
            String avatarUrl = "/api/file/" + avatarPath.replace("\\", "/");

            boolean updated = lambdaUpdate()
                .eq(User::getId, currentUser.getId())
                .set(User::getAvatar, avatarUrl)
                .update();

            BizRequire.requireTrue(updated, "更新头像失败");
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

    @Override
    public User findUserByAccount(String account) {
        if (account == null || account.isBlank()) {
            return null;
        }

        boolean isEmail = UserConstant.EMAIL_PATTERN.matcher(account).matches();
        boolean isPhone = UserConstant.PHONE_PATTERN.matcher(account).matches();

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if (isEmail) {
            wrapper.eq(User::getEmail, account).or().eq(User::getPhone, account);
        } else if (isPhone) {
            wrapper.eq(User::getPhone, account);
        }

        wrapper.or().eq(User::getUsername, account);

        return getOne(wrapper);
    }
}