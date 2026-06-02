package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import com.cartethyia.easyorange.user.domain.port.PasswordEncoderPort;
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
@DisplayName("PasswordManagementService 测试")
class PasswordManagementServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoderPort passwordEncoder;
    @Mock
    private SmsCodeService smsCodeService;

    private PasswordManagementService service;

    private static final String PHONE = "13812345678";
    private static final String VERIFY_CODE = "123456";
    private static final String NEW_PASSWORD = "NewPass123";
    private static final String ENCODED_PW = "$2a$10$encoded";
    private static final String NEW_ENCODED = "$2a$10$newEncoded";
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        service = new PasswordManagementService(userRepository, passwordEncoder, smsCodeService);
    }

    private User createTestUser() {
        return User.builder()
            .id(USER_ID)
            .credentials(new Credentials("testuser", ENCODED_PW))
            .userType(UserType.NORMAL)
            .status(UserStatus.NORMAL)
            .loginInfo(LoginInfo.empty())
            .build();
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPasswordTests {

        @Test
        @DisplayName("验证码正确且用户存在时重置成功")
        void success() {
            User user = createTestUser();

            doNothing().when(smsCodeService).verifyCode(PHONE, VERIFY_CODE);
            when(userRepository.findByPhone(PHONE)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(NEW_PASSWORD, ENCODED_PW)).thenReturn(false);
            when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(NEW_ENCODED);

            User result = service.resetPassword(PHONE, VERIFY_CODE, NEW_PASSWORD);

            assertThat(result).isNotNull();
            assertThat(result.getPassword()).isEqualTo(NEW_ENCODED);
            verify(smsCodeService).verifyCode(PHONE, VERIFY_CODE);
            verify(passwordEncoder).encode(NEW_PASSWORD);
            verify(userRepository).update(any(User.class));
        }

        @Test
        @DisplayName("验证码无效时抛出异常")
        void invalidVerifyCode() {
            doThrow(BusinessException.of(UserResultCode.SMS_CODE_INVALID))
                .when(smsCodeService).verifyCode(PHONE, VERIFY_CODE);

            assertThatThrownBy(() -> service.resetPassword(PHONE, VERIFY_CODE, NEW_PASSWORD))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("验证码无效");

            verify(userRepository, never()).findByPhone(any());
            verify(userRepository, never()).update(any());
        }

        @Test
        @DisplayName("用户不存在时抛出异常")
        void userNotFound() {
            doNothing().when(smsCodeService).verifyCode(PHONE, VERIFY_CODE);
            when(userRepository.findByPhone(PHONE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resetPassword(PHONE, VERIFY_CODE, NEW_PASSWORD))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在");

            verify(userRepository, never()).update(any());
        }

        @Test
        @DisplayName("新密码与当前密码相同时抛出异常")
        void samePassword() {
            User user = createTestUser();

            doNothing().when(smsCodeService).verifyCode(PHONE, VERIFY_CODE);
            when(userRepository.findByPhone(PHONE)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(NEW_PASSWORD, ENCODED_PW)).thenReturn(true);

            assertThatThrownBy(() -> service.resetPassword(PHONE, VERIFY_CODE, NEW_PASSWORD))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("新密码不能与旧密码相同");

            verify(passwordEncoder, never()).encode(any());
            verify(userRepository, never()).update(any());
        }
    }
}
