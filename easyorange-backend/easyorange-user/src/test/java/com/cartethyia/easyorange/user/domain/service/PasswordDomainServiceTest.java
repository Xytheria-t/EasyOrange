package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordDomainService 测试")
class PasswordDomainServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    private PasswordDomainService passwordDomainService;

    @BeforeEach
    void setUp() {
        passwordDomainService = new PasswordDomainService(passwordEncoder);
    }

    @Nested
    @DisplayName("encode")
    class EncodeTests {

        @Test
        @DisplayName("应委托给 PasswordEncoder 进行编码")
        void shouldDelegateToEncoder() {
            // Arrange
            String rawPassword = "MyPassword123";
            String encodedPassword = "$2a$10$encodedHash";
            when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);

            // Act
            String result = passwordDomainService.encode(rawPassword);

            // Assert
            assertThat(result).isEqualTo(encodedPassword);
            verify(passwordEncoder).encode(rawPassword);
        }

        @Test
        @DisplayName("应对不同密码返回不同编码结果")
        void shouldReturnDifferentEncodedResults() {
            // Arrange
            when(passwordEncoder.encode("password1")).thenReturn("encoded1");
            when(passwordEncoder.encode("password2")).thenReturn("encoded2");

            // Act
            String result1 = passwordDomainService.encode("password1");
            String result2 = passwordDomainService.encode("password2");

            // Assert
            assertThat(result1).isEqualTo("encoded1");
            assertThat(result2).isEqualTo("encoded2");
        }
    }

    @Nested
    @DisplayName("matches")
    class MatchesTests {

        @Test
        @DisplayName("应委托给 PasswordEncoder 进行匹配验证")
        void shouldDelegateToEncoder() {
            // Arrange
            String rawPassword = "MyPassword123";
            String encodedPassword = "$2a$10$encodedHash";
            when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);

            // Act
            boolean result = passwordDomainService.matches(rawPassword, encodedPassword);

            // Assert
            assertThat(result).isTrue();
            verify(passwordEncoder).matches(rawPassword, encodedPassword);
        }

        @Test
        @DisplayName("密码不匹配时应返回 false")
        void shouldReturnFalseWhenNotMatch() {
            // Arrange
            when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

            // Act
            boolean result = passwordDomainService.matches("wrongPassword", "encodedPassword");

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("密码匹配时应返回 true")
        void shouldReturnTrueWhenMatch() {
            // Arrange
            when(passwordEncoder.matches("correctPassword", "encodedPassword")).thenReturn(true);

            // Act
            boolean result = passwordDomainService.matches("correctPassword", "encodedPassword");

            // Assert
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("validateDifferentPassword")
    class ValidateDifferentPasswordTests {

        @Test
        @DisplayName("新旧密码相同时应抛出异常")
        void shouldThrowWhenPasswordsAreSame() {
            // Act & Assert
            assertThatThrownBy(() -> passwordDomainService.validateDifferentPassword("samePassword", "samePassword"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("新密码不能与旧密码相同");
        }

        @Test
        @DisplayName("新旧密码不同时应通过验证")
        void shouldPassWhenPasswordsAreDifferent() {
            // Act & Assert - 不应抛出异常
            passwordDomainService.validateDifferentPassword("oldPassword", "newPassword");
        }

        @Test
        @DisplayName("空字符串密码相同时也应抛出异常")
        void shouldThrowWhenBothEmpty() {
            assertThatThrownBy(() -> passwordDomainService.validateDifferentPassword("", ""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("新密码不能与旧密码相同");
        }
    }
}
