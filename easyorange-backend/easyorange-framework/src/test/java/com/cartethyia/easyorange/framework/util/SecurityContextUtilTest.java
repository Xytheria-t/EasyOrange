package com.cartethyia.easyorange.framework.util;

import com.cartethyia.easyorange.common.security.AuthUser;
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

@DisplayName("SecurityContextUtil Tests")
class SecurityContextUtilTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Get Current User ID Tests")
    class GetCurrentUserIdTests {

        @Test
        @DisplayName("getCurrentUserId with no context should return empty")
        void getCurrentUserId_withNoContext_shouldReturnEmpty() {
            SecurityContextHolder.clearContext();

            Optional<String> result = SecurityContextUtil.getCurrentUserId();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getCurrentUserId with Long principal should return id")
        void getCurrentUserId_withLongPrincipal_shouldReturnId() {
            Long userId = 123L;
            var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

            Optional<String> result = SecurityContextUtil.getCurrentUserId();

            assertThat(result).contains("123");
        }

        @Test
        @DisplayName("getCurrentUserId with AuthUser principal should return userId")
        void getCurrentUserId_withAuthUserPrincipal_shouldReturnUserId() {
            AuthUser authUser = new AuthUser("456", "testuser", null, null, System.currentTimeMillis());
            var auth = new UsernamePasswordAuthenticationToken(authUser, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

            Optional<String> result = SecurityContextUtil.getCurrentUserId();

            assertThat(result).contains("456");
        }

        @Test
        @DisplayName("getCurrentUserId with String principal should return it")
        void getCurrentUserId_withStringPrincipal_shouldReturnIt() {
            String userIdStr = "789";
            var auth = new UsernamePasswordAuthenticationToken(userIdStr, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

            Optional<String> result = SecurityContextUtil.getCurrentUserId();

            assertThat(result).contains("789");
        }

        @Test
        @DisplayName("getCurrentUserId with AuthUser having null userId should return empty")
        void getCurrentUserId_withAuthUserNullUserId_shouldReturnEmpty() {
            AuthUser authUser = new AuthUser(null, "testuser", null, null, System.currentTimeMillis());
            var auth = new UsernamePasswordAuthenticationToken(authUser, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

            Optional<String> result = SecurityContextUtil.getCurrentUserId();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getCurrentUserIdOrThrow with no context should throw")
        void getCurrentUserIdOrThrow_withNoContext_shouldThrow() {
            SecurityContextHolder.clearContext();

            assertThatThrownBy(SecurityContextUtil::getCurrentUserIdOrThrow)
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户未登录");
        }

        @Test
        @DisplayName("getCurrentUserIdOrThrow with authenticated should return id")
        void getCurrentUserIdOrThrow_withAuthenticated_shouldReturnId() {
            var auth = new UsernamePasswordAuthenticationToken("999", null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

            String result = SecurityContextUtil.getCurrentUserIdOrThrow();

            assertThat(result).isEqualTo("999");
        }
    }

    @Nested
    @DisplayName("Get User Context Tests")
    class GetUserContextTests {

        @Test
        @DisplayName("getUserContext with no context should return empty")
        void getUserContext_withNoContext_shouldReturnEmpty() {
            SecurityContextHolder.clearContext();

            Optional<AuthUser> result = SecurityContextUtil.getUserContext();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getUserContext with AuthUser principal should return it")
        void getUserContext_withAuthUserPrincipal_shouldReturnIt() {
            AuthUser authUser = new AuthUser("1", "testuser", Set.of("ROLE_USER"), Set.of("user:read"), System.currentTimeMillis());
            var auth = new UsernamePasswordAuthenticationToken(authUser, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

            Optional<AuthUser> result = SecurityContextUtil.getUserContext();

            assertThat(result).contains(authUser);
        }

        @Test
        @DisplayName("getUserContext with other principal should build AuthUser")
        void getUserContext_withOtherPrincipal_shouldBuildAuthUser() {
            var auth = new UsernamePasswordAuthenticationToken(
                    "1", "testuser", List.of(
                            new SimpleGrantedAuthority("ROLE_USER"),
                            new SimpleGrantedAuthority("user:read")
                    )
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            Optional<AuthUser> result = SecurityContextUtil.getUserContext();

            assertThat(result).isPresent();
            assertThat(result.get().userId()).isEqualTo("1");
            assertThat(result.get().username()).isEqualTo("testuser");
            assertThat(result.get().roles()).contains("USER");
            assertThat(result.get().permissions()).contains("user:read");
        }

        @Test
        @DisplayName("getUserContextOrThrow with no context should throw")
        void getUserContextOrThrow_withNoContext_shouldThrow() {
            SecurityContextHolder.clearContext();

            assertThatThrownBy(SecurityContextUtil::getUserContextOrThrow)
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户未登录");
        }

        @Test
        @DisplayName("getUserContextOrThrow with authenticated should return AuthUser")
        void getUserContextOrThrow_withAuthenticated_shouldReturnAuthUser() {
            var auth = new UsernamePasswordAuthenticationToken("1", "testuser", List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

            AuthUser result = SecurityContextUtil.getUserContextOrThrow();

            assertThat(result.userId()).isEqualTo("1");
            assertThat(result.username()).isEqualTo("testuser");
        }
    }

    @Nested
    @DisplayName("Clear Context Tests")
    class ClearContextTests {

        @Test
        @DisplayName("clearContext should clear SecurityContextHolder")
        void clearContext_shouldClearSecurityContextHolder() {
            var auth = new UsernamePasswordAuthenticationToken("user", "password", List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

            SecurityContextUtil.clearContext();

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }
}
