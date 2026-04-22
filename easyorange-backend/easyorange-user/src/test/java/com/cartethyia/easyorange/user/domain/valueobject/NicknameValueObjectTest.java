package com.cartethyia.easyorange.user.domain.valueobject;

import com.cartethyia.easyorange.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Nickname 值对象测试")
class NicknameValueObjectTest {

    @Nested
    @DisplayName("构造函数 - 正常场景")
    class ConstructorValidTests {

        @Test
        @DisplayName("使用构造函数创建有效昵称")
        void constructor_withValidNickname_createsNickname() {
            Nickname nickname = new Nickname("TestUser");
            assertThat(nickname.value()).isEqualTo("TestUser");
        }

        @Test
        @DisplayName("使用 of 静态工厂方法创建昵称")
        void of_withValidNickname_createsNickname() {
            Nickname nickname = Nickname.of("User123");
            assertThat(nickname.value()).isEqualTo("User123");
        }

        @Test
        @DisplayName("昵称去除首尾空格")
        void constructor_withSpaces_trimsSpaces() {
            Nickname nickname = new Nickname("  TestUser  ");
            assertThat(nickname.value()).isEqualTo("TestUser");
        }

        @Test
        @DisplayName("支持中文昵称")
        void constructor_withChineseNickname_createsNickname() {
            Nickname nickname = new Nickname("测试用户");
            assertThat(nickname.value()).isEqualTo("测试用户");
        }

        @Test
        @DisplayName("最小长度昵称 (1字符)")
        void constructor_minLength_createsNickname() {
            Nickname nickname = new Nickname("A");
            assertThat(nickname.value()).isEqualTo("A");
        }

        @Test
        @DisplayName("最大长度昵称 (30字符)")
        void constructor_maxLength_createsNickname() {
            String maxLength = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234"; // 30 chars
            Nickname nickname = new Nickname(maxLength);
            assertThat(nickname.value()).isEqualTo(maxLength);
        }
    }

    @Nested
    @DisplayName("构造函数 - 非法输入")
    class ConstructorInvalidTests {

        @Test
        @DisplayName("空字符串输入抛出 BusinessException")
        void constructor_withEmpty_throws() {
            assertThatThrownBy(() -> new Nickname(""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("昵称不能为空");
        }

        @Test
        @DisplayName("仅空格输入抛出 BusinessException")
        void constructor_withBlankSpaces_throws() {
            assertThatThrownBy(() -> new Nickname("   "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("昵称不能为空");
        }

        @Test
        @DisplayName("null 输入抛出 BusinessException")
        void constructor_withNull_throws() {
            assertThatThrownBy(() -> new Nickname(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("昵称不能为空");
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ12345", // 31 chars - too long
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456"  // 32 chars - definitely too long
        })
        @DisplayName("超过30字符的昵称抛出 BusinessException")
        void constructor_tooLong_throws(String tooLongNickname) {
            assertThatThrownBy(() -> new Nickname(tooLongNickname))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("昵称长度不能超过30个字符");
        }
    }

    @Nested
    @DisplayName("equals 和 hashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("相同昵称值相等")
        void equals_sameValue_returnsTrue() {
            Nickname nickname1 = Nickname.of("TestUser");
            Nickname nickname2 = Nickname.of("TestUser");
            assertThat(nickname1).isEqualTo(nickname2);
        }

        @Test
        @DisplayName("不同昵称值不相等")
        void equals_differentValue_returnsFalse() {
            Nickname nickname1 = Nickname.of("User1");
            Nickname nickname2 = Nickname.of("User2");
            assertThat(nickname1).isNotEqualTo(nickname2);
        }

        @Test
        @DisplayName("equals 与自身返回 true")
        void equals_sameInstance_returnsTrue() {
            Nickname nickname = Nickname.of("TestUser");
            assertThat(nickname).isEqualTo(nickname);
        }

        @Test
        @DisplayName("equals 与 null 返回 false")
        void equals_withNull_returnsFalse() {
            Nickname nickname = Nickname.of("TestUser");
            assertThat(nickname).isNotEqualTo(null);
        }

        @Test
        @DisplayName("相同昵称值的 hashCode 相等")
        void hashCode_sameValue_returnsSameHash() {
            Nickname nickname1 = Nickname.of("TestUser");
            Nickname nickname2 = Nickname.of("TestUser");
            assertThat(nickname1.hashCode()).isEqualTo(nickname2.hashCode());
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("toString 返回昵称值")
        void toString_returnsNicknameValue() {
            Nickname nickname = Nickname.of("TestUser");
            assertThat(nickname.toString()).isEqualTo("TestUser");
        }
    }
}