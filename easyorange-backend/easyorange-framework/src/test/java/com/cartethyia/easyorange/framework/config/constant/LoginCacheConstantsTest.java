package com.cartethyia.easyorange.framework.config.constant;

import com.cartethyia.easyorange.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LoginCacheConstants Tests")
class LoginCacheConstantsTest {

    @Nested
    @DisplayName("Constants")
    class ConstantsTests {

        @Test
        @DisplayName("APP_PREFIX should be 'eo:'")
        void appPrefix_shouldBeEo() {
            assertThat(LoginCacheConstants.APP_PREFIX).isEqualTo("eo:");
        }

        @Test
        @DisplayName("TOKEN_BLACKLIST_KEY should be correct")
        void tokenBlacklistKey_shouldBeCorrect() {
            assertThat(LoginCacheConstants.TOKEN_BLACKLIST_KEY).isEqualTo("eo:user:token:blacklist:");
        }

        @Test
        @DisplayName("ATTEMPTS_KEY should be 'eo:user:login:attempts:'")
        void attemptsKey_shouldBeCorrect() {
            assertThat(LoginCacheConstants.ATTEMPTS_KEY).isEqualTo("eo:user:login:attempts:");
        }

        @Test
        @DisplayName("FORCE_LOGOUT_KEY should be 'eo:user:token:force-logout:'")
        void forceLogoutKey_shouldBeCorrect() {
            assertThat(LoginCacheConstants.FORCE_LOGOUT_KEY).isEqualTo("eo:user:token:force-logout:");
        }
    }

    @Nested
    @DisplayName("buildAttemptsKey")
    class BuildAttemptsKeyTests {

        @Test
        @DisplayName("buildAttemptsKey should return correct key format")
        void buildAttemptsKey_withValidUsername_shouldReturnCorrectKey() {
            String username = "admin";
            String key = LoginCacheConstants.buildAttemptsKey(username);

            assertThat(key).isEqualTo("eo:user:login:attempts:admin");
        }

        @Test
        @DisplayName("buildAttemptsKey with null username should throw")
        void buildAttemptsKey_withNullUsername_shouldThrow() {
            assertThatThrownBy(() -> LoginCacheConstants.buildAttemptsKey(null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("identifier 不能为 null");
        }
    }
}