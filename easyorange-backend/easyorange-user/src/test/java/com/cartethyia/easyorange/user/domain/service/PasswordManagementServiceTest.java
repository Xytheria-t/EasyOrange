package com.cartethyia.easyorange.user.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.aggregate.UserTestFixture;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.port.PasswordEncoderPort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordManagementService 测试")
class PasswordManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    @Mock
    private SmsVerificationService smsVerificationService;

    private PasswordManagementService service;

    private static final String USER_ID = UserTestFixture.USER_ID;
    private static final String PHONE = UserTestFixture.PHONE;
    private static final String OLD_ENCODED = "encodedOld";
    private static final String NEW_ENCODED = "encodedNew";

    @BeforeEach
    void setUp() {
        service = new PasswordManagementService(userRepository, passwordEncoder, smsVerificationService);
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePasswordTests {

        @Test
        @DisplayName("旧密码正确 — 更新为新密码")
        void success() {
            User user = UserTestFixture.userWithCredentials("testuser", OLD_ENCODED);
            when(passwordEncoder.matches("oldPwd123", OLD_ENCODED)).thenReturn(true);
            when(passwordEncoder.matches("NewPass123", OLD_ENCODED)).thenReturn(false);
            when(passwordEncoder.encode("NewPass123")).thenReturn(NEW_ENCODED);

            User result = service.changePassword(user, "oldPwd123", "NewPass123");

            assertThat(result).isNotNull();
            assertThat(result.getPassword()).isEqualTo(NEW_ENCODED);
        }

        @Test
        @DisplayName("旧密码错误 — 抛 B1011")
        void wrongOldPassword() {
            User user = UserTestFixture.userWithCredentials("testuser", OLD_ENCODED);
            when(passwordEncoder.matches("wrongOld", OLD_ENCODED)).thenReturn(false);

            assertThatThrownBy(() -> service.changePassword(user, "wrongOld", "NewPass123"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("账号或密码错误");
        }

        @Test
        @DisplayName("新密码与旧密码相同 — 抛 B1013")
        void sameAsOld() {
            User user = UserTestFixture.userWithCredentials("testuser", OLD_ENCODED);
            when(passwordEncoder.matches("oldPwd123", OLD_ENCODED)).thenReturn(true);

            assertThatThrownBy(() -> service.changePassword(user, "oldPwd123", "oldPwd123"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(UserResultCode.PASSWORD_SAME_AS_OLD.getCode());
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPasswordTests {

        @Test
        @DisplayName("验证码正确 — 更新密码")
        void success() {
            User user = UserTestFixture.userWithCredentials("testuser", OLD_ENCODED);
            when(userRepository.findByPhone(PHONE)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("NewPass123", OLD_ENCODED)).thenReturn(false);
            when(passwordEncoder.encode("NewPass123")).thenReturn(NEW_ENCODED);

            User result = service.resetPassword(PHONE, "123456", "NewPass123");

            assertThat(result.getPassword()).isEqualTo(NEW_ENCODED);
            verify(smsVerificationService).verifyCodeOrThrow(PHONE, "123456");
        }

        @Test
        @DisplayName("手机号无对应用户 — 抛 B1001")
        void userNotFound() {
            when(userRepository.findByPhone(PHONE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resetPassword(PHONE, "123456", "NewPass123"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户不存在");
        }

        @Test
        @DisplayName("验证码校验失败 — 不查库直接抛")
        void verifyCodeFail() {
            doThrow(BusinessException.of(UserResultCode.SMS_CODE_INVALID))
                    .when(smsVerificationService)
                    .verifyCodeOrThrow(PHONE, "123456");

            assertThatThrownBy(() -> service.resetPassword(PHONE, "123456", "NewPass123"))
                    .isInstanceOf(BusinessException.class);

            verify(userRepository, never()).findByPhone(any());
        }
    }
}
