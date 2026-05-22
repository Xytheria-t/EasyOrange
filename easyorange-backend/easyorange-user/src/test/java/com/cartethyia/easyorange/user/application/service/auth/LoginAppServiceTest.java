package com.cartethyia.easyorange.user.application.service.auth;

import com.cartethyia.easyorange.framework.service.TokenService;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.LoginRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.LoginResponse;
import com.cartethyia.easyorange.user.application.assembler.UserAssembler;
import com.cartethyia.easyorange.user.application.dto.UserVO;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import com.cartethyia.easyorange.user.domain.service.AuthenticationService;
import com.cartethyia.easyorange.user.domain.valueobject.Credentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginAppService 测试")
class LoginAppServiceTest {

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private TokenService tokenService;

    @Mock
    private UserAssembler userAssembler;

    private LoginAppService service;

    private static final Long USER_ID = 1L;
    private static final String USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        service = new LoginAppService(authenticationService, tokenService, userAssembler);
    }

    @Test
    @DisplayName("密码登录成功")
    void login_password_success() {
        String account = "testuser";
        String password = "Password123";
        LoginRequest request = new LoginRequest(null, null, account, password);

        User user = User.builder()
            .id(USER_ID)
            .credentials(new Credentials(USERNAME, "encoded"))
            .userType(UserType.NORMAL)
            .build();
        when(authenticationService.authenticateByPassword(eq(account), eq(password), anyString()))
            .thenReturn(user);
        when(tokenService.createAccessToken(USER_ID, USERNAME, "01")).thenReturn("access-token");
        when(tokenService.createRefreshToken(USER_ID, USERNAME, "01")).thenReturn("refresh-token");

        UserVO userVO = UserVO.builder().userId(USER_ID).username(USERNAME).build();
        when(userAssembler.toLoginResponse(user, "access-token", "refresh-token"))
            .thenReturn(LoginResponse.builder().token("access-token").refreshToken("refresh-token").user(userVO).build());

        LoginResponse response = service.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getUserId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("短信登录成功")
    void login_sms_success() {
        String account = "13812345678";
        String verifyCode = "123456";
        LoginRequest request = new LoginRequest(null, "sms", account, verifyCode);

        User user = User.builder()
            .id(USER_ID)
            .credentials(new Credentials(USERNAME, "encoded"))
            .userType(UserType.NORMAL)
            .build();
        when(authenticationService.authenticateBySms(eq(account), eq(verifyCode), anyString()))
            .thenReturn(user);
        when(tokenService.createAccessToken(USER_ID, USERNAME, "01")).thenReturn("access-token");
        when(tokenService.createRefreshToken(USER_ID, USERNAME, "01")).thenReturn("refresh-token");

        UserVO userVO = UserVO.builder().userId(USER_ID).username(USERNAME).build();
        when(userAssembler.toLoginResponse(user, "access-token", "refresh-token"))
            .thenReturn(LoginResponse.builder().token("access-token").refreshToken("refresh-token").user(userVO).build());

        LoginResponse response = service.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("access-token");
    }

    @Test
    @DisplayName("登出成功 — 应撤销Token")
    void logout_success() {
        String accessToken = "access-token";
        String refreshToken = "refresh-token";

        service.logout(accessToken, refreshToken);

        verify(tokenService).revokeAllTokens(accessToken, refreshToken);
    }

    @Test
    @DisplayName("刷新Token成功")
    void refreshToken_success() {
        String oldRefreshToken = "old-refresh-token";
        String newAccessToken = "new-access-token";
        when(tokenService.refreshToken(oldRefreshToken)).thenReturn(newAccessToken);

        String result = service.refreshToken(oldRefreshToken);

        assertThat(result).isEqualTo("new-access-token");
        verify(tokenService).refreshToken(oldRefreshToken);
    }
}