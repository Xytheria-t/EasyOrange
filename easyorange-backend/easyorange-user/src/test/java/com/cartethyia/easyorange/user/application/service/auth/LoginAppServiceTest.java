package com.cartethyia.easyorange.user.application.service.auth;

import com.cartethyia.easyorange.framework.service.TokenRefreshResult;
import com.cartethyia.easyorange.framework.service.TokenService;
import com.cartethyia.easyorange.user.domain.valueobject.LoginCommand;
import com.cartethyia.easyorange.user.application.command.LoginResult;
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
        LoginCommand command = new LoginCommand.PasswordLogin(account, password);

        User user = User.builder()
            .id(USER_ID)
            .credentials(new Credentials(USERNAME, "encoded"))
            .userType(UserType.NORMAL)
            .build();
        when(authenticationService.authenticate(any(LoginCommand.class), anyString()))
            .thenReturn(user);
        when(tokenService.createAccessToken(USER_ID, USERNAME, "01")).thenReturn("access-token");
        when(tokenService.createRefreshToken(USER_ID, USERNAME, "01")).thenReturn("refresh-token");

        UserVO userVO = UserVO.builder().userId(USER_ID).username(USERNAME).build();
        when(userAssembler.toLoginResult(user, "access-token", "refresh-token"))
            .thenReturn(new LoginResult("access-token", "refresh-token", userVO));

        LoginResult result = service.login(command);

        assertThat(result).isNotNull();
        assertThat(result.token()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.user()).isNotNull();
        assertThat(result.user().getUserId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("短信登录成功")
    void login_sms_success() {
        String phone = "13812345678";
        String verifyCode = "123456";
        LoginCommand command = new LoginCommand.SmsLogin(phone, verifyCode);

        User user = User.builder()
            .id(USER_ID)
            .credentials(new Credentials(USERNAME, "encoded"))
            .userType(UserType.NORMAL)
            .build();
        when(authenticationService.authenticate(any(LoginCommand.class), anyString()))
            .thenReturn(user);
        when(tokenService.createAccessToken(USER_ID, USERNAME, "01")).thenReturn("access-token");
        when(tokenService.createRefreshToken(USER_ID, USERNAME, "01")).thenReturn("refresh-token");

        UserVO userVO = UserVO.builder().userId(USER_ID).username(USERNAME).build();
        when(userAssembler.toLoginResult(user, "access-token", "refresh-token"))
            .thenReturn(new LoginResult("access-token", "refresh-token", userVO));

        LoginResult result = service.login(command);

        assertThat(result).isNotNull();
        assertThat(result.token()).isEqualTo("access-token");
    }

    @Test
    @DisplayName("登出成功 — 应撤销Refresh Token")
    void logout_success() {
        String refreshToken = "refresh-token";

        service.logout(refreshToken);

        verify(tokenService).invalidateToken(refreshToken);
    }

    @Test
    @DisplayName("刷新Token成功 — 应返回新的 access + refresh token")
    void refreshToken_success() {
        String oldRefreshToken = "old-refresh-token";
        TokenRefreshResult mockResult = new TokenRefreshResult("new-access-token", "new-refresh-token");
        when(tokenService.refreshToken(oldRefreshToken)).thenReturn(mockResult);

        TokenRefreshResult result = service.refreshToken(oldRefreshToken);

        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        verify(tokenService).refreshToken(oldRefreshToken);
    }
}