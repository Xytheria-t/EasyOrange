package com.cartethyia.easyorange.user.service.impl;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.common.util.RequestUtil;
import com.cartethyia.easyorange.framework.service.TokenService;
import com.cartethyia.easyorange.user.dto.request.LoginDTO;
import com.cartethyia.easyorange.user.dto.response.LoginResponse;
import com.cartethyia.easyorange.user.entity.User;
import com.cartethyia.easyorange.user.enums.ClientType;
import com.cartethyia.easyorange.user.enums.UserStatus;
import com.cartethyia.easyorange.user.mapper.UserMapper;
import com.cartethyia.easyorange.user.service.LoginSecurityService;
import com.cartethyia.easyorange.user.service.LoginStrategyService;
import com.cartethyia.easyorange.user.service.UserQueryService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebLoginServiceImpl implements LoginStrategyService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final LoginSecurityService loginSecurityService;
    private final UserQueryService userQueryService;

    @Override
    public LoginResponse login(LoginDTO loginDTO) {
        String account = loginDTO.getAccount();
        String password = loginDTO.getPassword();

        BizRequire.notBlank(account, "账号不能为空");
        BizRequire.notBlank(password, "密码不能为空");

        loginSecurityService.checkLoginAttempts(account);

        User user = userQueryService.findUserByAccount(account);
        BizRequire.notNull(user, "用户不存在");

        validateUserStatus(user);

        verifyPassword(password, user, account);

        loginSecurityService.clearLoginAttempts(account);

        updateLoginInfo(user);

        String token = generateToken(user);

        log.info("action=login, account={}, userId={}, result=success", loginSecurityService.maskAccount(account), user.getId());

        return buildLoginResponse(user, token);
    }

    private void validateUserStatus(User user) {
        String status = user.getStatus();
        if (UserStatus.DISABLED.getCode().equals(status)) {
            throw BusinessException.of("账号已被禁用");
        }
        if (UserStatus.LOCKED.getCode().equals(status)) {
            throw BusinessException.of("账号已被锁定");
        }
    }

    private void verifyPassword(String password, User user, String account) {
        if (!passwordEncoder.matches(password, user.getPassword())) {
            loginSecurityService.recordFailedAttempt(account);
            BizRequire.fail("密码错误");
        }
    }

    private void updateLoginInfo(User user) {
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getLoginDate, LocalDateTime.now())
                .set(User::getLoginIp, RequestUtil.getClientIp()));
    }

    private String generateToken(User user) {
        return tokenService.createToken(user.getId(), user.getUsername());
    }

    private LoginResponse buildLoginResponse(User user, String token) {
        return LoginResponse.builder()
                .token(token)
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .build())
                .build();
    }

    @Override
    public String[] supportedClientTypes() {
        return new String[]{ClientType.WEB.getValue()};
    }
}
