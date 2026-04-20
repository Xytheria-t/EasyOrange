package com.cartethyia.easyorange.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MaskUtils 单元测试
 */
@DisplayName("MaskUtils 单元测试")
class MaskUtilsTest {

    @Nested
    @DisplayName("手机号脱敏")
    class MaskPhoneTests {

        @Test
        @DisplayName("正常手机号脱敏")
        void maskPhone_normal() {
            assertEquals("138****5678", MaskUtils.maskPhone("13812345678"));
        }

        @Test
        @DisplayName("null 返回 null")
        void maskPhone_null() {
            assertNull(MaskUtils.maskPhone(null));
        }

        @Test
        @DisplayName("长度不足返回原值")
        void maskPhone_tooShort() {
            assertEquals("12345", MaskUtils.maskPhone("12345"));
        }
    }

    @Nested
    @DisplayName("邮箱脱敏")
    class MaskEmailTests {

        @Test
        @DisplayName("正常邮箱脱敏")
        void maskEmail_normal() {
            assertEquals("u****@example.com", MaskUtils.maskEmail("user@example.com"));
        }

        @Test
        @DisplayName("null 返回 null")
        void maskEmail_null() {
            assertNull(MaskUtils.maskEmail(null));
        }

        @Test
        @DisplayName("不含 @ 返回原值")
        void maskEmail_noAt() {
            assertEquals("invalidemail", MaskUtils.maskEmail("invalidemail"));
        }

        @Test
        @DisplayName("首字符后直接 @")
        void maskEmail_shortLocal() {
            assertEquals("****@example.com", MaskUtils.maskEmail("a@example.com"));
        }
    }

    @Nested
    @DisplayName("身份证脱敏")
    class MaskIdCardTests {

        @Test
        @DisplayName("正常身份证号脱敏")
        void maskIdCard_normal() {
            assertEquals("110101********1234", MaskUtils.maskIdCard("110101199001011234"));
        }

        @Test
        @DisplayName("null 返回 null")
        void maskIdCard_null() {
            assertNull(MaskUtils.maskIdCard(null));
        }

        @Test
        @DisplayName("长度不足返回原值")
        void maskIdCard_tooShort() {
            assertEquals("123456789", MaskUtils.maskIdCard("123456789"));
        }
    }

    @Nested
    @DisplayName("姓名脱敏")
    class MaskNameTests {

        @Test
        @DisplayName("2 个字姓名")
        void maskName_twoChars() {
            assertEquals("张*", MaskUtils.maskName("张三"));
        }

        @Test
        @DisplayName("3 个字姓名")
        void maskName_threeChars() {
            assertEquals("张*丰", MaskUtils.maskName("张三丰"));
        }

        @Test
        @DisplayName("4 个字姓名")
        void maskName_fourChars() {
            assertEquals("欧阳**", MaskUtils.maskName("欧阳娜娜"));
        }

        @Test
        @DisplayName("1 个字姓名")
        void maskName_oneChar() {
            assertEquals("张", MaskUtils.maskName("张"));
        }

        @Test
        @DisplayName("null 返回 null")
        void maskName_null() {
            assertNull(MaskUtils.maskName(null));
        }

        @Test
        @DisplayName("空字符串返回空")
        void maskName_empty() {
            assertEquals("", MaskUtils.maskName(""));
        }
    }

    @Nested
    @DisplayName("银行卡脱敏")
    class MaskBankCardTests {

        @Test
        @DisplayName("正常银行卡号脱敏")
        void maskBankCard_normal() {
            assertEquals("6222****0123", MaskUtils.maskBankCard("6222021234567890123"));
        }

        @Test
        @DisplayName("null 返回 null")
        void maskBankCard_null() {
            assertNull(MaskUtils.maskBankCard(null));
        }

        @Test
        @DisplayName("长度不足返回原值")
        void maskBankCard_tooShort() {
            assertEquals("1234567", MaskUtils.maskBankCard("1234567"));
        }
    }

    @Nested
    @DisplayName("地址脱敏")
    class MaskAddressTests {

        @Test
        @DisplayName("正常地址脱敏")
        void maskAddress_normal() {
            String result = MaskUtils.maskAddress("北京市朝阳区xxx街道");
            assertEquals("北京市朝阳区***", result);
        }

        @Test
        @DisplayName("null 返回 null")
        void maskAddress_null() {
            assertNull(MaskUtils.maskAddress(null));
        }
    }

    @Nested
    @DisplayName("通用脱敏")
    class MaskTests {

        @Test
        @DisplayName("保留前3后4")
        void mask_normal() {
            assertEquals("138****5678", MaskUtils.mask("13812345678", 3, 4));
        }

        @Test
        @DisplayName("长度不足返回原值")
        void mask_tooShort() {
            assertEquals("123", MaskUtils.mask("123", 2, 2));
        }

        @Test
        @DisplayName("null 返回 null")
        void mask_null() {
            assertNull(MaskUtils.mask(null, 2, 2));
        }
    }
}
