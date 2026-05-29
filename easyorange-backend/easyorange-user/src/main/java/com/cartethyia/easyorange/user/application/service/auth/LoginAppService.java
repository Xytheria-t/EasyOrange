package com.cartethyia.easyorange.user.application.service.auth;

import com.cartethyia.easyorange.framework.service.TokenRefreshResult;
import com.cartethyia.easyorange.framework.service.TokenService;
import com.cartethyia.easyorange.framework.util.RequestUtil;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.user.application.assembler.UserAssembler;
import com.cartethyia.easyorange.user.application.command.LoginResult;
import com.cartethyia.easyorange.user.domain.valueobject.LoginCommand;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginAppService {

    private final AuthenticationService authenticationService;
    private final TokenService tokenService;
    private final UserAssembler userAssembler;

    @Transactional(rollbackFor = Exception.class)
    public LoginResult login(LoginCommand command) {
        User user = authenticationService.authenticate(command, RequestUtil.getClientIp());
        return buildLoginResult(user);
    }

    public void logout(String refreshToken) {
        if (refreshToken != null) {
            tokenService.invalidateToken(refreshToken);
        }
        SecurityContextUtil.clearContext();
    }

    public TokenRefreshResult refreshToken(String refreshToken) {
        return tokenService.refreshToken(refreshToken);
    }

    private LoginResult buildLoginResult(User user) {
        String userTypeCode = user.getUserType() != null ? user.getUserType().getCode() : null;
        String accessToken = tokenService.createAccessToken(user.getId(), user.getUsername(), userTypeCode);
        String newRefreshToken = tokenService.createRefreshToken(user.getId(), user.getUsername(), userTypeCode);

        return userAssembler.toLoginResult(user, accessToken, newRefreshToken);
    }
}
