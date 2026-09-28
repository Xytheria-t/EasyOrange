package com.cartethyia.easyorange.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.auth.TokenService;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.aggregate.UserTestFixture;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.event.UserPasswordChangedEvent;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.service.PasswordManagementService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CredentialAppService 测试")
class CredentialAppServiceTest {

    @Mock
    private PasswordManagementService passwordManagementService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private CredentialAppService service;

    private static final String USER_ID = UserTestFixture.USER_ID;
    private static final String PHONE = UserTestFixture.PHONE;

    @BeforeEach
    void setUp() {
        service =
                new CredentialAppService(passwordManagementService, userRepository, tokenService, domainEventPublisher);
    }

    @Nested
    @DisplayName("重置密码（忘记密码）")
    class ResetPassword {

        @Test
        @DisplayName("应委托 PasswordManagementService 重置并保存")
        void shouldDelegateAndSave() {
            String verifyCode = "123456";
            String newPassword = "NewPass123";

            User updated = UserTestFixture.userWithCredentials("testuser", "encodedNewPwd");
            when(passwordManagementService.resetPassword(PHONE, verifyCode, newPassword))
                    .thenReturn(updated);

            service.resetPassword(PHONE, verifyCode, newPassword);

            verify(passwordManagementService).resetPassword(PHONE, verifyCode, newPassword);
            verify(userRepository).update(updated);
        }

        @Test
        @DisplayName("找回密码同样吊销旧会话并发 source=sms 的改密事件")
        void shouldRevokeSessionsAndPublishEvent() {
            String verifyCode = "123456";
            String newPassword = "NewPass123";
            when(passwordManagementService.resetPassword(PHONE, verifyCode, newPassword))
                    .thenReturn(UserTestFixture.userWithCredentials("testuser", "encodedNewPwd"));

            service.resetPassword(PHONE, verifyCode, newPassword);

            verify(tokenService).revokeAllUserSessions(USER_ID);
            assertThat(capturedEvent().source()).isEqualTo("sms");
        }
    }

    @Nested
    @DisplayName("修改密码（已登录）")
    class ChangePassword {

        @Test
        @DisplayName("应查询用户后委托 PasswordManagementService 并保存、吊销会话、发事件")
        void shouldFindUserAndDelegateToChangePassword() {
            var user = UserTestFixture.userWithCredentials("testuser", "encodedOldPwd");
            var updatedUser = UserTestFixture.userWithCredentials("testuser", "encodedNewPwd");
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(passwordManagementService.changePassword(user, "oldPwd123", "NewPass123"))
                    .thenReturn(updatedUser);

            service.changePassword(USER_ID, "oldPwd123", "NewPass123");

            verify(passwordManagementService).changePassword(user, "oldPwd123", "NewPass123");
            verify(userRepository).update(updatedUser);
            verify(tokenService).revokeAllUserSessions(USER_ID);
        }

        @Test
        @DisplayName("应发 UserPasswordChangedEvent（source=self）")
        void shouldPublishPasswordChangedEvent() {
            var user = UserTestFixture.userWithCredentials("testuser", "encodedOldPwd");
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(passwordManagementService.changePassword(user, "oldPwd123", "NewPass123"))
                    .thenReturn(UserTestFixture.userWithCredentials("testuser", "encodedNewPwd"));

            service.changePassword(USER_ID, "oldPwd123", "NewPass123");

            UserPasswordChangedEvent event = capturedEvent();
            assertThat(event.userId()).isEqualTo(USER_ID);
            assertThat(event.source()).isEqualTo("self");
            assertThat(event.eventId()).isNotBlank();
        }

        @Test
        @DisplayName("用户不存在时应抛出 USER_NOT_FOUND")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById("999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.changePassword("999", "123456", "NewPass123"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(UserResultCode.USER_NOT_FOUND.getCode());
        }
    }

    private UserPasswordChangedEvent capturedEvent() {
        var eventCaptor = ArgumentCaptor.forClass(UserPasswordChangedEvent.class);
        verify(domainEventPublisher).publish(eventCaptor.capture());
        return eventCaptor.getValue();
    }
}
