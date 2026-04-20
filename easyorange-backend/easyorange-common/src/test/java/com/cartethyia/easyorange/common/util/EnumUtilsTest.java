package com.cartethyia.easyorange.common.util;

import com.cartethyia.easyorange.common.enums.BusinessType;
import com.cartethyia.easyorange.common.enums.IResultCode;
import com.cartethyia.easyorange.common.enums.ResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EnumUtils 单元测试
 */
@DisplayName("EnumUtils 单元测试")
class EnumUtilsTest {

    @Nested
    @DisplayName("fromCode")
    class FromCodeTests {

        @Test
        @DisplayName("找到匹配的枚举")
        void fromCode_found_returnsEnum() {
            BusinessType result = EnumUtils.fromCode(1, BusinessType.values(), BusinessType::getCode);
            assertEquals(BusinessType.ADD, result);
        }

        @Test
        @DisplayName("找不到返回 null")
        void fromCode_notFound_returnsNull() {
            BusinessType result = EnumUtils.fromCode(99, BusinessType.values(), BusinessType::getCode);
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("fromCodeSafe")
    class FromCodeSafeTests {

        @Test
        @DisplayName("找到匹配的枚举")
        void fromCodeSafe_found_returnsPresent() {
            Optional<BusinessType> result = EnumUtils.fromCodeSafe(2, BusinessType.values(), BusinessType::getCode);
            assertTrue(result.isPresent());
            assertEquals(BusinessType.UPDATE, result.get());
        }

        @Test
        @DisplayName("找不到返回 empty")
        void fromCodeSafe_notFound_returnsEmpty() {
            Optional<BusinessType> result = EnumUtils.fromCodeSafe(99, BusinessType.values(), BusinessType::getCode);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("fromResultCode")
    class FromResultCodeTests {

        @Test
        @DisplayName("找到匹配的错误码")
        void fromResultCode_found_returnsEnum() {
            ResultCode result = EnumUtils.fromResultCode("A0000", ResultCode.values());
            assertEquals(ResultCode.SUCCESS, result);
        }

        @Test
        @DisplayName("找不到返回 null")
        void fromResultCode_notFound_returnsNull() {
            ResultCode result = EnumUtils.fromResultCode("X9999", ResultCode.values());
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("fromResultCodeSafe")
    class FromResultCodeSafeTests {

        @Test
        @DisplayName("找到匹配的错误码")
        void fromResultCodeSafe_found_returnsPresent() {
            Optional<ResultCode> result = EnumUtils.fromResultCodeSafe("A0401", ResultCode.values());
            assertTrue(result.isPresent());
            assertEquals(ResultCode.UNAUTHORIZED, result.get());
        }

        @Test
        @DisplayName("找不到返回 empty")
        void fromResultCodeSafe_notFound_returnsEmpty() {
            Optional<ResultCode> result = EnumUtils.fromResultCodeSafe("X9999", ResultCode.values());
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("fromName")
    class FromNameTests {

        @Test
        @DisplayName("找到匹配的枚举名称")
        void fromName_found_returnsEnum() {
            BusinessType result = EnumUtils.fromName("ADD", BusinessType.values());
            assertEquals(BusinessType.ADD, result);
        }

        @Test
        @DisplayName("大小写敏感，小写返回 null")
        void fromName_caseSensitive_lowercaseReturnsNull() {
            // 枚举名应严格匹配，不忽略大小写
            assertNull(EnumUtils.fromName("add", BusinessType.values()));
        }

        @Test
        @DisplayName("精确匹配返回枚举")
        void fromName_exactMatch_returnsEnum() {
            assertEquals(BusinessType.ADD, EnumUtils.fromName("ADD", BusinessType.values()));
        }

        @Test
        @DisplayName("null 返回 null")
        void fromName_null_returnsNull() {
            assertNull(EnumUtils.fromName(null, BusinessType.values()));
        }

        @Test
        @DisplayName("空字符串返回 null")
        void fromName_empty_returnsNull() {
            assertNull(EnumUtils.fromName("", BusinessType.values()));
        }
    }
}
