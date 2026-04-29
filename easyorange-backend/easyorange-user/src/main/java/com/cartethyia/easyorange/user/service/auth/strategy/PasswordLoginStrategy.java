package com.cartethyia.easyorange.user.service.auth.strategy;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.common.util.RequestUtil;
import com.cartethyia.easyorange.framework.service.TokenService;
import com.cartethyia.easyorange.user.dto.request.LoginRequest;
import com.cartethyia.easyorange.user.dto.response.LoginResponse;
import com.cartethyia.easyorange.user.dto.vo.UserVO;
import com.cartethyia.easyorange.user.entity.User;
import com.cartethyia.easyorange.user.enums.UserStatus;
import com.cartethyia.easyorange.user.mapper.UserMapper;
import com.cartethyia.easyorange.user.service.auth.LoginSecurityService;
import com.cartethyia.easyorange.user.service.user.UserQueryService;
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

        if (user == null || user.getStatus() != UserStatus.NORMAL || !passwordEncoder.matches(password, user.getPassword())) {
            loginSecurityService.recordFailedAttempt(account);
            BizRequire.fail("账号或密码错误");
        }

        loginSecurityService.clearLoginAttempts(account);

        updateLoginInfo(user);

        log.info("action=login, account={}, userId={}, result=success",
                loginSecurityService.maskAccount(account), user.getId());

        return LoginResponse.builder()
                .token(tokenService.createAccessToken(user.getId(), user.getUsername(),
                        user.getUserType() != null ? user.getUserType().getCode() : null))
                .user(UserVO.from(user))
                .build();
    }

    private void updateLoginInfo(User user) {
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getLoginDate, LocalDateTime.now())
                .set(User::getLoginIp, RequestUtil.getClientIp()));
    }
}