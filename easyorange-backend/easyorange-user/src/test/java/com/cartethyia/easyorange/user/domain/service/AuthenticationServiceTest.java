package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import com.cartethyia.easyorange.user.domain.port.PasswordEncoderPort;
import com.cartethyia.easyorange.user.domain.port.SmsCodePort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.valueobject.ContactInfo;
import com.cartethyia.easyorange.user.domain.valueobject.Credentials;
import com.cartethyia.easyorange.user.domain.valueobject.LoginCredential;
import com.cartethyia.easyorange.user.domain.valueobject.LoginInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.cartethyia.easyorange.user.domain.port.SmsCodePort.VerifyResult;
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
    private SmsCodePort smsCodePort;

    private AuthenticationService service;

    private static final String ACCOUNT = "testuser";
    private static final String PASSWORD = "Password123";
    private static final String ENCODED_PW = "$2a$10$encoded";
    private static final String USER_ID = "1";
    private static final String PHONE = "13812345678";
    private static final String VERIFY_CODE = "123456";

    @BeforeEach
    void setUp() {
        service = new AuthenticationService(userRepository, passwordEncoder, loginSecurityService, smsCodePort);
    }

    private User createNormalUser() {
        return User.builder()
            .id(USER_ID)
            .credentials(new Credentials(ACCOUNT, ENCODED_PW))
            .userType(UserType.NORMAL)
            .status(UserStatus.NORMAL)
            .loginInfo(LoginInfo.empty())
            .build();
    }

    @Nested
    @DisplayName("authenticate — PasswordLogin")
    class AuthenticateByPasswordTests {

        @Test
        @DisplayName("密码认证成功")
        void success() {
            User user = createNormalUser();
            doNothing().when(loginSecurityService).checkAndThrowIfLocked(ACCOUNT);
            when(userRepository.findByLoginIdentifier(ACCOUNT)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(PASSWORD, ENCODED_PW)).thenReturn(true);

            User result = service.authenticate(new LoginCredential.Password(ACCOUNT, PASSWORD));

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(USER_ID);
            verify(loginSecurityService).checkAndThrowIfLocked(ACCOUNT);
            verify(loginSecurityService).clear(ACCOUNT);
        }

        @Test
        @DisplayName("用户不存在时抛出异常")
        void userNotFound() {
            doNothing().when(loginSecurityService).checkAndThrowIfLocked(ACCOUNT);
            when(userRepository.findByLoginIdentifier(ACCOUNT)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.authenticate(new LoginCredential.Password(ACCOUNT, PASSWORD)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号或密码错误");

            verify(loginSecurityService).incrementAndCheck(ACCOUNT);
            verify(loginSecurityService, never()).clear(any());
        }

        @Test
        @DisplayName("密码错误时抛出异常")
        void wrongPassword() {
            User user = createNormalUser();
            doNothing().when(loginSecurityService).checkAndThrowIfLocked(ACCOUNT);
            when(userRepository.findByLoginIdentifier(ACCOUNT)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(PASSWORD, ENCODED_PW)).thenReturn(false);

            assertThatThrownBy(() -> service.authenticate(new LoginCredential.Password(ACCOUNT, PASSWORD)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号或密码错误");

            verify(loginSecurityService).incrementAndCheck(ACCOUNT);
            verify(loginSecurityService, never()).clear(any());
        }

        @Test
        @DisplayName("用户被禁用时抛出异常")
        void userDisabled() {
            User user = User.builder()
                .id(USER_ID)
                .credentials(new Credentials(ACCOUNT, ENCODED_PW))
                .status(UserStatus.DISABLED)
                .loginInfo(LoginInfo.empty())
                .build();
            doNothing().when(loginSecurityService).checkAndThrowIfLocked(ACCOUNT);
            when(userRepository.findByLoginIdentifier(ACCOUNT)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(PASSWORD, ENCODED_PW)).thenReturn(true);

            assertThatThrownBy(() -> service.authenticate(new LoginCredential.Password(ACCOUNT, PASSWORD)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账户已被禁用");

            verify(loginSecurityService, never()).incrementAndCheck(ACCOUNT);
            verify(loginSecurityService, never()).clear(any());
        }
    }

    @Nested
    @DisplayName("authenticate — SmsLogin")
    class AuthenticateBySmsTests {

        @Test
        @DisplayName("短信认证成功")
        void success() {
            String phone = "13812345678";
            String verifyCode = "123456";
            User user = createNormalUser();
            when(smsCodePort.verify(phone, verifyCode)).thenReturn(VerifyResult.OK);
            when(userRepository.findByPhone(phone)).thenReturn(Optional.of(user));

            User result = service.authenticate(new LoginCredential.Sms(phone, verifyCode));

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(USER_ID);
            verify(smsCodePort).verify(phone, verifyCode);
        }

        @Test
        @DisplayName("用户不存在时抛出异常")
        void userNotFound() {
            String phone = "13812345678";
            String verifyCode = "123456";
            when(smsCodePort.verify(phone, verifyCode)).thenReturn(VerifyResult.OK);
            when(userRepository.findByPhone(phone)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.authenticate(new LoginCredential.Sms(phone, verifyCode)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号或密码错误");
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
                .loginInfo(LoginInfo.empty())
                .build();
            when(smsCodePort.verify(phone, verifyCode)).thenReturn(VerifyResult.OK);
            when(userRepository.findByPhone(phone)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> service.authenticate(new LoginCredential.Sms(phone, verifyCode)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账户已被禁用");
        }
    }
}
