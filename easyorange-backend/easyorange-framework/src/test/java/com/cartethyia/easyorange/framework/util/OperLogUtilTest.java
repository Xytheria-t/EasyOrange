package com.cartethyia.easyorange.framework.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OperLogUtil Tests")
class OperLogUtilTest {

    @Nested
    @DisplayName("truncate")
    class TruncateTests {

        @Test
        @DisplayName("truncate with null input should return null")
        void truncate_withNull_shouldReturnNull() {
            String result = OperLogUtil.truncate(null, 10);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("truncate with short string should return the original string")
        void truncate_withShortString_shouldReturnOriginal() {
            String input = "hello";

            String result = OperLogUtil.truncate(input, 10);

            assertThat(result).isEqualTo("hello");
        }

        @Test
        @DisplayName("truncate with exact length string should return the original string")
        void truncate_withExactLength_shouldReturnOriginal() {
            String input = "1234567890";

            String result = OperLogUtil.truncate(input, 10);

            assertThat(result).isEqualTo("1234567890");
        }

        @Test
        @DisplayName("truncate with long string should append suffix")
        void truncate_withLongString_shouldAppendSuffix() {
            String input = "This is a very long string that exceeds the max length";

            String result = OperLogUtil.truncate(input, 20);

            assertThat(result).isEqualTo("This is a ve...(已截断)");
        }

        @Test
        @DisplayName("truncate with long string when suffix exceeds maxLength should truncate without suffix")
        void truncate_whenSuffixExceedsMaxLength_shouldTruncateWithoutSuffix() {
            String input = "This is a very long string that exceeds the max length";

            String result = OperLogUtil.truncate(input, 3);

            assertThat(result).isEqualTo("Thi");
        }

        @Test
        @DisplayName("truncate with maxLength 0 should return empty string for long input")
        void truncate_withZeroMaxLength_shouldReturnEmpty() {
            String result = OperLogUtil.truncate("hello", 0);

            assertThat(result).isEqualTo("");
        }

        @Test
        @DisplayName("truncate with negative maxLength should throw")
        void truncate_withNegativeMaxLength_shouldThrow() {
            assertThatThrownBy(() -> OperLogUtil.truncate("hello", -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("maxLength must be non-negative");
        }

        @Test
        @DisplayName("truncate with null and negative maxLength should return null (null check before maxLength)")
        void truncate_withNullAndNegativeMaxLength_shouldReturnNull() {
            String result = OperLogUtil.truncate(null, -1);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("deriveModuleName")
    class DeriveModuleNameTests {

        @Test
        @DisplayName("deriveModuleName should map ProductController to 商品管理")
        void deriveModuleName_withProductController_shouldMap() {
            String result = OperLogUtil.deriveModuleName("ProductController");

            assertThat(result).isEqualTo("商品管理");
        }

        @Test
        @DisplayName("deriveModuleName should map UserController to 用户管理")
        void deriveModuleName_withUserController_shouldMap() {
            String result = OperLogUtil.deriveModuleName("UserController");

            assertThat(result).isEqualTo("用户管理");
        }

        @Test
        @DisplayName("deriveModuleName should handle unknown controller")
        void deriveModuleName_withUnknownController_shouldReturnClassName() {
            String result = OperLogUtil.deriveModuleName("SomeController");

            assertThat(result).isEqualTo("Some");
        }

        @Test
        @DisplayName("deriveModuleName should handle empty input")
        void deriveModuleName_withEmptyInput_shouldReturnEmpty() {
            String result = OperLogUtil.deriveModuleName("");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("deriveModuleName should handle null input")
        void deriveModuleName_withNullInput_shouldReturnNull() {
            String result = OperLogUtil.deriveModuleName(null);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("deriveOperationTitle")
    class DeriveOperationTitleTests {

        @Test
        @DisplayName("deriveOperationTitle should map create to 创建")
        void deriveOperationTitle_withCreate_shouldMap() {
            String result = OperLogUtil.deriveOperationTitle("createProduct");

            assertThat(result).isEqualTo("创建");
        }

        @Test
        @DisplayName("deriveOperationTitle should map delete to 删除")
        void deriveOperationTitle_withDelete_shouldMap() {
            String result = OperLogUtil.deriveOperationTitle("deleteUser");

            assertThat(result).isEqualTo("删除");
        }

        @Test
        @DisplayName("deriveOperationTitle should return original method name when no mapping")
        void deriveOperationTitle_withUnknown_shouldReturnOriginal() {
            String result = OperLogUtil.deriveOperationTitle("customAction");

            assertThat(result).isEqualTo("customAction");
        }

        @Test
        @DisplayName("deriveOperationTitle should handle null input")
        void deriveOperationTitle_withNull_shouldReturnNull() {
            String result = OperLogUtil.deriveOperationTitle(null);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("deriveOperationTitle should handle empty input")
        void deriveOperationTitle_withEmpty_shouldReturnEmpty() {
            String result = OperLogUtil.deriveOperationTitle("");

            assertThat(result).isEmpty();
        }
    }
}
