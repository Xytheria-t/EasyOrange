package com.cartethyia.easyorange.user.service.auth.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.service.TokenService;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.user.dto.bo.ForgotPasswordBo;
import com.cartethyia.easyorange.user.dto.bo.RegisterBo;
import com.cartethyia.easyorange.user.dto.request.LoginRequest;
import com.cartethyia.easyorange.user.dto.response.LoginResponse;
import com.cartethyia.easyorange.user.entity.User;
import com.cartethyia.easyorange.user.event.annotation.PublishEvent;
import com.cartethyia.easyorange.user.mapper.UserMapper;
import com.cartethyia.easyorange.user.service.auth.AuthService;
import com.cartethyia.easyorange.user.service.auth.strategy.LoginDispatcher;
import com.cartethyia.easyorange.user.service.user.UserActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final LoginDispatcher loginDispatcher;
    private final TokenService tokenService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserActivityService userActivityService;

    @Override
    @PublishEvent(type = "UserRegistered", extractor = "userRegisteredEventExtractor")
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterBo bo) {
        User existingUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, bo.username()));
        BizRequire.isNull(existingUser, "用户名已存在");

        User user = bo.toEntity(passwordEncoder);
        BizRequire.requireTrue(userMapper.insert(user) > 0, "注册失败，请稍后重试");

        return user.getId();
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        LoginResponse response = loginDispatcher.login(loginRequest);
        userActivityService.recordLogin(response.getUser().getId());
        return response;
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        tokenService.revokeAllTokens(accessToken, refreshToken);
        SecurityContextUtil.clearContext();
    }

    @Override
    public String refreshToken(String refreshToken) {
        String newToken = tokenService.refreshToken(refreshToken);
        BizRequire.notNull(newToken, ResultCode.UNAUTHORIZED);
        return newToken;
    }

    @Override
    @PublishEvent(type = "PasswordChanged", extractor = "forgotPasswordEventExtractor")
    @Transactional(rollbackFor = Exception.class)
    public Long forgotPassword(ForgotPasswordBo bo) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, bo.phone()));
        BizRequire.notNull(user, "该手机号未注册");

        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<User>()
            .eq(User::getId, user.getId())
            .set(User::getPassword, bo.encodePassword(passwordEncoder))
            .set(User::getPwdUpdateDate, bo.getPasswordUpdateTime());

        boolean updated = userMapper.update(null, updateWrapper) > 0;

        BizRequire.requireTrue(updated, "重置密码失败，请稍后重试");
        return user.getId();
    }
}