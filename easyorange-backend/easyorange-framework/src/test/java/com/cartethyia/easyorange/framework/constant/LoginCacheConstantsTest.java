package com.cartethyia.easyorange.framework.constant;

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
        @DisplayName("TOKEN_KEY should be 'eo:user:token:'")
        void tokenKey_shouldBeCorrect() {
            assertThat(LoginCacheConstants.TOKEN_KEY).isEqualTo("eo:user:token:");
        }

        @Test
        @DisplayName("ATTEMPTS_KEY should be 'eo:user:login:attempts:'")
        void attemptsKey_shouldBeCorrect() {
            assertThat(LoginCacheConstants.ATTEMPTS_KEY).isEqualTo("eo:user:login:attempts:");
        }

        @Test
        @DisplayName("ATTEMPTS_EXPIRE_TIME should be 30")
        void attemptsExpireTime_shouldBe30() {
            assertThat(LoginCacheConstants.ATTEMPTS_EXPIRE_TIME).isEqualTo(30L);
        }

        @Test
        @DisplayName("MAX_LOGIN_ATTEMPTS should be 5")
        void maxLoginAttempts_shouldBe5() {
            assertThat(LoginCacheConstants.MAX_LOGIN_ATTEMPTS).isEqualTo(5);
        }

        @Test
        @DisplayName("LOGIN_LOCK_MINUTES should be 30")
        void loginLockMinutes_shouldBe30() {
            assertThat(LoginCacheConstants.LOGIN_LOCK_MINUTES).isEqualTo(30);
        }

        @Test
        @DisplayName("PASSWORD_REGEX should be correct")
        void passwordRegex_shouldBeCorrect() {
            assertThat(LoginCacheConstants.PASSWORD_REGEX)
                    .isEqualTo("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{6,20}$");
        }

        @Test
        @DisplayName("PHONE_PATTERN should match valid phone numbers")
        void phonePattern_shouldMatchValidPhones() {
            assertThat(LoginCacheConstants.PHONE_PATTERN.matcher("13800138000").matches()).isTrue();
            assertThat(LoginCacheConstants.PHONE_PATTERN.matcher("15912345678").matches()).isTrue();
        }

        @Test
        @DisplayName("PHONE_PATTERN should reject invalid phone numbers")
        void phonePattern_shouldRejectInvalidPhones() {
            assertThat(LoginCacheConstants.PHONE_PATTERN.matcher("12300138000").matches()).isFalse();
            assertThat(LoginCacheConstants.PHONE_PATTERN.matcher("1380013800").matches()).isFalse();
            assertThat(LoginCacheConstants.PHONE_PATTERN.matcher("abc").matches()).isFalse();
        }
    }

    @Nested
    @DisplayName("buildTokenKey")
    class BuildTokenKeyTests {

        @Test
        @DisplayName("buildTokenKey should return correct key format")
        void buildTokenKey_withValidToken_shouldReturnCorrectKey() {
            String token = "abc123";
            String key = LoginCacheConstants.buildTokenKey(token);

            assertThat(key).isEqualTo("eo:user:token:abc123");
        }

        @Test
        @DisplayName("buildTokenKey with null token should throw")
        void buildTokenKey_withNullToken_shouldThrow() {
            assertThatThrownBy(() -> LoginCacheConstants.buildTokenKey(null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("token 不能为 null");
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
                    .hasMessageContaining("username 不能为 null");
        }
    }
}
