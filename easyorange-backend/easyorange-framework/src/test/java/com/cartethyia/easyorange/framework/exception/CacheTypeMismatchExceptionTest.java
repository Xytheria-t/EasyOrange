package com.cartethyia.easyorange.framework.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CacheTypeMismatchException Tests")
class CacheTypeMismatchExceptionTest {

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("should set message with key, expected and actual types")
        void constructor_shouldSetMessage() {
            var ex = new CacheTypeMismatchException("myKey", String.class, Integer.class);

            assertThat(ex.getMessage())
                    .contains("myKey")
                    .contains(String.class.getName())
                    .contains(Integer.class.getName())
                    .contains("缓存类型不匹配");
        }

        @Test
        @DisplayName("should store expectedType")
        void constructor_shouldStoreExpectedType() {
            var ex = new CacheTypeMismatchException("k", String.class, Integer.class);

            assertThat(ex.getExpectedType()).isEqualTo(String.class);
        }

        @Test
        @DisplayName("should store actualType")
        void constructor_shouldStoreActualType() {
            var ex = new CacheTypeMismatchException("k", String.class, Integer.class);

            assertThat(ex.getActualType()).isEqualTo(Integer.class);
        }

        @Test
        @DisplayName("should store key")
        void constructor_shouldStoreKey() {
            var ex = new CacheTypeMismatchException("myKey", String.class, Integer.class);

            assertThat(ex.getKey()).isEqualTo("myKey");
        }

        @Test
        @DisplayName("should be a RuntimeException")
        void constructor_shouldBeRuntimeException() {
            var ex = new CacheTypeMismatchException("k", String.class, Integer.class);

            assertThat(ex).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("should handle null key")
        void constructor_withNullKey_shouldHandleNull() {
            var ex = new CacheTypeMismatchException(null, String.class, Integer.class);

            assertThat(ex.getKey()).isNull();
            assertThat(ex.getMessage()).contains("null");
        }

        @Test
        @DisplayName("should throw NPE when expectedType is null")
        void constructor_withNullExpectedType_shouldThrow() {
            assertThatThrownBy(() -> new CacheTypeMismatchException("k", null, Integer.class))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
