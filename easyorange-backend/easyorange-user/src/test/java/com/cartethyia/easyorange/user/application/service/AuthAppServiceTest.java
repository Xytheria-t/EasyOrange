package com.cartethyia.easyorange.user.application.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.service.TokenService;
import com.cartethyia.easyorange.user.application.assembler.UserAssembler;
import com.cartethyia.easyorange.user.domain.model.User;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.service.LoginSecurityDomainService;
import com.cartethyia.easyorange.user.domain.service.PasswordDomainService;
import com.cartethyia.easyorange.user.domain.service.SmsCodeService;
import com.cartethyia.easyorange.user.dto.request.ForgotPasswordRequest;
import com.cartethyia.easyorange.user.dto.request.LoginRequest;
import com.cartethyia.easyorange.user.dto.request.RegisterRequest;
import com.cartethyia.easyorange.user.dto.response.LoginResponse;
import com.cartethyia.easyorange.user.dto.vo.UserVO;
import com.cartethyia.easyorange.user.common.enums.UserStatus;
import com.cartethyia.easyorange.user.common.enums.UserType;
import com.cartethyia.easyorange.user.infrastructure.event.UserEventPublisher;
import com.cartethyia.easyorange.user.util.NicknameGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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
    private UserRepository userRepository;

    @Mock
    private PasswordDomainService passwordDomainService;

    @Mock
    private LoginSecurityDomainService loginSecurityDomainService;

    @Mock
    private SmsCodeService smsCodeService;

    @Mock
    private TokenService tokenService;

    @Mock
    private UserAssembler userAssembler;

    @Mock
    private UserEventPublisher userEventPublisher;

    @Mock
    private NicknameGenerator nicknameGenerator;

    private AuthAppService authAppService;

    @BeforeEach
    void setUp() {
        authAppService = new AuthAppService(
            userRepository,
            passwordDomainService,
            loginSecurityDomainService,
            smsCodeService,
            tokenService,
            userAssembler,
            userEventPublisher,
            nicknameGenerator
        );
    }

    @Nested
    @DisplayName("register")
    class RegisterTests {

        @Test
        @DisplayName("应保存用户并发布注册事件")
        void shouldSaveUserAndPublishEvent() {
            RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .password("Password123")
                .build();
            when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
            when(passwordDomainService.encode("Password123")).thenReturn("$2a$10$encoded");
            when(nicknameGenerator.generate()).thenReturn("阳光橙子");

            User savedUser = User.builder()
                .id(1L)
                .username("newuser")
                .password("$2a$10$encoded")
                .nickName("阳光橙子")
                .userType(UserType.NORMAL)
                .status(UserStatus.NORMAL)
                .build();
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            Long result = authAppService.register(request);

            assertThat(result).isEqualTo(1L);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User capturedUser = userCaptor.getValue();
            assertThat(capturedUser.getUsername()).isEqualTo("newuser");
            assertThat(capturedUser.getPassword()).isEqualTo("$2a$10$encoded");
            assertThat(capturedUser.getNickName()).isEqualTo("阳光橙子");
            assertThat(capturedUser.getUserType()).isEqualTo(UserType.NORMAL);
            assertThat(capturedUser.getStatus()).isEqualTo(UserStatus.NORMAL);

            verify(userEventPublisher).publishUserRegistered(1L, "newuser");
        }

        @Test
        @DisplayName("用户名已存在时应抛出异常")
        void shouldThrowWhenUsernameExists() {
            RegisterRequest request = RegisterRequest.builder()
                .username("existinguser")
                .password("Password123")
                .build();
            User existingUser = User.builder().id(1L).username("existinguser").build();
            when(userRepository.findByUsername("existinguser")).thenReturn(Optional.of(existingUser));

            assertThatThrownBy(() -> authAppService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名已存在");

            verify(userRepository, never()).save(any());
            verify(userEventPublisher, never()).publishUserRegistered(any(), any());
        }
    }

    @Nested
    @DisplayName("login")
    class LoginTests {

        @Test
        @DisplayName("密码登录成功时应返回 LoginResponse")
        void shouldReturnLoginResponseOnPasswordLogin() {
            String rawPassword = "Password123";
            String encodedPassword = new BCryptPasswordEncoder().encode(rawPassword);

            LoginRequest loginRequest = LoginRequest.builder()
                .account("testuser")
                .password(rawPassword)
                .loginMethod("password")
                .build();

            User user = User.builder()
                .id(1L)
                .username("testuser")
                .password(encodedPassword)
                .userType(UserType.NORMAL)
                .status(UserStatus.NORMAL)
                .build();

            when(userRepository.findByAccount("testuser")).thenReturn(Optional.of(user));
            when(passwordDomainService.matches(rawPassword, encodedPassword)).thenReturn(true);
            when(tokenService.createAccessToken(any(), any(), any())).thenReturn("access-token");
            when(tokenService.createRefreshToken(any(), any(), any())).thenReturn("refresh-token");

            LoginResponse expectedResponse = LoginResponse.builder()
                .token("access-token")
                .refreshToken("refresh-token")
                .user(UserVO.builder().userId(1L).username("testuser").build())
                .build();
            when(userAssembler.toLoginResponse(any(), anyString(), anyString())).thenReturn(expectedResponse);
            when(userRepository.updateLoginInfo(any(), any())).thenReturn(true);

            LoginResponse result = authAppService.login(loginRequest);

            assertThat(result).isNotNull();
            assertThat(result.getToken()).isEqualTo("access-token");
            assertThat(result.getRefreshToken()).isEqualTo("refresh-token");

            verify(loginSecurityDomainService).checkLoginAttempts("testuser");
            verify(loginSecurityDomainService).clearLoginAttempts("testuser");
            verify(userRepository).updateLoginInfo(eq(1L), any());
        }

        @Test
        @DisplayName("凭证错误时应抛出异常并记录失败尝试")
        void shouldThrowOnWrongCredentials() {
            LoginRequest loginRequest = LoginRequest.builder()
                .account("testuser")
                .password("WrongPassword")
                .loginMethod("password")
                .build();

            User user = User.builder()
                .id(1L)
                .username("testuser")
                .password("$2a$10$encoded")
                .status(UserStatus.NORMAL)
                .build();

            when(userRepository.findByAccount("testuser")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authAppService.login(loginRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号或密码错误");

            verify(loginSecurityDomainService).recordFailedAttempt("testuser");
            verify(loginSecurityDomainService, never()).clearLoginAttempts(any());
        }

        @Test
        @DisplayName("用户不存在时应抛出异常")
        void shouldThrowWhenUserNotFound() {
            LoginRequest loginRequest = LoginRequest.builder()
                .account("nonexistent")
                .password("Password123")
                .loginMethod("password")
                .build();

            when(userRepository.findByAccount("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authAppService.login(loginRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号或密码错误");

            verify(loginSecurityDomainService).recordFailedAttempt("nonexistent");
        }

        @Test
        @DisplayName("用户状态非正常时应抛出异常")
        void shouldThrowWhenUserDisabled() {
            LoginRequest loginRequest = LoginRequest.builder()
                .account("disableduser")
                .password("Password123")
                .loginMethod("password")
                .build();

            User disabledUser = User.builder()
                .id(1L)
                .username("disableduser")
                .password("$2a$10$encoded")
                .status(UserStatus.DISABLED)
                .build();

            when(userRepository.findByAccount("disableduser")).thenReturn(Optional.of(disabledUser));

            assertThatThrownBy(() -> authAppService.login(loginRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号或密码错误");

            verify(loginSecurityDomainService).recordFailedAttempt("disableduser");
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
        @DisplayName("应验证验证码后重置密码并发布事件")
        void shouldVerifyCodeAndResetPasswordAndPublishEvent() {
            ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .phone("13812345678")
                .verifyCode("123456")
                .newPassword("NewPassword123")
                .build();
            User user = User.builder().id(1L).username("testuser").phone("13812345678").build();
            when(userRepository.findByPhone("13812345678")).thenReturn(Optional.of(user));
            when(passwordDomainService.encode("NewPassword123")).thenReturn("$2a$10$newEncoded");
            when(userRepository.updatePassword(1L, "$2a$10$newEncoded")).thenReturn(true);

            Long result = authAppService.forgotPassword(request);

            assertThat(result).isEqualTo(1L);
            verify(smsCodeService).verifyCode("13812345678", "123456");
            verify(passwordDomainService).encode("NewPassword123");
            verify(userRepository).updatePassword(1L, "$2a$10$newEncoded");
            verify(userEventPublisher).publishForgotPassword(1L, "13812345678");
        }

        @Test
        @DisplayName("验证码无效时应抛出异常")
        void shouldThrowWhenVerifyCodeInvalid() {
            ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .phone("13812345678")
                .verifyCode("000000")
                .newPassword("NewPassword123")
                .build();
            doThrow(BusinessException.of("验证码无效或已过期")).when(smsCodeService).verifyCode("13812345678", "000000");

            assertThatThrownBy(() -> authAppService.forgotPassword(request))
                .isInstanceOf(BusinessException.class);

            verify(userRepository, never()).findByPhone(any());
            verify(userEventPublisher, never()).publishForgotPassword(any(), any());
        }

        @Test
        @DisplayName("手机号未注册时应抛出异常")
        void shouldThrowWhenPhoneNotRegistered() {
            ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .phone("13899999999")
                .verifyCode("123456")
                .newPassword("NewPassword123")
                .build();
            when(userRepository.findByPhone("13899999999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authAppService.forgotPassword(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("该手机号未注册");

            verify(userEventPublisher, never()).publishForgotPassword(any(), any());
        }

        @Test
        @DisplayName("更新密码失败时应抛出异常")
        void shouldThrowWhenUpdateFails() {
            ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .phone("13812345678")
                .verifyCode("123456")
                .newPassword("NewPassword123")
                .build();
            User user = User.builder().id(1L).username("testuser").phone("13812345678").build();
            when(userRepository.findByPhone("13812345678")).thenReturn(Optional.of(user));
            when(passwordDomainService.encode("NewPassword123")).thenReturn("$2a$10$newEncoded");
            when(userRepository.updatePassword(1L, "$2a$10$newEncoded")).thenReturn(false);

            assertThatThrownBy(() -> authAppService.forgotPassword(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("重置密码失败");

            verify(userEventPublisher, never()).publishForgotPassword(any(), any());
        }
    }
}
