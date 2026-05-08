package com.cartethyia.easyorange.user.application.service;

import com.cartethyia.easyorange.framework.service.TokenService;
import com.cartethyia.easyorange.framework.util.RequestUtil;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.LoginRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.LoginResponse;
import com.cartethyia.easyorange.user.application.assembler.UserAssembler;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.service.AuthenticationDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserLoginAppService {

    private final AuthenticationDomainService authenticationDomainService;
    private final TokenService tokenService;
    private final UserAssembler userAssembler;

    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(LoginRequest loginRequest) {
        String clientIp = RequestUtil.getClientIp();

        User user = switch (loginRequest.getEffectiveLoginMethod()) {
            case PASSWORD -> authenticationDomainService.authenticateByPassword(
                loginRequest.account(), loginRequest.password(), clientIp
            );
            case SMS -> authenticationDomainService.authenticateBySms(
                loginRequest.account(), loginRequest.password(), clientIp
            );
        };

        return buildLoginResponse(user);
    }

    public void logout(String accessToken, String refreshToken) {
        tokenService.revokeAllTokens(accessToken, refreshToken);
        SecurityContextUtil.clearContext();
    }

    public String refreshToken(String refreshToken) {
        return tokenService.refreshToken(refreshToken);
    }

    private LoginResponse buildLoginResponse(User user) {
        String userTypeCode = user.getUserType() != null ? user.getUserType().getCode() : null;
        String accessToken = tokenService.createAccessToken(user.getId(), user.getUsername(), userTypeCode);
        String newRefreshToken = tokenService.createRefreshToken(user.getId(), user.getUsername(), userTypeCode);

        return userAssembler.toLoginResponse(user, accessToken, newRefreshToken);
    }
}
