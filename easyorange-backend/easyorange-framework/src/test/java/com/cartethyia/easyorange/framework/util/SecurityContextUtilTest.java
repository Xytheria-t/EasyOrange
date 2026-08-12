package com.cartethyia.easyorange.framework.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.security.AuthUser;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

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
        @DisplayName("getCurrentUserId with anonymous auth should return empty")
        void getCurrentUserId_withAnonymousAuth_shouldReturnEmpty() {
            SecurityContextHolder.getContext().setAuthentication(anonymousToken());

            Optional<String> result = SecurityContextUtil.getCurrentUserId();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getCurrentUserId with AuthUser principal should return userId")
        void getCurrentUserId_withAuthUserPrincipal_shouldReturnUserId() {
            AuthUser authUser = new AuthUser("456", "testuser");
            SecurityContextHolder.getContext().setAuthentication(authenticatedToken(authUser));

            Optional<String> result = SecurityContextUtil.getCurrentUserId();

            assertThat(result).contains("456");
        }

        @Test
        @DisplayName("getCurrentUserId with AuthUser having null userId should return empty")
        void getCurrentUserId_withAuthUserNullUserId_shouldReturnEmpty() {
            AuthUser authUser = new AuthUser(null, "testuser");
            SecurityContextHolder.getContext().setAuthentication(authenticatedToken(authUser));

            Optional<String> result = SecurityContextUtil.getCurrentUserId();

            assertThat(result).isEmpty();
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
            AuthUser authUser = new AuthUser("1", "testuser");
            SecurityContextHolder.getContext().setAuthentication(authenticatedToken(authUser));

            Optional<AuthUser> result = SecurityContextUtil.getUserContext();

            assertThat(result).contains(authUser);
        }

        @Test
        @DisplayName("getUserContext with non-AuthUser principal should throw")
        void getUserContext_withNonAuthUserPrincipal_shouldThrow() {
            var auth = new UsernamePasswordAuthenticationToken("1", null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertThatThrownBy(() -> SecurityContextUtil.getUserContext())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("AuthUser");
        }

        @Test
        @DisplayName("getUserContext with null principal should throw instead of NPE")
        void getUserContext_withNullPrincipal_shouldThrow() {
            var auth = new UsernamePasswordAuthenticationToken(null, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertThatThrownBy(() -> SecurityContextUtil.getUserContext())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("AuthUser");
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
            var authUser = new AuthUser("1", "testuser");
            SecurityContextHolder.getContext().setAuthentication(authenticatedToken(authUser));

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
            SecurityContextHolder.getContext().setAuthentication(authenticatedToken(
                    new AuthUser("1", "testuser")));

            SecurityContextUtil.clearContext();

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    private static AnonymousAuthenticationToken anonymousToken() {
        return new AnonymousAuthenticationToken(
                "anonymousKey", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    }

    private static UsernamePasswordAuthenticationToken authenticatedToken(AuthUser authUser) {
        return new UsernamePasswordAuthenticationToken(authUser, null, List.of());
    }
}
