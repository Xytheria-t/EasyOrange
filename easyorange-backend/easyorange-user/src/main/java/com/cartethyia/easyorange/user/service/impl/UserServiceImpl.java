package com.cartethyia.easyorange.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.common.util.SecurityContextUtil;
import org.apache.commons.lang3.StringUtils;
import com.cartethyia.easyorange.user.dto.request.*;
import com.cartethyia.easyorange.user.dto.vo.UserVO;
import com.cartethyia.easyorange.user.entity.User;
import com.cartethyia.easyorange.user.enums.AccountType;
import com.cartethyia.easyorange.user.enums.UserStatus;
import com.cartethyia.easyorange.user.enums.UserType;
import com.cartethyia.easyorange.user.event.annotation.PublishEvent;
import com.cartethyia.easyorange.user.event.extractor.PasswordChangedEventExtractor;
import com.cartethyia.easyorange.user.event.extractor.UserRegisteredEventExtractor;
import com.cartethyia.easyorange.user.mapper.UserMapper;
import com.cartethyia.easyorange.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final BCryptPasswordEncoder passwordEncoder;
    private final UserRegisteredEventExtractor userRegisteredEventExtractor;
    private final PasswordChangedEventExtractor passwordChangedEventExtractor;

    @Override
    public UserVO getUserInfo() {
        return convertToUserVO(getUserOrThrow());
    }

    @Override
    @PublishEvent(type = "UserRegistered", extractor = "userRegisteredEventExtractor")
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterRequest request) {
        String username = request.getUsername();
        BizRequire.isFalse(lambdaQuery().eq(User::getUsername, username).exists(), "用户名已存在");

        User user = User.builder()
            .username(username)
            .password(passwordEncoder.encode(request.getPassword()))
            .loginType(AccountType.WEB.getCode())
            .userType(UserType.NORMAL.getCode())
            .status(UserStatus.NORMAL.getCode())
            .build();

        BizRequire.isTrue(save(user), "注册失败，请稍后重试");
        
        userRegisteredEventExtractor.setUser(user);
        
        log.info("action=register success username={}", username);
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateUserInfo(UpdateUserRequest request) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        boolean updated = lambdaUpdate()
            .eq(User::getId, userId)
            .set(StringUtils.isNotBlank(request.getEmail()), User::getEmail, request.getEmail())
            .set(StringUtils.isNotBlank(request.getPhone()), User::getPhone, request.getPhone())
            .set(request.getGender() != null, User::getSex, String.valueOf(request.getGender()))
            .update();

        BizRequire.isTrue(updated, "更新用户信息失败");

        User updatedUser = getById(userId);
        BizRequire.notNull(updatedUser, "用户不存在");
        log.info("action=updateUserInfo success userId={}", userId);
        return convertToUserVO(updatedUser);
    }

    @Override
    @PublishEvent(type = "PasswordChanged", extractor = "passwordChangedEventExtractor")
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordRequest request) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        User user = getById(userId);
        BizRequire.notNull(user, "用户不存在");

        BizRequire.isTrue(passwordEncoder.matches(request.getOldPassword(), user.getPassword()), "旧密码错误");

        boolean updated = lambdaUpdate()
            .eq(User::getId, userId)
            .set(User::getPassword, passwordEncoder.encode(request.getNewPassword()))
            .set(User::getPwdUpdateDate, LocalDateTime.now())
            .update();

        BizRequire.isTrue(updated, "修改密码失败，请稍后重试");
        
        passwordChangedEventExtractor.setUserId(userId);
        
        log.info("action=changePassword success userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = lambdaQuery().eq(User::getPhone, request.getPhone()).one();
        BizRequire.notNull(user, "该手机号未注册");

        boolean updated = lambdaUpdate()
            .eq(User::getId, user.getId())
            .set(User::getPassword, passwordEncoder.encode(request.getNewPassword()))
            .set(User::getPwdUpdateDate, LocalDateTime.now())
            .update();

        BizRequire.isTrue(updated, "重置密码失败，请稍后重试");
        log.info("action=forgotPassword success phone={}", request.getPhone());
    }

    private User getUserOrThrow() {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        User user = getById(userId);
        BizRequire.notNull(user, "用户不存在");
        return user;
    }

    private UserVO convertToUserVO(User user) {
        return UserVO.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .phone(user.getPhone())
            .studentId(user.getStudentId())
            .realName(user.getRealName())
            .status(parseStatus(user.getStatus()))
            .createTime(user.getCreateTime())
            .updateTime(user.getUpdateTime())
            .build();
    }

    private Integer parseStatus(String status) {
        if (StringUtils.isBlank(status)) {
            return 0;
        }
        try {
            return Integer.parseInt(status);
        } catch (NumberFormatException e) {
            log.warn("Invalid user status value: {}", status);
            return 0;
        }
    }
}
