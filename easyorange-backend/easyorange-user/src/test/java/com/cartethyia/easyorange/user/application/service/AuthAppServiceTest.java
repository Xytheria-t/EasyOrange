package com.cartethyia.easyorange.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.auth.TokenRotation;
import com.cartethyia.easyorange.framework.auth.TokenService;
import com.cartethyia.easyorange.framework.util.TestSecurityUtil;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.aggregate.UserTestFixture;
import com.cartethyia.easyorange.user.domain.port.SmsCodePort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.service.AuthenticationService;
import com.cartethyia.easyorange.user.domain.service.RegistrationService;
import com.cartethyia.easyorange.user.domain.valueobject.LoginCredential;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        service = new AuthAppService(
                authenticationService,
                registrationService,
                tokenService,
                userRepository,
                smsCodePort,
                domainEventPublisher);
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

            User savedUser = UserTestFixture.userWithCredentials(username, "encodedPassword").toBuilder()
                    .id("100")
                    .personalInfo(null)
                    .build();
            when(registrationService.registerNewUser(username, password)).thenReturn(savedUser);
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
            when(authenticationService.authenticate(any(LoginCredential.class))).thenReturn(user);
            when(tokenService.createAccessToken(USER_ID, USERNAME, List.of("ROLE_USER")))
                    .thenReturn("access-token");
            when(tokenService.createRefreshToken(USER_ID)).thenReturn("refresh-token");

            var result = service.login(credential);

            assertThat(result).isNotNull();
            assertThat(result.user().getId()).isEqualTo(USER_ID);
            assertThat(result.accessToken()).isEqualTo("access-token");
            assertThat(result.refreshToken()).isEqualTo("refresh-token");
            verify(authenticationService).authenticate(credential);
            verify(userRepository).update(any(User.class));
            verify(tokenService).createAccessToken(USER_ID, USERNAME, List.of("ROLE_USER"));
            verify(tokenService).createRefreshToken(USER_ID);
        }

        @Test
        @DisplayName("短信登录成功 — 应委托认证服务并创建Token返回LoginContext")
        void sms_success() {
            String phone = "13812345678";
            String verifyCode = "123456";
            LoginCredential credential = new LoginCredential.Sms(phone, verifyCode);

            User user = UserTestFixture.userWithCredentials(USERNAME, "encoded");
            when(authenticationService.authenticate(any(LoginCredential.class))).thenReturn(user);
            when(tokenService.createAccessToken(USER_ID, USERNAME, List.of("ROLE_USER")))
                    .thenReturn("access-token");
            when(tokenService.createRefreshToken(USER_ID)).thenReturn("refresh-token");

            var result = service.login(credential);

            assertThat(result).isNotNull();
            assertThat(result.user().getId()).isEqualTo(USER_ID);
            assertThat(result.accessToken()).isEqualTo("access-token");
            assertThat(result.refreshToken()).isEqualTo("refresh-token");
            verify(authenticationService).authenticate(credential);
            verify(userRepository).update(any(User.class));
            verify(tokenService).createAccessToken(USER_ID, USERNAME, List.of("ROLE_USER"));
            verify(tokenService).createRefreshToken(USER_ID);
        }
    }

    @Nested
    @DisplayName("Token 管理")
    class TokenManagement {

        @Test
        @DisplayName("登出成功 — 吊销 access 与 refresh")
        void logout_success() {
            service.logout("access-token", "refresh-token");

            verify(tokenService).revokeAccessToken("access-token");
            verify(tokenService).revokeRefreshToken("refresh-token");
        }

        @Test
        @DisplayName("刷新Token — 轮换refresh并重验用户后签发新access")
        void refreshToken() {
            String oldRT = "old-refresh-token";
            User user = UserTestFixture.normalUser();
            when(tokenService.rotateRefreshToken(oldRT)).thenReturn(new TokenRotation(USER_ID, "new-refresh-token"));
            when(userRepository.findById(USER_ID)).thenReturn(java.util.Optional.of(user));
            when(tokenService.createAccessToken(USER_ID, USERNAME, List.of("ROLE_USER")))
                    .thenReturn("new-access-token");

            var result = service.refreshToken(oldRT);

            assertThat(result).isNotNull();
            assertThat(result.accessToken()).isEqualTo("new-access-token");
            assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
            verify(tokenService).rotateRefreshToken(oldRT);
            verify(tokenService).createAccessToken(USER_ID, USERNAME, List.of("ROLE_USER"));
            verify(tokenService, never()).revokeAllUserSessions(anyString());
        }

        @Test
        @DisplayName("刷新Token — 用户不存在时吊销全会话并抛401")
        void refreshToken_userNotFound_revokesAndThrows() {
            when(tokenService.rotateRefreshToken("rt")).thenReturn(new TokenRotation(USER_ID, "new-refresh-token"));
            when(userRepository.findById(USER_ID)).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> service.refreshToken("rt"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(ResultCode.UNAUTHORIZED.getCode());
            verify(tokenService).revokeAllUserSessions(USER_ID);
        }

        @Test
        @DisplayName("刷新Token — 用户被禁用时吊销全会话并抛401")
        void refreshToken_disabledUser_revokesAndThrows() {
            when(tokenService.rotateRefreshToken("rt")).thenReturn(new TokenRotation(USER_ID, "new-refresh-token"));
            when(userRepository.findById(USER_ID)).thenReturn(java.util.Optional.of(UserTestFixture.disabledUser()));

            assertThatThrownBy(() -> service.refreshToken("rt"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(ResultCode.UNAUTHORIZED.getCode());
            verify(tokenService).revokeAllUserSessions(USER_ID);
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

            assertThatThrownBy(() -> service.sendSmsCode(PHONE)).isInstanceOf(BusinessException.class);
        }
    }
}
