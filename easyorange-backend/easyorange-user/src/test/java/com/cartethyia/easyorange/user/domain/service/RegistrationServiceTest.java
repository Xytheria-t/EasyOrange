package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.port.output.PasswordEncoderPort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.valueobject.Credentials;
import com.cartethyia.easyorange.user.domain.valueobject.ImmutablePersonalInfo;
import com.cartethyia.easyorange.user.domain.valueobject.PersonalInfo;
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
@DisplayName("RegistrationService 测试")
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    private RegistrationService service;

    private static final String USERNAME = "newuser";
    private static final String PASSWORD = "Password123";
    private static final String PHONE = "13812345678";
    private static final String EMAIL = "test@example.com";
    private static final String NICKNAME = "阳光番茄";

    @BeforeEach
    void setUp() {
        service = new RegistrationService(userRepository, passwordEncoder);
    }

    @Nested
    @DisplayName("registerNewUser")
    class RegisterNewUserTests {

        @Test
        @DisplayName("注册成功 — 无联系方式")
        void success_withoutContact() {
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
            when(passwordEncoder.encode(PASSWORD)).thenReturn("$2a$10$encoded");

            User savedUser = User.builder()
                .id(1L)
                .credentials(new Credentials(USERNAME, "$2a$10$encoded"))
                .personalInfo(ImmutablePersonalInfo.builder().nickName(NICKNAME).build())
                .build();
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            User result = service.registerNewUser(USERNAME, PASSWORD, null, null, NICKNAME);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(userRepository).findByUsername(USERNAME);
            verify(passwordEncoder).encode(PASSWORD);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("注册成功 — 含手机和邮箱")
        void success_withContact() {
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
            when(userRepository.findByPhone(PHONE)).thenReturn(Optional.empty());
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
            when(passwordEncoder.encode(PASSWORD)).thenReturn("$2a$10$encoded");

            User savedUser = User.builder()
                .id(1L)
                .credentials(new Credentials(USERNAME, "$2a$10$encoded"))
                .personalInfo(ImmutablePersonalInfo.builder().nickName(NICKNAME).build())
                .build();
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            User result = service.registerNewUser(USERNAME, PASSWORD, PHONE, EMAIL, NICKNAME);

            assertThat(result).isNotNull();
            verify(userRepository).findByUsername(USERNAME);
            verify(userRepository).findByPhone(PHONE);
            verify(userRepository).findByEmail(EMAIL);
            verify(passwordEncoder).encode(PASSWORD);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("用户名已存在时抛出异常")
        void usernameAlreadyExists() {
            User existingUser = User.builder()
                .id(99L)
                .credentials(new Credentials(USERNAME, "existing"))
                .build();
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(existingUser));

            assertThatThrownBy(() -> service.registerNewUser(USERNAME, PASSWORD, null, null, NICKNAME))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名已存在");

            verify(passwordEncoder, never()).encode(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("手机号已存在时抛出异常")
        void phoneAlreadyExists() {
            User existingUser = User.builder()
                .id(99L)
                .credentials(new Credentials("other", "existing"))
                .build();
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
            when(userRepository.findByPhone(PHONE)).thenReturn(Optional.of(existingUser));

            assertThatThrownBy(() -> service.registerNewUser(USERNAME, PASSWORD, PHONE, null, NICKNAME))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("手机号已被注册");

            verify(passwordEncoder, never()).encode(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("邮箱已存在时抛出异常")
        void emailAlreadyExists() {
            User existingUser = User.builder()
                .id(99L)
                .credentials(new Credentials("other", "existing"))
                .build();
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser));

            assertThatThrownBy(() -> service.registerNewUser(USERNAME, PASSWORD, null, EMAIL, NICKNAME))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邮箱已被注册");

            verify(passwordEncoder, never()).encode(any());
            verify(userRepository, never()).save(any());
        }
    }
}
