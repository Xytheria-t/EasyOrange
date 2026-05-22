package com.cartethyia.easyorange.user.application.service.password;

import com.cartethyia.easyorange.common.dto.AuthUser;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.password.ChangePasswordRequest;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.Sex;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import com.cartethyia.easyorange.user.domain.event.PasswordChangedEvent;
import com.cartethyia.easyorange.user.domain.port.output.PasswordEncoderPort;
import com.cartethyia.easyorange.user.domain.port.output.UserEventPort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.valueobject.ContactInfo;
import com.cartethyia.easyorange.user.domain.valueobject.Credentials;
import com.cartethyia.easyorange.user.domain.valueobject.ImmutablePersonalInfo;
import com.cartethyia.easyorange.user.domain.valueobject.LoginInfo;
import com.cartethyia.easyorange.user.domain.valueobject.PersonalInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangePasswordAppService 测试")
class ChangePasswordAppServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    @Mock
    private UserEventPort userEventPort;

    private ChangePasswordAppService changePasswordAppService;

    @BeforeEach
    void setUp() {
        changePasswordAppService = new ChangePasswordAppService(userRepository, passwordEncoder, userEventPort);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContext(Long userId) {
        AuthUser authUser = AuthUser.builder()
            .userId(userId)
            .username("testuser")
            .roles(Set.of("ROLE_USER"))
            .permissions(Set.of("user:read"))
            .loginTime(System.currentTimeMillis())
            .build();
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(authUser, null, Set.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private User buildTestUser() {
        ContactInfo contactInfo = new ContactInfo("test@example.com", "13812345678");
        PersonalInfo personalInfo = ImmutablePersonalInfo.builder()
            .realName("张三")
            .sex(Sex.MALE)
            .avatar("/avatar/old.png")
            .build();

        return User.builder()
            .id(1L)
            .credentials(new Credentials("testuser", "$2a$10$encoded"))
            .userType(UserType.NORMAL)
            .status(UserStatus.NORMAL)
            .contactInfo(contactInfo)
            .personalInfo(personalInfo)
            .loginInfo(LoginInfo.initial())
            .build();
    }

    @Test
    @DisplayName("应验证并更新密码")
    void shouldValidateAndUpdatePassword() {
        setSecurityContext(1L);
        User user = buildTestUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPassword123", "$2a$10$encoded")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword456")).thenReturn("$2a$10$newEncoded");
        when(userRepository.update(any(User.class))).thenReturn(true);

        ChangePasswordRequest request = new ChangePasswordRequest("OldPassword123", "NewPassword456");

        changePasswordAppService.changePassword(request);

        verify(passwordEncoder).matches("OldPassword123", "$2a$10$encoded");
        verify(passwordEncoder).encode("NewPassword456");
        verify(userRepository).update(any(User.class));

        ArgumentCaptor<PasswordChangedEvent> eventCaptor = ArgumentCaptor.forClass(PasswordChangedEvent.class);
        verify(userEventPort).publishPasswordChanged(eventCaptor.capture());
        PasswordChangedEvent event = eventCaptor.getValue();
        assertThat(event.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("旧密码错误时应抛出异常")
    void shouldThrowWhenOldPasswordWrong() {
        setSecurityContext(1L);
        User user = buildTestUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongOldPassword", "$2a$10$encoded")).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest("WrongOldPassword", "NewPassword456");

        assertThatThrownBy(() -> changePasswordAppService.changePassword(request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("密码错误");

        verify(userRepository, never()).update(any());
        verify(userEventPort, never()).publishPasswordChanged(any());
    }

    @Test
    @DisplayName("新旧密码相同时应抛出异常")
    void shouldThrowWhenPasswordsAreSame() {
        setSecurityContext(1L);
        User user = buildTestUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ChangePasswordRequest request = new ChangePasswordRequest("SamePassword", "SamePassword");

        assertThatThrownBy(() -> changePasswordAppService.changePassword(request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("新密码不能与旧密码相同");
    }
}