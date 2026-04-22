package com.cartethyia.easyorange.user.domain.valueobject;

import com.cartethyia.easyorange.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Phone 值对象测试")
class PhoneValueObjectTest {

    @Nested
    @DisplayName("构造函数 - 正常场景")
    class ConstructorValidTests {

        @Test
        @DisplayName("使用构造函数创建有效手机号")
        void constructor_withValidPhone_createsPhone() {
            Phone phone = new Phone("13800138000");
            assertThat(phone.value()).isEqualTo("13800138000");
        }

        @Test
        @DisplayName("使用 of 静态工厂方法创建手机号")
        void of_withValidPhone_createsPhone() {
            Phone phone = Phone.of("13912345678");
            assertThat(phone.value()).isEqualTo("13912345678");
        }

        @Test
        @DisplayName("手机号自动去除首尾空格")
        void constructor_withSpaces_trimsSpaces() {
            Phone phone = new Phone("  13800138000  ");
            assertThat(phone.value()).isEqualTo("13800138000");
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "13800138000", "13912345678", "15912345678",
            "18612345678", "19912345678", "16712345678"
        })
        @DisplayName("各种有效手机号格式创建成功")
        void constructor_withVariousValidPhones_createsPhone(String validPhone) {
            Phone phone = new Phone(validPhone);
            assertThat(phone.value()).isEqualTo(validPhone);
        }
    }

    @Nested
    @DisplayName("构造函数 - 非法输入")
    class ConstructorInvalidTests {

        @Test
        @DisplayName("空字符串抛出 BusinessException")
        void constructor_withEmpty_throws() {
            assertThatThrownBy(() -> new Phone(""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("手机号不能为空");
        }

        @Test
        @DisplayName("仅空格抛出 BusinessException")
        void constructor_withSpaces_throws() {
            assertThatThrownBy(() -> new Phone("   "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("手机号不能为空");
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "1234567890", "abc12345678", "138-0013-8000", "138 0013 8000"
        })
        @DisplayName("无效手机号格式抛出 BusinessException")
        void constructor_withInvalidPhone_throws(String invalidPhone) {
            assertThatThrownBy(() -> new Phone(invalidPhone))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("手机号格式不正确");
        }

        @Test
        @DisplayName("null 输入抛出 BusinessException")
        void constructor_withNull_throws() {
            assertThatThrownBy(() -> new Phone(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("手机号不能为空");
        }
    }

    @Nested
    @DisplayName("equals 和 hashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("相同手机号值相等")
        void equals_sameValue_returnsTrue() {
            Phone phone1 = Phone.of("13800138000");
            Phone phone2 = Phone.of("13800138000");
            assertThat(phone1).isEqualTo(phone2);
        }

        @Test
        @DisplayName("不同手机号值不相等")
        void equals_differentValue_returnsFalse() {
            Phone phone1 = Phone.of("13800138000");
            Phone phone2 = Phone.of("13900138000");
            assertThat(phone1).isNotEqualTo(phone2);
        }

        @Test
        @DisplayName("equals 与自身返回 true")
        void equals_sameInstance_returnsTrue() {
            Phone phone = Phone.of("13800138000");
            assertThat(phone).isEqualTo(phone);
        }

        @Test
        @DisplayName("equals 与 null 返回 false")
        void equals_withNull_returnsFalse() {
            Phone phone = Phone.of("13800138000");
            assertThat(phone).isNotEqualTo(null);
        }

        @Test
        @DisplayName("相同手机号值的 hashCode 相等")
        void hashCode_sameValue_returnsSameHash() {
            Phone phone1 = Phone.of("13800138000");
            Phone phone2 = Phone.of("13800138000");
            assertThat(phone1.hashCode()).isEqualTo(phone2.hashCode());
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("toString 返回手机号值")
        void toString_returnsPhoneValue() {
            Phone phone = Phone.of("13800138000");
            assertThat(phone.toString()).isEqualTo("13800138000");
        }
    }
}