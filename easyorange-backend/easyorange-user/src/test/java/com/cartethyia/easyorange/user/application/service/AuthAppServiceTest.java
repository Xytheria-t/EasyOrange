package com.cartethyia.easyorange.user.application.service;

import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.auth.TokenRefreshResult;
import com.cartethyia.easyorange.framework.auth.TokenService;
import com.cartethyia.easyorange.framework.util.TestSecurityUtil;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.aggregate.UserTestFixture;
import com.cartethyia.easyorange.user.domain.port.SmsCodePort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.service.AuthenticationService;
import com.cartethyia.easyorange.user.domain.service.RegistrationService;
import com.cartethyia.easyorange.user.domain.valueobject.LoginCredential;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

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
    @Mock
    private UserRepository userRepository;
    @Mock
    private SmsCodePort smsCodePort;
    @Mock
    private DomainEventPublisher domainEventPublisher;

    private AuthAppService service;

    private static final String USER_ID = UserTestFixture.USER_ID;
    private static final String USERNAME = UserTestFixture.USERNAME;
    private static final String PHONE = UserTestFixture.PHONE;

    @BeforeEach
    void setUp() {
        service = new AuthAppService(authenticationService, registrationService, tokenService, userRepository, smsCodePort, domainEventPublisher);
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

            User savedUser = UserTestFixture.userWithCredentials(username, "encodedPassword")
                .toBuilder().id("100").personalInfo(null).build();
            when(registrationService.registerNewUser(username, password))
                .thenReturn(savedUser);
            when(userRepository.save(savedUser)).thenReturn(savedUser);

            String result = service.register(username, password);

            assertThat(result).isEqualTo("100");
            verify(registrationService).registerNewUser(username, password);
            verify(userRepository).save(savedUser);
        }
    }

    @Nested
    @DisplayName("登录")
    class Login {

        @Test
        @DisplayName("密码登录成功 — 应委托认证服务并创建Token返回LoginContext")
        void password_success() {
            String account = "testuser";
            String password = "Password123";
            LoginCredential credential = new LoginCredential.Password(account, password);

            User user = UserTestFixture.userWithCredentials(USERNAME, "encoded");
            when(authenticationService.authenticate(any(LoginCredential.class)))
                .thenReturn(user);
            when(tokenService.createAccessToken(USER_ID, USERNAME, List.of("ROLE_USER")))
                .thenReturn("access-token");
            when(tokenService.createRefreshToken(USER_ID, USERNAME, List.of("ROLE_USER")))
                .thenReturn("refresh-token");

            var result = service.login(credential);

            assertThat(result).isNotNull();
            assertThat(result.user().getId()).isEqualTo(USER_ID);
            assertThat(result.accessToken()).isEqualTo("access-token");
            assertThat(result.refreshToken()).isEqualTo("refresh-token");
            verify(authenticationService).authenticate(credential);
            verify(userRepository).update(any(User.class));
            verify(tokenService).createAccessToken(USER_ID, USERNAME, List.of("ROLE_USER"));
            verify(tokenService).createRefreshToken(USER_ID, USERNAME, List.of("ROLE_USER"));
        }

        @Test
        @DisplayName("短信登录成功 — 应委托认证服务并创建Token返回LoginContext")
        void sms_success() {
            String phone = "13812345678";
            String verifyCode = "123456";
            LoginCredential credential = new LoginCredential.Sms(phone, verifyCode);

            User user = UserTestFixture.userWithCredentials(USERNAME, "encoded");
            when(authenticationService.authenticate(any(LoginCredential.class)))
                .thenReturn(user);
            when(tokenService.createAccessToken(USER_ID, USERNAME, List.of("ROLE_USER")))
                .thenReturn("access-token");
            when(tokenService.createRefreshToken(USER_ID, USERNAME, List.of("ROLE_USER")))
                .thenReturn("refresh-token");

            var result = service.login(credential);

            assertThat(result).isNotNull();
            assertThat(result.user().getId()).isEqualTo(USER_ID);
            assertThat(result.accessToken()).isEqualTo("access-token");
            assertThat(result.refreshToken()).isEqualTo("refresh-token");
            verify(authenticationService).authenticate(credential);
            verify(userRepository).update(any(User.class));
            verify(tokenService).createAccessToken(USER_ID, USERNAME, List.of("ROLE_USER"));
            verify(tokenService).createRefreshToken(USER_ID, USERNAME, List.of("ROLE_USER"));
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
    @DisplayName("发送短信验证码")
    class SendSmsCode {

        @Test
        @DisplayName("发送成功 — 应委托 SmsCodePort")
        void success() {
            when(smsCodePort.send(PHONE)).thenReturn(true);

            service.sendSmsCode(PHONE);

            verify(smsCodePort).send(PHONE);
        }

        @Test
        @DisplayName("频率限制 — 应抛出业务异常")
        void tooFrequent() {
            when(smsCodePort.send(PHONE)).thenReturn(false);

            assertThatThrownBy(() -> service.sendSmsCode(PHONE))
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("重置密码（忘记密码）")
    class ResetPassword {

        @Test
        @DisplayName("应委托 AuthenticationService 重置并保存")
        void shouldDelegateAndSave() {
            String phone = "13812345678";
            String verifyCode = "123456";
            String newPassword = "NewPass123";

            User updated = UserTestFixture.userWithCredentials(USERNAME, "encodedNewPwd");
            when(authenticationService.resetPassword(phone, verifyCode, newPassword))
                .thenReturn(updated);

            service.resetPassword(phone, verifyCode, newPassword);

            verify(authenticationService).resetPassword(phone, verifyCode, newPassword);
            verify(userRepository).update(updated);
        }
    }

    @Nested
    @DisplayName("修改密码（已登录）")
    class ChangePassword {

        @Test
        @DisplayName("应查询用户后委托 AuthenticationService.changePassword 并保存")
        void shouldFindUserAndDelegateToChangePassword() {
            TestSecurityUtil.setSecurityContext(USER_ID);
            var user = UserTestFixture.userWithCredentials("testuser", "encodedOldPwd");
            var updatedUser = UserTestFixture.userWithCredentials("testuser", "encodedNewPwd");
            when(userRepository.findById(USER_ID)).thenReturn(java.util.Optional.of(user));
            when(authenticationService.changePassword(user, "oldPwd123", "NewPass123"))
                .thenReturn(updatedUser);

            service.changePassword("oldPwd123", "NewPass123");

            verify(authenticationService).changePassword(user, "oldPwd123", "NewPass123");
            verify(userRepository).update(updatedUser);
        }

        @Test
        @DisplayName("SecurityContext 无用户时应抛出异常")
        void shouldThrowWhenNoUserInContext() {
            assertThatThrownBy(() -> service.changePassword("123456", "NewPass123"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.UNAUTHORIZED.getCode());
        }
    }
}
