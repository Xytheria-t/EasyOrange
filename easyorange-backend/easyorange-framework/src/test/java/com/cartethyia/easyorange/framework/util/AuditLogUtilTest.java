package com.cartethyia.easyorange.framework.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AuditLogUtil Tests")
class AuditLogUtilTest {

    @Nested
    @DisplayName("truncate")
    class TruncateTests {

        @Test
        @DisplayName("truncate with null input should return null")
        void truncate_withNull_shouldReturnNull() {
            String result = AuditLogUtil.truncate(null, 10);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("truncate with short string should return the original string")
        void truncate_withShortString_shouldReturnOriginal() {
            String input = "hello";

            String result = AuditLogUtil.truncate(input, 10);

            assertThat(result).isEqualTo("hello");
        }

        @Test
        @DisplayName("truncate with exact length string should return the original string")
        void truncate_withExactLength_shouldReturnOriginal() {
            String input = "1234567890";

            String result = AuditLogUtil.truncate(input, 10);

            assertThat(result).isEqualTo("1234567890");
        }

        @Test
        @DisplayName("truncate with long string should append suffix")
        void truncate_withLongString_shouldAppendSuffix() {
            String input = "This is a very long string that exceeds the max length";

            String result = AuditLogUtil.truncate(input, 20);

            assertThat(result).isEqualTo("This is a ve...(已截断)");
        }

        @Test
        @DisplayName("truncate with long string when suffix exceeds maxLength should truncate without suffix")
        void truncate_whenSuffixExceedsMaxLength_shouldTruncateWithoutSuffix() {
            String input = "This is a very long string that exceeds the max length";

            String result = AuditLogUtil.truncate(input, 3);

            assertThat(result).isEqualTo("Thi");
        }

        @Test
        @DisplayName("truncate with maxLength 0 should return empty string for long input")
        void truncate_withZeroMaxLength_shouldReturnEmpty() {
            String result = AuditLogUtil.truncate("hello", 0);

            assertThat(result).isEqualTo("");
        }

        @Test
        @DisplayName("truncate with negative maxLength should throw")
        void truncate_withNegativeMaxLength_shouldThrow() {
            assertThatThrownBy(() -> AuditLogUtil.truncate("hello", -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("maxLength must be non-negative");
        }

        @Test
        @DisplayName("truncate with null and negative maxLength should return null (null check before maxLength)")
        void truncate_withNullAndNegativeMaxLength_shouldReturnNull() {
            String result = AuditLogUtil.truncate(null, -1);

            assertThat(result).isNull();
        }
    }
}
