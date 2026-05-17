package com.cartethyia.easyorange.message.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SensitiveWordFilterService 单元测试")
class SensitiveWordFilterServiceTest {

    private final SensitiveWordFilterService filterService = new SensitiveWordFilterService();

    @Nested
    @DisplayName("filter")
    class FilterTests {

        @Test
        @DisplayName("null 内容返回 null")
        void filter_null_returnsNull() {
            String result = filterService.filter(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("空字符串返回空字符串")
        void filter_empty_returnsEmpty() {
            String result = filterService.filter("");
            assertThat(result).isEqualTo("");
        }

        @Test
        @DisplayName("空白字符串原样返回（isBlank 短路）")
        void filter_blank_returnsAsIs() {
            String result = filterService.filter("   ");
            assertThat(result).isEqualTo("   ");
        }

        @Test
        @DisplayName("正常文本不被过滤")
        void filter_normalText_unchanged() {
            String result = filterService.filter("这是一段正常的文本内容");
            assertThat(result).isEqualTo("这是一段正常的文本内容");
        }

        @Test
        @DisplayName("包含敏感词的文本被替换")
        void filter_containsSensitive_replaced() {
            String result = filterService.filter("这段文本包含敏感词示例需要过滤");
            assertThat(result).isEqualTo("这段文本包含***需要过滤");
        }

        @Test
        @DisplayName("多个敏感词都被替换")
        void filter_multipleSensitive_allReplaced() {
            // The current SENSITIVE_WORDS set only contains "敏感词示例"
            // This test verifies the mechanism works for future additions
            String result = filterService.filter("敏感词示例这是一段敏感词示例文本");
            assertThat(result).isEqualTo("***这是一段***文本");
        }

        @Test
        @DisplayName("前后空格被去除")
        void filter_trimWhitespace() {
            String result = filterService.filter("  文本内容  ");
            assertThat(result).isEqualTo("文本内容");
        }
    }

    @Nested
    @DisplayName("containsSensitive")
    class ContainsSensitiveTests {

        @Test
        @DisplayName("null 内容返回 false")
        void containsSensitive_null_returnsFalse() {
            assertThat(filterService.containsSensitive(null)).isFalse();
        }

        @Test
        @DisplayName("空白内容返回 false")
        void containsSensitive_blank_returnsFalse() {
            assertThat(filterService.containsSensitive("  ")).isFalse();
        }

        @Test
        @DisplayName("正常文本返回 false")
        void containsSensitive_normalText_returnsFalse() {
            assertThat(filterService.containsSensitive("正常文本")).isFalse();
        }

        @Test
        @DisplayName("包含敏感词返回 true")
        void containsSensitive_containsSensitive_returnsTrue() {
            assertThat(filterService.containsSensitive("这是敏感词示例")).isTrue();
        }
    }
}
