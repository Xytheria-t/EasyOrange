package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.port.PasswordEncoderPort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.valueobject.Credentials;
import com.cartethyia.easyorange.user.domain.valueobject.ImmutablePersonalInfo;
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

    @BeforeEach
    void setUp() {
        service = new RegistrationService(userRepository, passwordEncoder);
    }

    @Nested
    @DisplayName("registerNewUser")
    class RegisterNewUserTests {

        @Test
        @DisplayName("注册成功")
        void success() {
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
            when(passwordEncoder.encode(PASSWORD)).thenReturn("$2a$10$encoded");

            User savedUser = User.builder()
                .id("1")
                .credentials(new Credentials(USERNAME, "$2a$10$encoded"))
                .personalInfo(ImmutablePersonalInfo.builder().nickName(USERNAME).build())
                .build();
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            User result = service.registerNewUser(USERNAME, PASSWORD);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("1");
            verify(userRepository).findByUsername(USERNAME);
            verify(passwordEncoder).encode(PASSWORD);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("用户名已存在时抛出异常")
        void usernameAlreadyExists() {
            User existingUser = User.builder()
                .id("99")
                .credentials(new Credentials(USERNAME, "existing"))
                .build();
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(existingUser));

            assertThatThrownBy(() -> service.registerNewUser(USERNAME, PASSWORD))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名已存在");

            verify(passwordEncoder, never()).encode(any());
            verify(userRepository, never()).save(any());
        }
    }
}