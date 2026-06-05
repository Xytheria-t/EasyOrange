package com.cartethyia.easyorange.user.application.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.auth.TokenRefreshResult;
import com.cartethyia.easyorange.framework.auth.TokenService;
import com.cartethyia.easyorange.framework.util.TestSecurityUtil;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import com.cartethyia.easyorange.user.domain.service.AuthenticationService;
import com.cartethyia.easyorange.user.domain.service.RegistrationService;
import com.cartethyia.easyorange.user.domain.valueobject.Credentials;
import com.cartethyia.easyorange.user.domain.valueobject.LoginCredential;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthAppService 测试")
class AuthAppServiceTest {

    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private RegistrationService registrationService;
    @Mock
    private TokenService tokenService;

    private AuthAppService service;

    private static final Long USER_ID = 1L;
    private static final String USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        service = new AuthAppService(authenticationService, registrationService, tokenService);
    }

    @AfterEach
    void tearDown() {
        TestSecurityUtil.clearSecurityContext();
    }

    @Nested
    @DisplayName("注册")
    class Register {

        @Test
        @DisplayName("成功 — 应委托领域服务注册并返回ID")
        void success() {
            String username = "newuser";
            String password = "Password123";

            User savedUser = User.builder()
                .id(100L)
                .credentials(new Credentials(username, "encodedPassword"))
                .personalInfo(null)
                .build();
            when(registrationService.registerNewUser(username, password))
                .thenReturn(savedUser);

            Long result = service.register(username, password);

            assertThat(result).isEqualTo(100L);
            verify(registrationService).registerNewUser(username, password);
        }
    }

    @Nested
    @DisplayName("登录")
    class Login {

        @Test
        @DisplayName("密码登录成功 — 应委托认证服务并返回领域用户")
        void password_success() {
            String account = "testuser";
            String password = "Password123";
            LoginCredential credential = new LoginCredential.Password(account, password);

            User user = User.builder()
                .id(USER_ID)
                .credentials(new Credentials(USERNAME, "encoded"))
                .userType(UserType.NORMAL)
                .build();
            when(authenticationService.authenticate(any(LoginCredential.class), anyString()))
                .thenReturn(user);

            User result = service.login(credential);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("短信登录成功")
        void sms_success() {
            String phone = "13812345678";
            String verifyCode = "123456";
            LoginCredential credential = new LoginCredential.Sms(phone, verifyCode);

            User user = User.builder()
                .id(USER_ID)
                .credentials(new Credentials(USERNAME, "encoded"))
                .userType(UserType.NORMAL)
                .build();
            when(authenticationService.authenticate(any(LoginCredential.class), anyString()))
                .thenReturn(user);

            User result = service.login(credential);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(USER_ID);
        }
    }

    @Nested
    @DisplayName("Token 管理")
    class TokenManagement {

        @Test
        @DisplayName("登出成功 — 应撤销Refresh Token")
        void logout_success() {
            String refreshToken = "refresh-token";

            service.logout(refreshToken);

            verify(tokenService).invalidateToken(refreshToken);
        }

        @Test
        @DisplayName("刷新Token — 应委托 tokenService 并返回结果")
        void refreshToken() {
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

    @Nested
    @DisplayName("重置密码（忘记密码）")
    class ResetPassword {

        @Test
        @DisplayName("应委托 AuthenticationService 重置")
        void shouldDelegate() {
            String phone = "13812345678";
            String verifyCode = "123456";
            String newPassword = "NewPass123";

            service.resetPassword(phone, verifyCode, newPassword);

            verify(authenticationService).resetPassword(phone, verifyCode, newPassword);
        }
    }

    @Nested
    @DisplayName("修改密码（已登录）")
    class ChangePassword {

        @Test
        @DisplayName("应提取userId并委托 AuthenticationService")
        void shouldDelegateWithUserId() {
            TestSecurityUtil.setSecurityContext(USER_ID);

            service.changePassword("123456", "NewPass123");

            verify(authenticationService).resetPassword(USER_ID, "123456", "NewPass123");
        }

        @Test
        @DisplayName("SecurityContext 无用户时应抛出异常")
        void shouldThrowWhenNoUserInContext() {
            assertThatThrownBy(() -> service.changePassword("123456", "NewPass123"))
                .isInstanceOf(Exception.class);
        }
    }
}
