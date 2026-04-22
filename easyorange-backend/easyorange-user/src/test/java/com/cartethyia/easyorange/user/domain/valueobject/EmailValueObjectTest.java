package com.cartethyia.easyorange.user.domain.valueobject;

import com.cartethyia.easyorange.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Email 值对象测试")
class EmailValueObjectTest {

    @Nested
    @DisplayName("构造函数 - 正常场景")
    class ConstructorValidTests {

        @Test
        @DisplayName("使用构造函数创建有效邮箱")
        void constructor_withValidEmail_createsEmail() {
            Email email = new Email("Test@Example.COM");
            assertThat(email.value()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("使用 of 静态工厂方法创建邮箱")
        void of_withValidEmail_createsEmail() {
            Email email = Email.of("user@domain.org");
            assertThat(email.value()).isEqualTo("user@domain.org");
        }

        @Test
        @DisplayName("邮箱值返回小写格式")
        void value_withMixedCase_returnsLowerCase() {
            Email email = Email.of("USERNAME@GMAIL.COM");
            assertThat(email.value()).isEqualTo("username@gmail.com");
        }

        @Test
        @DisplayName("邮箱自动去除首尾空格")
        void constructor_withSpaces_trimsAndLowerCase() {
            Email email = new Email("  user@test.com  ");
            assertThat(email.value()).isEqualTo("user@test.com");
        }
    }

    @Nested
    @DisplayName("构造函数 - 非法输入")
    class ConstructorInvalidTests {

        @Test
        @DisplayName("空字符串输入抛出 BusinessException")
        void constructor_withEmpty_throws() {
            assertThatThrownBy(() -> new Email(""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邮箱地址不能为空");
        }

        @Test
        @DisplayName("仅空格输入抛出 BusinessException")
        void constructor_withSpacesOnly_throws() {
            assertThatThrownBy(() -> new Email("   "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邮箱地址不能为空");
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "notanemail", "missing@domain", "@nodomain.com",
            "spaces in@email.com", "中文邮箱@qq.com",
            "a@b.c", "test@.com", "test@domain."
        })
        @DisplayName("无效邮箱格式抛出 BusinessException")
        void constructor_withInvalidEmail_throws(String invalidEmail) {
            assertThatThrownBy(() -> new Email(invalidEmail))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邮箱格式不正确");
        }

        @Test
        @DisplayName("null 输入抛出 BusinessException")
        void constructor_withNull_throws() {
            assertThatThrownBy(() -> new Email(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邮箱地址不能为空");
        }
    }

    @Nested
    @DisplayName("equals 和 hashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("相同邮箱值相等")
        void equals_sameValue_returnsTrue() {
            Email email1 = Email.of("user@test.com");
            Email email2 = Email.of("user@test.com");
            assertThat(email1).isEqualTo(email2);
        }

        @Test
        @DisplayName("不同邮箱值不相等")
        void equals_differentValue_returnsFalse() {
            Email email1 = Email.of("user1@test.com");
            Email email2 = Email.of("user2@test.com");
            assertThat(email1).isNotEqualTo(email2);
        }

        @Test
        @DisplayName("equals 与自身返回 true")
        void equals_sameInstance_returnsTrue() {
            Email email = Email.of("user@test.com");
            assertThat(email).isEqualTo(email);
        }

        @Test
        @DisplayName("equals 与 null 返回 false")
        void equals_withNull_returnsFalse() {
            Email email = Email.of("user@test.com");
            assertThat(email).isNotEqualTo(null);
        }

        @Test
        @DisplayName("相同邮箱值的 hashCode 相等")
        void hashCode_sameValue_returnsSameHash() {
            Email email1 = Email.of("user@test.com");
            Email email2 = Email.of("user@test.com");
            assertThat(email1.hashCode()).isEqualTo(email2.hashCode());
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("toString 返回邮箱值")
        void toString_returnsEmailValue() {
            Email email = Email.of("USER@TEST.COM");
            assertThat(email.toString()).isEqualTo("user@test.com");
        }
    }
}