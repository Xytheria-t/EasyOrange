package com.cartethyia.easyorange.user.domain.valueobject;

import com.cartethyia.easyorange.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserId 值对象测试")
class UserIdValueObjectTest {

    @Nested
    @DisplayName("构造函数 - 正常场景")
    class ConstructorValidTests {

        @Test
        @DisplayName("使用构造函数创建有效的正数 UserId")
        void constructor_withPositiveLong_createsUserId() {
            UserId userId = new UserId(1L);
            assertThat(userId.value()).isEqualTo(1L);
        }

        @Test
        @DisplayName("使用 of 静态工厂方法创建 UserId")
        void of_withPositiveLong_createsUserId() {
            UserId userId = UserId.of(123456L);
            assertThat(userId.value()).isEqualTo(123456L);
        }

        @Test
        @DisplayName("较大的正数 UserId 创建成功")
        void constructor_withLargePositive_createsUserId() {
            UserId userId = new UserId(Long.MAX_VALUE);
            assertThat(userId.value()).isEqualTo(Long.MAX_VALUE);
        }
    }

    @Nested
    @DisplayName("构造函数 - 非法输入")
    class ConstructorInvalidTests {

        @Test
        @DisplayName("零值输入抛出 BusinessException")
        void constructor_withZero_throws() {
            assertThatThrownBy(() -> new UserId(0L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户ID必须为正数");
        }

        @Test
        @DisplayName("负数输入抛出 BusinessException")
        void constructor_withNegative_throws() {
            assertThatThrownBy(() -> new UserId(-1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户ID必须为正数");
        }

        @ParameterizedTest
        @ValueSource(longs = { -100, -1, Long.MIN_VALUE })
        @DisplayName("各种负数输入都抛出 BusinessException")
        void constructor_withVariousNegative_throws(long negativeValue) {
            assertThatThrownBy(() -> new UserId(negativeValue))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户ID必须为正数");
        }

        @Test
        @DisplayName("null 输入抛出 BusinessException")
        void constructor_withNull_throws() {
            assertThatThrownBy(() -> new UserId(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户ID不能为空");
        }
    }

    @Nested
    @DisplayName("equals 和 hashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("相同 UserId 值相等")
        void equals_sameValue_returnsTrue() {
            UserId userId1 = UserId.of(1L);
            UserId userId2 = UserId.of(1L);
            assertThat(userId1).isEqualTo(userId2);
        }

        @Test
        @DisplayName("不同 UserId 值不相等")
        void equals_differentValue_returnsFalse() {
            UserId userId1 = UserId.of(1L);
            UserId userId2 = UserId.of(2L);
            assertThat(userId1).isNotEqualTo(userId2);
        }

        @Test
        @DisplayName("equals 与自身返回 true")
        void equals_sameInstance_returnsTrue() {
            UserId userId = UserId.of(1L);
            assertThat(userId).isEqualTo(userId);
        }

        @Test
        @DisplayName("equals 与 null 返回 false")
        void equals_withNull_returnsFalse() {
            UserId userId = UserId.of(1L);
            assertThat(userId).isNotEqualTo(null);
        }

        @Test
        @DisplayName("相同 UserId 值的 hashCode 相等")
        void hashCode_sameValue_returnsSameHash() {
            UserId userId1 = UserId.of(1L);
            UserId userId2 = UserId.of(1L);
            assertThat(userId1.hashCode()).isEqualTo(userId2.hashCode());
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("toString 返回包含用户ID的字符串")
        void toString_returnsUserIdValue() {
            UserId userId = UserId.of(123L);
            assertThat(userId.toString()).isEqualTo("UserId{123}");
        }
    }
}