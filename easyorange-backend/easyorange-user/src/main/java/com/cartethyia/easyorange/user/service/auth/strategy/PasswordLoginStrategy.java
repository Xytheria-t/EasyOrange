package com.cartethyia.easyorange.user.service.auth.strategy;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.service.TokenService;
import com.cartethyia.easyorange.user.dto.request.LoginRequest;
import com.cartethyia.easyorange.user.dto.response.LoginResponse;
import com.cartethyia.easyorange.user.dto.vo.UserVO;
import com.cartethyia.easyorange.user.entity.User;
import com.cartethyia.easyorange.user.enums.UserStatus;
import com.cartethyia.easyorange.user.service.auth.LoginSecurityService;
import com.cartethyia.easyorange.user.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordLoginStrategy implements LoginStrategy {

    private final BCryptPasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final LoginSecurityService loginSecurityService;
    private final UserService userService;

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        String account = loginRequest.getAccount();
        String password = loginRequest.getPassword();

        BizRequire.notBlank(account, "账号不能为空");
        BizRequire.notBlank(password, "密码不能为空");

        loginSecurityService.checkLoginAttempts(account);

        User user = userService.findUserByAccount(account);

        if (user == null || user.getStatus() != UserStatus.NORMAL || !passwordEncoder.matches(password, user.getPassword())) {
            loginSecurityService.recordFailedAttempt(account);
            BizRequire.fail("账号或密码错误");
        }

        loginSecurityService.clearLoginAttempts(account);

        log.info("action=login, account={}, userId={}, result=success",
                loginSecurityService.maskAccount(account), user.getId());

        String accessToken = tokenService.createAccessToken(user.getId(), user.getUsername(),
                user.getUserType() != null ? user.getUserType().getCode() : null);
        String refreshToken = tokenService.createRefreshToken(user.getId(), user.getUsername(),
                user.getUserType() != null ? user.getUserType().getCode() : null);

        return LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .user(UserVO.from(user))
                .build();
    }
}