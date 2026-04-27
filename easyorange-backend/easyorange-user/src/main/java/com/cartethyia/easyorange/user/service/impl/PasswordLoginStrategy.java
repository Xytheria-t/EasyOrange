package com.cartethyia.easyorange.user.service.impl;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.common.util.RequestUtil;
import com.cartethyia.easyorange.framework.service.TokenService;
import com.cartethyia.easyorange.user.dto.request.LoginRequest;
import com.cartethyia.easyorange.user.dto.response.LoginResponse;
import com.cartethyia.easyorange.user.dto.vo.UserVO;
import com.cartethyia.easyorange.user.entity.User;
import com.cartethyia.easyorange.user.enums.LoginMethod;
import com.cartethyia.easyorange.user.enums.UserStatus;
import com.cartethyia.easyorange.user.mapper.UserMapper;
import com.cartethyia.easyorange.user.service.LoginSecurityService;
import com.cartethyia.easyorange.user.service.UserQueryService;
import com.cartethyia.easyorange.user.service.strategy.LoginStrategy;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordLoginStrategy implements LoginStrategy {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final LoginSecurityService loginSecurityService;
    private final UserQueryService userQueryService;

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        String account = loginRequest.getAccount();
        String password = loginRequest.getPassword();

        BizRequire.notBlank(account, "账号不能为空");
        BizRequire.notBlank(password, "密码不能为空");

        loginSecurityService.checkLoginAttempts(account);

        User user = userQueryService.findUserByAccount(account);
        if (user == null) {
            loginSecurityService.recordFailedAttempt(account);
            BizRequire.fail("账号或密码错误");
        }

        if (user.getStatus() != UserStatus.NORMAL) {
            throw BusinessException.of("账号或密码错误");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            loginSecurityService.recordFailedAttempt(account);
            BizRequire.fail("账号或密码错误");
        }

        loginSecurityService.clearLoginAttempts(account);

        updateLoginInfo(user);

        String token = createToken(user);

        log.info("action=login, account={}, userId={}, result=success", 
                loginSecurityService.maskAccount(account), user.getId());

        return LoginResponse.builder()
                .token(token)
                .user(UserVO.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .build())
                .build();
    }

    private void updateLoginInfo(User user) {
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getLoginDate, LocalDateTime.now())
                .set(User::getLoginIp, RequestUtil.getClientIp()));
    }

    private String createToken(User user) {
        String userType = user.getUserType() != null ? user.getUserType().getCode() : null;
        return tokenService.createToken(user.getId(), user.getUsername(), userType);
    }

    @Override
    public LoginMethod supportedLoginMethod() {
        return LoginMethod.PASSWORD;
    }
}
