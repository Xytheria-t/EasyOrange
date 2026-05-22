package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import com.cartethyia.easyorange.user.domain.port.output.PasswordEncoderPort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.valueobject.Credentials;
import com.cartethyia.easyorange.user.domain.valueobject.LoginInfo;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService 测试")
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    @Mock
    private LoginSecurityService loginSecurityService;

    @Mock
    private SmsCodeService smsCodeService;

    private AuthenticationService service;

    private static final String ACCOUNT = "testuser";
    private static final String PASSWORD = "Password123";
    private static final String CLIENT_IP = "192.168.1.1";
    private static final String ENCODED_PW = "$2a$10$encoded";
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        service = new AuthenticationService(userRepository, passwordEncoder, loginSecurityService, smsCodeService);
    }

    private User createNormalUser() {
        return User.builder()
            .id(USER_ID)
            .credentials(new Credentials(ACCOUNT, ENCODED_PW))
            .userType(UserType.NORMAL)
            .status(UserStatus.NORMAL)
            .loginInfo(LoginInfo.initial())
            .build();
    }

    @Nested
    @DisplayName("authenticateByPassword")
    class AuthenticateByPasswordTests {

        @Test
        @DisplayName("密码认证成功")
        void success() {
            User user = createNormalUser();
            doNothing().when(loginSecurityService).checkLoginAttempts(ACCOUNT);
            when(userRepository.findByAccount(ACCOUNT)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(PASSWORD, ENCODED_PW)).thenReturn(true);
            when(userRepository.update(any(User.class))).thenReturn(true);

            User result = service.authenticateByPassword(ACCOUNT, PASSWORD, CLIENT_IP);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(USER_ID);
            verify(loginSecurityService).checkLoginAttempts(ACCOUNT);
            verify(loginSecurityService).clearLoginAttempts(ACCOUNT);
            verify(userRepository).update(any(User.class));
        }

        @Test
        @DisplayName("用户不存在时抛出异常")
        void userNotFound() {
            doNothing().when(loginSecurityService).checkLoginAttempts(ACCOUNT);
            when(userRepository.findByAccount(ACCOUNT)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.authenticateByPassword(ACCOUNT, PASSWORD, CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号或密码错误");

            verify(loginSecurityService).recordFailedAttempt(ACCOUNT);
            verify(loginSecurityService, never()).clearLoginAttempts(any());
            verify(userRepository, never()).update(any());
        }

        @Test
        @DisplayName("密码错误时抛出异常")
        void wrongPassword() {
            User user = createNormalUser();
            doNothing().when(loginSecurityService).checkLoginAttempts(ACCOUNT);
            when(userRepository.findByAccount(ACCOUNT)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(PASSWORD, ENCODED_PW)).thenReturn(false);

            assertThatThrownBy(() -> service.authenticateByPassword(ACCOUNT, PASSWORD, CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号或密码错误");

            verify(loginSecurityService).recordFailedAttempt(ACCOUNT);
            verify(loginSecurityService, never()).clearLoginAttempts(any());
            verify(userRepository, never()).update(any());
        }

        @Test
        @DisplayName("用户被禁用时抛出异常")
        void userDisabled() {
            User user = User.builder()
                .id(USER_ID)
                .credentials(new Credentials(ACCOUNT, ENCODED_PW))
                .status(UserStatus.DISABLED)
                .loginInfo(LoginInfo.initial())
                .build();
            doNothing().when(loginSecurityService).checkLoginAttempts(ACCOUNT);
            when(userRepository.findByAccount(ACCOUNT)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(PASSWORD, ENCODED_PW)).thenReturn(true);

            assertThatThrownBy(() -> service.authenticateByPassword(ACCOUNT, PASSWORD, CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账户已被禁用");

            verify(loginSecurityService).recordFailedAttempt(ACCOUNT);
            verify(loginSecurityService, never()).clearLoginAttempts(any());
            verify(userRepository, never()).update(any());
        }
    }

    @Nested
    @DisplayName("authenticateBySms")
    class AuthenticateBySmsTests {

        @Test
        @DisplayName("短信认证成功")
        void success() {
            String phone = "13812345678";
            String verifyCode = "123456";
            User user = createNormalUser();
            doNothing().when(smsCodeService).verifyCode(phone, verifyCode);
            when(userRepository.findByPhone(phone)).thenReturn(Optional.of(user));
            when(userRepository.update(any(User.class))).thenReturn(true);

            User result = service.authenticateBySms(phone, verifyCode, CLIENT_IP);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(USER_ID);
            verify(smsCodeService).verifyCode(phone, verifyCode);
            verify(userRepository).update(any(User.class));
        }

        @Test
        @DisplayName("用户不存在时抛出异常")
        void userNotFound() {
            String phone = "13812345678";
            String verifyCode = "123456";
            doNothing().when(smsCodeService).verifyCode(phone, verifyCode);
            when(userRepository.findByPhone(phone)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.authenticateBySms(phone, verifyCode, CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号或密码错误");

            verify(userRepository, never()).update(any());
        }

        @Test
        @DisplayName("用户被禁用时抛出异常")
        void userDisabled() {
            String phone = "13812345678";
            String verifyCode = "123456";
            User user = User.builder()
                .id(USER_ID)
                .credentials(new Credentials(ACCOUNT, ENCODED_PW))
                .status(UserStatus.DISABLED)
                .loginInfo(LoginInfo.initial())
                .build();
            doNothing().when(smsCodeService).verifyCode(phone, verifyCode);
            when(userRepository.findByPhone(phone)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> service.authenticateBySms(phone, verifyCode, CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号或密码错误");

            verify(userRepository, never()).update(any());
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPasswordTests {

        @Test
        @DisplayName("重置密码成功")
        void success() {
            String phone = "13812345678";
            String verifyCode = "123456";
            String newPassword = "NewPass123";
            User user = createNormalUser();

            doNothing().when(smsCodeService).verifyCode(phone, verifyCode);
            when(userRepository.findByPhone(phone)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode(newPassword)).thenReturn("$2a$10$newEncoded");
            when(userRepository.update(any(User.class))).thenReturn(true);

            User result = service.resetPassword(phone, verifyCode, newPassword);

            assertThat(result).isNotNull();
            assertThat(result.getPassword()).isEqualTo("$2a$10$newEncoded");
            verify(smsCodeService).verifyCode(phone, verifyCode);
            verify(passwordEncoder).encode(newPassword);
            verify(userRepository).update(any(User.class));
        }

        @Test
        @DisplayName("用户不存在时返回null")
        void userNotFound() {
            String phone = "13812345678";
            String verifyCode = "123456";
            String newPassword = "NewPass123";

            doNothing().when(smsCodeService).verifyCode(phone, verifyCode);
            when(userRepository.findByPhone(phone)).thenReturn(Optional.empty());

            User result = service.resetPassword(phone, verifyCode, newPassword);

            assertThat(result).isNull();
            verify(userRepository, never()).update(any());
        }
    }
}
