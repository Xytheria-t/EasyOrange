package com.cartethyia.easyorange.user.application.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.service.TokenService;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.ForgotPasswordRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.LoginRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.RegisterRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.LoginResponse;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.UserVO;
import com.cartethyia.easyorange.user.application.assembler.UserAssembler;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.valueobject.LoginInfo;
import com.cartethyia.easyorange.user.domain.valueobject.UserProfile;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.service.AuthenticationDomainService;
import com.cartethyia.easyorange.user.domain.service.SmsCodeDomainService;
import com.cartethyia.easyorange.user.domain.service.UserRegistrationDomainService;
import com.cartethyia.easyorange.user.domain.event.ForgotPasswordEvent;
import com.cartethyia.easyorange.user.domain.event.UserRegisteredEvent;
import com.cartethyia.easyorange.user.domain.port.output.UserEventPort;
import com.cartethyia.easyorange.user.infrastructure.util.NicknameGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthAppService 测试")
class AuthAppServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SmsCodeDomainService smsCodeDomainService;

    @Mock
    private TokenService tokenService;

    @Mock
    private UserAssembler userAssembler;

    @Mock
    private UserEventPort userEventPort;

    @Mock
    private NicknameGenerator nicknameGenerator;

    @Mock
    private AuthenticationDomainService authenticationDomainService;

    @Mock
    private UserRegistrationDomainService userRegistrationDomainService;

    private AuthAppService authAppService;

    @BeforeEach
    void setUp() {
        authAppService = new AuthAppService(
            userRepository,
            smsCodeDomainService,
            tokenService,
            userAssembler,
            userEventPort,
            nicknameGenerator,
            authenticationDomainService,
            userRegistrationDomainService
        );
    }

    @Nested
    @DisplayName("register")
    class RegisterTests {

        @Test
        @DisplayName("应通过领域服务注册用户并发布事件")
        void shouldRegisterUserAndPublishEvent() {
            RegisterRequest request = new RegisterRequest("newuser", "Password123", null, null);
            when(nicknameGenerator.generate()).thenReturn("阳光橙子");

            UserProfile profile = new UserProfile(null, null, null, "阳光橙子", null, null, null);
            User registeredUser = User.builder()
                .username("newuser")
                .password("$2a$10$encoded")
                .profile(profile)
                .userType(UserType.NORMAL)
                .status(UserStatus.NORMAL)
                .build();

            User savedUser = User.builder()
                .id(1L)
                .username("newuser")
                .password("$2a$10$encoded")
                .profile(profile)
                .userType(UserType.NORMAL)
                .status(UserStatus.NORMAL)
                .build();

            when(userRegistrationDomainService.register("newuser", "Password123", null, null, "阳光橙子"))
                .thenReturn(registeredUser);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            Long result = authAppService.register(request);

            assertThat(result).isEqualTo(1L);
            verify(userRegistrationDomainService).register("newuser", "Password123", null, null, "阳光橙子");
            verify(userRepository).save(any(User.class));

            ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
            verify(userEventPort).publish(eventCaptor.capture());
            UserRegisteredEvent event = eventCaptor.getValue();
            assertThat(event.getUserId()).isEqualTo(1L);
            assertThat(event.getUsername()).isEqualTo("newuser");
        }

        @Test
        @DisplayName("用户名已存在时应抛出异常")
        void shouldThrowWhenUsernameExists() {
            RegisterRequest request = new RegisterRequest("existinguser", "Password123", null, null);
            when(nicknameGenerator.generate()).thenReturn("阳光橙子");
            when(userRegistrationDomainService.register(anyString(), anyString(), any(), any(), anyString()))
                .thenThrow(BusinessException.of("用户名已存在"));

            assertThatThrownBy(() -> authAppService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名已存在");

            verify(userRepository, never()).save(any());
            verify(userEventPort, never()).publish(any());
        }
    }

    @Nested
    @DisplayName("login")
    class LoginTests {

        @Test
        @DisplayName("密码登录成功时应返回 LoginResponse")
        void shouldReturnLoginResponseOnPasswordLogin() {
            LoginRequest loginRequest = new LoginRequest(null, "password", "testuser", "Password123");

            User user = User.builder()
                .id(1L)
                .username("testuser")
                .password("$2a$10$encoded")
                .userType(UserType.NORMAL)
                .status(UserStatus.NORMAL)
                .loginInfo(LoginInfo.initial())
                .build();

            when(authenticationDomainService.authenticateByPassword("testuser", "Password123"))
                .thenReturn(user);
            when(tokenService.createAccessToken(any(), any(), any())).thenReturn("access-token");
            when(tokenService.createRefreshToken(any(), any(), any())).thenReturn("refresh-token");

            LoginResponse expectedResponse = LoginResponse.builder()
                .token("access-token")
                .refreshToken("refresh-token")
                .user(UserVO.builder().userId(1L).username("testuser").build())
                .build();
            when(userAssembler.toLoginResponse(any(), anyString(), anyString())).thenReturn(expectedResponse);

            LoginResponse result = authAppService.login(loginRequest);

            assertThat(result).isNotNull();
            assertThat(result.getToken()).isEqualTo("access-token");
            assertThat(result.getRefreshToken()).isEqualTo("refresh-token");

            verify(authenticationDomainService).authenticateByPassword("testuser", "Password123");
            verify(userRepository).updateLoginInfo(eq(1L), anyString());
        }

        @Test
        @DisplayName("凭证错误时应抛出异常")
        void shouldThrowOnWrongCredentials() {
            LoginRequest loginRequest = new LoginRequest(null, "password", "testuser", "WrongPassword");

            when(authenticationDomainService.authenticateByPassword("testuser", "WrongPassword"))
                .thenThrow(BusinessException.of("账号或密码错误"));

            assertThatThrownBy(() -> authAppService.login(loginRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号或密码错误");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("logout")
    class LogoutTests {

        @Test
        @DisplayName("应撤销令牌")
        void shouldRevokeTokens() {
            authAppService.logout("access-token", "refresh-token");

            verify(tokenService).revokeAllTokens("access-token", "refresh-token");
        }

        @Test
        @DisplayName("令牌为 null 时也应正常调用")
        void shouldHandleNullTokens() {
            authAppService.logout(null, null);

            verify(tokenService).revokeAllTokens(null, null);
        }
    }

    @Nested
    @DisplayName("forgotPassword")
    class ForgotPasswordTests {

        @Test
        @DisplayName("应通过领域服务重置密码并发布事件")
        void shouldResetPasswordAndPublishEvent() {
            ForgotPasswordRequest request = new ForgotPasswordRequest("13812345678", "123456", "NewPassword123");

            UserProfile profile = new UserProfile(null, "13812345678", null, null, null, null, null);
            User user = User.builder().id(1L).username("testuser").profile(profile).build();

            when(authenticationDomainService.resetPassword("13812345678", "123456", "NewPassword123", smsCodeDomainService))
                .thenReturn(user);
            when(userRepository.update(any(User.class))).thenReturn(true);

            Long result = authAppService.forgotPassword(request);

            assertThat(result).isEqualTo(1L);
            verify(authenticationDomainService).resetPassword("13812345678", "123456", "NewPassword123", smsCodeDomainService);
            verify(userRepository).update(any(User.class));

            ArgumentCaptor<ForgotPasswordEvent> eventCaptor = ArgumentCaptor.forClass(ForgotPasswordEvent.class);
            verify(userEventPort).publish(eventCaptor.capture());
            ForgotPasswordEvent event = eventCaptor.getValue();
            assertThat(event.getUserId()).isEqualTo(1L);
            assertThat(event.getPhone()).isEqualTo("13812345678");
        }

        @Test
        @DisplayName("验证码无效时应抛出异常")
        void shouldThrowWhenVerifyCodeInvalid() {
            ForgotPasswordRequest request = new ForgotPasswordRequest("13812345678", "000000", "NewPassword123");

            when(authenticationDomainService.resetPassword("13812345678", "000000", "NewPassword123", smsCodeDomainService))
                .thenThrow(BusinessException.of("验证码无效或已过期"));

            assertThatThrownBy(() -> authAppService.forgotPassword(request))
                .isInstanceOf(BusinessException.class);

            verify(userRepository, never()).save(any());
            verify(userEventPort, never()).publish(any());
        }
    }
}
