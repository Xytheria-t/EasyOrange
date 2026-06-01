package com.cartethyia.easyorange.user.application.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.service.TokenRefreshResult;
import com.cartethyia.easyorange.framework.service.TokenService;
import com.cartethyia.easyorange.framework.util.TestSecurityUtil;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.service.AuthenticationService;
import com.cartethyia.easyorange.user.domain.service.PasswordManagementService;
import com.cartethyia.easyorange.user.domain.service.RegistrationService;
import com.cartethyia.easyorange.user.domain.valueobject.ContactInfo;
import com.cartethyia.easyorange.user.domain.valueobject.Credentials;
import com.cartethyia.easyorange.user.domain.valueobject.ImmutablePersonalInfo;
import com.cartethyia.easyorange.user.domain.valueobject.LoginCredential;
import com.cartethyia.easyorange.user.domain.valueobject.LoginInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    private PasswordManagementService passwordManagementService;
    @Mock
    private TokenService tokenService;
    @Mock
    private UserRepository userRepository;

    private AuthAppService service;

    private static final Long USER_ID = 1L;
    private static final String USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        service = new AuthAppService(authenticationService, registrationService,
            passwordManagementService, tokenService, userRepository);
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
                .personalInfo(ImmutablePersonalInfo.builder().nickName(username).build())
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
        @DisplayName("应委托 PasswordManagementService 重置")
        void shouldDelegate() {
            String phone = "13812345678";
            String verifyCode = "123456";
            String newPassword = "NewPass123";

            service.resetPassword(phone, verifyCode, newPassword);

            verify(passwordManagementService).resetPassword(phone, verifyCode, newPassword);
        }
    }

    @Nested
    @DisplayName("修改密码（已登录）")
    class ChangePassword {

        private User buildTestUser() {
            ContactInfo contactInfo = new ContactInfo("test@example.com", "13812345678");
            return User.builder()
                .id(USER_ID)
                .credentials(new Credentials("testuser", "$2a$10$encoded"))
                .userType(UserType.NORMAL)
                .status(UserStatus.NORMAL)
                .contactInfo(contactInfo)
                .personalInfo(ImmutablePersonalInfo.builder().nickName("testuser").build())
                .loginInfo(LoginInfo.empty())
                .build();
        }

        @Test
        @DisplayName("应读取当前用户手机号并委托 PasswordManagementService")
        void shouldDelegateWithPhoneFromCurrentUser() {
            TestSecurityUtil.setSecurityContext(USER_ID);
            User user = buildTestUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            service.changePassword("123456", "NewPass123");

            verify(passwordManagementService).resetPassword("13812345678", "123456", "NewPass123");
        }

        @Test
        @DisplayName("用户不存在时应抛出异常")
        void shouldThrowWhenUserNotFound() {
            TestSecurityUtil.setSecurityContext(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.changePassword("123456", "NewPass123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在");

            verify(passwordManagementService, never()).resetPassword(any(), any(), any());
        }
    }
}
