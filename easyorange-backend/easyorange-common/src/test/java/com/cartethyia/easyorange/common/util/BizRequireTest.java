package com.cartethyia.easyorange.common.util;

import static org.junit.jupiter.api.Assertions.*;

import com.cartethyia.easyorange.common.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * BizRequire 单元测试
 */
@DisplayName("BizRequire 单元测试")
class BizRequireTest {

    @Nested
    @DisplayName("notNull")
    class NotNullTests {

        @Test
        @DisplayName("对象为 null 时抛出异常")
        void notNull_nullObject_throwsException() {
            assertThrows(BusinessException.class, () -> BizRequire.notNull(null, "不能为空"));
        }

        @Test
        @DisplayName("对象不为 null 时不抛出")
        void notNull_nonNullObject_noException() {
            assertDoesNotThrow(() -> BizRequire.notNull(new Object(), "不能为空"));
        }
    }

    @Nested
    @DisplayName("requireTrue")
    class RequireTrueTests {

        @Test
        @DisplayName("条件为 false 时抛出异常")
        void requireTrue_falseCondition_throwsException() {
            assertThrows(BusinessException.class, () -> BizRequire.requireTrue(false, "条件不满足"));
        }

        @Test
        @DisplayName("条件为 true 时不抛出")
        void requireTrue_trueCondition_noException() {
            assertDoesNotThrow(() -> BizRequire.requireTrue(true, "条件不满足"));
        }
    }

    @Nested
    @DisplayName("notBlank")
    class NotBlankTests {

        @Test
        @DisplayName("null 字符串抛出异常")
        void notBlank_null_throwsException() {
            assertThrows(BusinessException.class, () -> BizRequire.notBlank(null, "不能为空"));
        }

        @Test
        @DisplayName("空字符串抛出异常")
        void notBlank_empty_throwsException() {
            assertThrows(BusinessException.class, () -> BizRequire.notBlank("", "不能为空"));
        }

        @Test
        @DisplayName("纯空白字符串抛出异常")
        void notBlank_blank_throwsException() {
            assertThrows(BusinessException.class, () -> BizRequire.notBlank("   ", "不能为空"));
        }

        @Test
        @DisplayName("有效字符串不抛出")
        void notBlank_valid_noException() {
            assertDoesNotThrow(() -> BizRequire.notBlank("hello", "不能为空"));
        }
    }

    @Nested
    @DisplayName("notEmpty")
    class NotEmptyTests {

        @Test
        @DisplayName("null 集合抛出异常")
        void notEmpty_nullCollection_throwsException() {
            assertThrows(BusinessException.class, () -> BizRequire.notEmpty((List<?>) null, "不能为空"));
        }

        @Test
        @DisplayName("空集合抛出异常")
        void notEmpty_emptyCollection_throwsException() {
            assertThrows(BusinessException.class, () -> BizRequire.notEmpty(List.of(), "不能为空"));
        }

        @Test
        @DisplayName("非空集合不抛出")
        void notEmpty_nonEmptyCollection_noException() {
            assertDoesNotThrow(() -> BizRequire.notEmpty(List.of("a"), "不能为空"));
        }
    }
}
