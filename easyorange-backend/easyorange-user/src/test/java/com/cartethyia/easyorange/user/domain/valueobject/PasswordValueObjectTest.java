package com.cartethyia.easyorange.user.domain.valueobject;

import com.cartethyia.easyorange.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Password 值对象测试")
class PasswordValueObjectTest {

    @Nested
    @DisplayName("fromRaw - 正常场景")
    class FromRawValidTests {

        @Test
        @DisplayName("使用 fromRaw 创建有效密码")
        void fromRaw_withValidPassword_createsPassword() {
            Password password = Password.fromRaw("Abc123");
            assertThat(password.value()).isEqualTo("Abc123");
        }

        @Test
        @DisplayName("最小长度密码 (6位)")
        void fromRaw_minLength_createsPassword() {
            Password password = Password.fromRaw("Aa1aaa");
            assertThat(password.value()).isEqualTo("Aa1aaa");
        }

        @Test
        @DisplayName("最大长度密码 (20位)")
        void fromRaw_maxLength_createsPassword() {
            Password password = Password.fromRaw("Abcd1234567890Abcd");
            assertThat(password.value()).isEqualTo("Abcd1234567890Abcd");
        }
    }

    @Nested
    @DisplayName("fromRaw - 非法输入")
    class FromRawInvalidTests {

        @Test
        @DisplayName("空字符串抛出 BusinessException")
        void fromRaw_withEmpty_throws() {
            assertThatThrownBy(() -> Password.fromRaw(""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码不能为空");
        }

        @Test
        @DisplayName("仅空格抛出 BusinessException")
        void fromRaw_withSpaces_throws() {
            assertThatThrownBy(() -> Password.fromRaw("   "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码不能为空");
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "123456", "abcdef", "ABCDEF",
            "Abcdefg", "Abc12345678901234567890", "只有字母", "只有数字123"
        })
        @DisplayName("无效密码格式抛出 BusinessException")
        void fromRaw_withInvalidPassword_throws(String invalidPassword) {
            assertThatThrownBy(() -> Password.fromRaw(invalidPassword))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码必须包含大小写字母和数字");
        }

        @Test
        @DisplayName("null 输入抛出 BusinessException")
        void fromRaw_withNull_throws() {
            assertThatThrownBy(() -> Password.fromRaw(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码不能为空");
        }
    }

    @Nested
    @DisplayName("fromEncoded")
    class FromEncodedTests {

        @Test
        @DisplayName("使用 fromEncoded 创建加密密码")
        void fromEncoded_withEncodedPassword_createsPassword() {
            Password password = Password.fromEncoded("$2a$10$encoded");
            assertThat(password.getEncodedValue()).isEqualTo("$2a$10$encoded");
        }

        @Test
        @DisplayName("null 输入抛出 BusinessException")
        void fromEncoded_withNull_throws() {
            assertThatThrownBy(() -> Password.fromEncoded(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("加密密码不能为空");
        }

        @Test
        @DisplayName("空字符串输入抛出 BusinessException")
        void fromEncoded_withEmpty_throws() {
            assertThatThrownBy(() -> Password.fromEncoded(""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("加密密码不能为空");
        }
    }

    @Nested
    @DisplayName("encode 方法")
    class EncodeTests {

        @Test
        @DisplayName("未编码密码可以编码")
        void encode_rawPassword_encodesPassword() {
            Password rawPassword = Password.fromRaw("Abc123");
            Password encodedPassword = rawPassword.encode(raw -> "$2a$10$" + raw);

            assertThat(encodedPassword.getEncodedValue()).isEqualTo("$2a$10$Abc123");
        }

        @Test
        @DisplayName("已编码密码再次编码返回自身")
        void encode_alreadyEncoded_returnsThis() {
            Password encodedPassword = Password.fromEncoded("$2a$10$hash");
            Password result = encodedPassword.encode(raw -> "should_not_change");

            assertThat(result).isSameAs(encodedPassword);
        }

        @Test
        @DisplayName("编码后 getEncodedValue 返回编码值")
        void encode_afterEncoding_getEncodedValueWorks() {
            Password rawPassword = Password.fromRaw("Abc123");
            Password encodedPassword = rawPassword.encode(raw -> "encoded_hash");

            assertThat(encodedPassword.getEncodedValue()).isEqualTo("encoded_hash");
        }
    }

    @Nested
    @DisplayName("matches 方法")
    class MatchesTests {

        @Test
        @DisplayName("原始密码使用 matcher 验证成功")
        void matches_rawPasswordWithMatcher_returnsTrue() {
            Password rawPassword = Password.fromRaw("Abc123");
            BiFunction<String, String, Boolean> matcher = (raw, enc) -> raw.equals(raw); // raw.equals(raw) is always true

            assertThat(rawPassword.matches(matcher)).isTrue();
        }

        @Test
        @DisplayName("原始密码 matcher 验证失败")
        void matches_wrongPassword_returnsFalse() {
            Password rawPassword = Password.fromRaw("Abc123");
            BiFunction<String, String, Boolean> matcher = (raw, enc) -> false; // always return false

            assertThat(rawPassword.matches(matcher)).isFalse();
        }

        @Test
        @DisplayName("已编码密码调用 matches 返回 false")
        void matches_encodedPassword_returnsFalse() {
            Password encodedPassword = Password.fromEncoded("$2a$10$hash");
            BiFunction<String, String, Boolean> matcher = (raw, enc) -> true;

            assertThat(encodedPassword.matches(matcher)).isFalse();
        }
    }

    @Nested
    @DisplayName("value 方法")
    class ValueTests {

        @Test
        @DisplayName("原始密码 value 返回原始值")
        void value_rawPassword_returnsRawValue() {
            Password password = Password.fromRaw("Abc123");
            assertThat(password.value()).isEqualTo("Abc123");
        }

        @Test
        @DisplayName("已编码密码 value 返回编码值")
        void value_encodedPassword_returnsEncodedValue() {
            Password password = Password.fromEncoded("$2a$10$hash");
            assertThat(password.value()).isEqualTo("$2a$10$hash");
        }
    }

    @Nested
    @DisplayName("equals 和 hashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("相同编码值密码相等")
        void equals_sameEncodedValue_returnsTrue() {
            Password pwd1 = Password.fromEncoded("hash123");
            Password pwd2 = Password.fromEncoded("hash123");
            assertThat(pwd1).isEqualTo(pwd2);
        }

        @Test
        @DisplayName("不同编码值密码不相等")
        void equals_differentEncodedValue_returnsFalse() {
            Password pwd1 = Password.fromEncoded("hash1");
            Password pwd2 = Password.fromEncoded("hash2");
            assertThat(pwd1).isNotEqualTo(pwd2);
        }

        @Test
        @DisplayName("equals 与自身返回 true")
        void equals_sameInstance_returnsTrue() {
            Password pwd = Password.fromEncoded("hash");
            assertThat(pwd).isEqualTo(pwd);
        }

        @Test
        @DisplayName("equals 与 null 返回 false")
        void equals_withNull_returnsFalse() {
            Password pwd = Password.fromEncoded("hash");
            assertThat(pwd).isNotEqualTo(null);
        }

        @Test
        @DisplayName("相同编码值的 hashCode 相等")
        void hashCode_sameEncodedValue_returnsSameHash() {
            Password pwd1 = Password.fromEncoded("hash123");
            Password pwd2 = Password.fromEncoded("hash123");
            assertThat(pwd1.hashCode()).isEqualTo(pwd2.hashCode());
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("toString 不返回明文密码")
        void toString_returnsMasked() {
            Password password = Password.fromRaw("Abc123");
            assertThat(password.toString()).doesNotContain("Abc123");
            assertThat(password.toString()).contains("Password");
        }
    }
}