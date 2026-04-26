package com.cartethyia.easyorange.common.util;

import com.cartethyia.easyorange.common.dto.AuthUser;
import com.cartethyia.easyorange.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link SecurityContextUtil} 单元测试
 *
 * @author cartethyia
 */
@DisplayName("SecurityContextUtil Tests")
class SecurityContextUtilTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Authentication Tests")
    class AuthenticationTests {

        @Test
        @DisplayName("getAuthentication with no context should return empty")
        void getAuthentication_withNoContext_shouldReturnEmpty() {
            // Arrange
            SecurityContextHolder.clearContext();

            // Act
            Optional<org.springframework.security.core.Authentication> result = SecurityContextUtil.getAuthentication();

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getAuthentication with authenticated context should return authentication")
        void getAuthentication_withAuthenticatedContext_shouldReturnAuthentication() {
            // Arrange
            var auth = new UsernamePasswordAuthenticationToken("user", "password");
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Act
            Optional<org.springframework.security.core.Authentication> result = SecurityContextUtil.getAuthentication();

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(auth);
        }
    }

    @Nested
    @DisplayName("Is Authenticated Tests")
    class IsAuthenticatedTests {

        @Test
        @DisplayName("isAuthenticated with no context should return false")
        void isAuthenticated_withNoContext_shouldReturnFalse() {
            // Arrange
            SecurityContextHolder.clearContext();

            // Act
            boolean result = SecurityContextUtil.isAuthenticated();

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("isAuthenticated with unauthenticated context should return false")
        void isAuthenticated_withUnauthenticatedContext_shouldReturnFalse() {
            // Arrange
            var auth = new UsernamePasswordAuthenticationToken("anonymous", null);
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Act
            boolean result = SecurityContextUtil.isAuthenticated();

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("isAuthenticated with authenticated context should return true")
        void isAuthenticated_withAuthenticatedContext_shouldReturnTrue() {
            // Arrange
            var auth = new UsernamePasswordAuthenticationToken("user", "password", List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Act
            boolean result = SecurityContextUtil.isAuthenticated();

            // Assert
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("Get Current User ID Tests")
    class GetCurrentUserIdTests {

        @Test
        @DisplayName("getCurrentUserId with no context should return empty")
        void getCurrentUserId_withNoContext_shouldReturnEmpty() {
            // Arrange
            SecurityContextHolder.clearContext();

            // Act
            Optional<Long> result = SecurityContextUtil.getCurrentUserId();

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getCurrentUserId with Long principal should return id")
        void getCurrentUserId_withLongPrincipal_shouldReturnId() {
            // Arrange
            Long userId = 123L;
            var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Act
            Optional<Long> result = SecurityContextUtil.getCurrentUserId();

            // Assert
            assertThat(result).contains(userId);
        }

        @Test
        @DisplayName("getCurrentUserId with AuthUser principal should return userId")
        void getCurrentUserId_withAuthUserPrincipal_shouldReturnUserId() {
            // Arrange
            Long userId = 456L;
            AuthUser authUser = new AuthUser(userId, "testuser", null, null, System.currentTimeMillis());
            var auth = new UsernamePasswordAuthenticationToken(authUser, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Act
            Optional<Long> result = SecurityContextUtil.getCurrentUserId();

            // Assert
            assertThat(result).contains(userId);
        }

        @Test
        @DisplayName("getCurrentUserId with String principal should parse and return")
        void getCurrentUserId_withStringPrincipal_shouldParseAndReturn() {
            // Arrange
            String userIdStr = "789";
            var auth = new UsernamePasswordAuthenticationToken(userIdStr, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Act
            Optional<Long> result = SecurityContextUtil.getCurrentUserId();

            // Assert
            assertThat(result).contains(789L);
        }

        @Test
        @DisplayName("getCurrentUserId with invalid String principal should return empty")
        void getCurrentUserId_withInvalidStringPrincipal_shouldReturnEmpty() {
            // Arrange
            String userIdStr = "invalid";
            var auth = new UsernamePasswordAuthenticationToken(userIdStr, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Act
            Optional<Long> result = SecurityContextUtil.getCurrentUserId();

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getCurrentUserIdOrThrow with no context should throw")
        void getCurrentUserIdOrThrow_withNoContext_shouldThrow() {
            // Arrange
            SecurityContextHolder.clearContext();

            // Act & Assert
            assertThatThrownBy(SecurityContextUtil::getCurrentUserIdOrThrow)
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户未登录");
        }

        @Test
        @DisplayName("getCurrentUserIdOrThrow with authenticated should return id")
        void getCurrentUserIdOrThrow_withAuthenticated_shouldReturnId() {
            // Arrange
            Long userId = 999L;
            var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Act
            Long result = SecurityContextUtil.getCurrentUserIdOrThrow();

            // Assert
            assertThat(result).isEqualTo(userId);
        }
    }

    @Nested
    @DisplayName("Has Role Tests")
    class HasRoleTests {

        @Test
        @DisplayName("hasRole with null role should return false")
        void hasRole_withNullRole_shouldReturnFalse() {
            // Act
            boolean result = SecurityContextUtil.hasRole(null);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("hasRole with blank role should return false")
        void hasRole_withBlankRole_shouldReturnFalse() {
            // Act
            boolean result = SecurityContextUtil.hasRole("   ");

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("hasRole with matching role should return true")
        void hasRole_withMatchingRole_shouldReturnTrue() {
            // Arrange
            var auth = new UsernamePasswordAuthenticationToken(
                    "user", "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Act
            boolean result = SecurityContextUtil.hasRole("ADMIN");

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("hasRole with ROLE_ prefix should return true")
        void hasRole_withRolePrefix_shouldReturnTrue() {
            // Arrange
            var auth = new UsernamePasswordAuthenticationToken(
                    "user", "password", List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Act
            boolean result = SecurityContextUtil.hasRole("ROLE_USER");

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("hasRole with non-matching role should return false")
        void hasRole_withNonMatchingRole_shouldReturnFalse() {
            // Arrange
            var auth = new UsernamePasswordAuthenticationToken(
                    "user", "password", List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Act
            boolean result = SecurityContextUtil.hasRole("ADMIN");

            // Assert
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Has Authority Tests")
    class HasAuthorityTests {

        @Test
        @DisplayName("hasAuthority with null authority should return false")
        void hasAuthority_withNullAuthority_shouldReturnFalse() {
            // Act
            boolean result = SecurityContextUtil.hasAuthority(null);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("hasAuthority with matching authority should return true")
        void hasAuthority_withMatchingAuthority_shouldReturnTrue() {
            // Arrange
            var auth = new UsernamePasswordAuthenticationToken(
                    "user", "password", List.of(new SimpleGrantedAuthority("user:read"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Act
            boolean result = SecurityContextUtil.hasAuthority("user:read");

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("hasAuthority with non-matching authority should return false")
        void hasAuthority_withNonMatchingAuthority_shouldReturnFalse() {
            // Arrange
            var auth = new UsernamePasswordAuthenticationToken(
                    "user", "password", List.of(new SimpleGrantedAuthority("user:read"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Act
            boolean result = SecurityContextUtil.hasAuthority("user:write");

            // Assert
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Get User Context Tests")
    class GetUserContextTests {

        @Test
        @DisplayName("getUserContext with no context should return empty")
        void getUserContext_withNoContext_shouldReturnEmpty() {
            // Arrange
            SecurityContextHolder.clearContext();

            // Act
            Optional<AuthUser> result = SecurityContextUtil.getUserContext();

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getUserContext with AuthUser principal should return it")
        void getUserContext_withAuthUserPrincipal_shouldReturnIt() {
            // Arrange
            AuthUser authUser = new AuthUser(1L, "testuser", Set.of("ROLE_USER"), Set.of("user:read"), System.currentTimeMillis());
            var auth = new UsernamePasswordAuthenticationToken(authUser, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Act
            Optional<AuthUser> result = SecurityContextUtil.getUserContext();

            // Assert
            assertThat(result).contains(authUser);
        }

        @Test
        @DisplayName("getUserContext with other principal should build AuthUser")
        void getUserContext_withOtherPrincipal_shouldBuildAuthUser() {
            // Arrange
            var auth = new UsernamePasswordAuthenticationToken(
                    1L, "testuser", List.of(
                            new SimpleGrantedAuthority("ROLE_USER"),
                            new SimpleGrantedAuthority("user:read")
                    )
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Act
            Optional<AuthUser> result = SecurityContextUtil.getUserContext();

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().userId()).isEqualTo(1L);
            assertThat(result.get().username()).isEqualTo("testuser");
            assertThat(result.get().roles()).contains("USER");
            assertThat(result.get().permissions()).contains("user:read");
        }

        @Test
        @DisplayName("getUserContextOrThrow with no context should throw")
        void getUserContextOrThrow_withNoContext_shouldThrow() {
            // Arrange
            SecurityContextHolder.clearContext();

            // Act & Assert
            assertThatThrownBy(SecurityContextUtil::getUserContextOrThrow)
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户未登录");
        }

        @Test
        @DisplayName("getUserContextOrThrow with authenticated should return AuthUser")
        void getUserContextOrThrow_withAuthenticated_shouldReturnAuthUser() {
            // Arrange
            var auth = new UsernamePasswordAuthenticationToken(1L, "testuser", List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Act
            AuthUser result = SecurityContextUtil.getUserContextOrThrow();

            // Assert
            assertThat(result.userId()).isEqualTo(1L);
            assertThat(result.username()).isEqualTo("testuser");
        }
    }

    @Nested
    @DisplayName("Clear Context Tests")
    class ClearContextTests {

        @Test
        @DisplayName("clearContext should clear SecurityContextHolder")
        void clearContext_shouldClearSecurityContextHolder() {
            // Arrange
            var auth = new UsernamePasswordAuthenticationToken("user", "password", List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Act
            SecurityContextUtil.clearContext();

            // Assert
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }
}
