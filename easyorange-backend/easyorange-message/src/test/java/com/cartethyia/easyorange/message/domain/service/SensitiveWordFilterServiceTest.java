package com.cartethyia.easyorange.message.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SensitiveWordFilterService 单元测试")
class SensitiveWordFilterServiceTest {

    // 词表由配置注入，测试自带一份而非依赖基线词库内容（基线词库可随时调整）
    private final SensitiveWordFilterService filterService =
            new SensitiveWordFilterService(List.of("诈骗", "刷单", "代开发票"));

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
        @DisplayName("包含敏感词的文本被替换为 ***")
        void filter_containsSensitive_replaced() {
            String result = filterService.filter("这段文本包含诈骗内容需要过滤");
            assertThat(result).isEqualTo("这段文本包含***内容需要过滤");
        }

        @Test
        @DisplayName("多个敏感词都被替换")
        void filter_multipleSensitive_allReplaced() {
            String result = filterService.filter("先说刷单，再提代开发票，最后说诈骗");
            assertThat(result).isEqualTo("先说***，再提***，最后说***");
        }

        @Test
        @DisplayName("大小写不敏感（英文词同样命中）")
        void filter_caseInsensitive() {
            String result = new SensitiveWordFilterService(List.of("spam")).filter("这是 SPAM 广告");
            assertThat(result).isEqualTo("这是 *** 广告");
        }

        @Test
        @DisplayName("前后空格被去除")
        void filter_trimWhitespace() {
            String result = filterService.filter("  文本内容  ");
            assertThat(result).isEqualTo("文本内容");
        }
    }

    @Nested
    @DisplayName("词表边界")
    class WordListTests {

        @Test
        @DisplayName("空词表不做任何替换（不抛错，也不隐式兜底词）")
        void filter_emptyWordList_noReplacement() {
            String result = new SensitiveWordFilterService(List.of()).filter("包含诈骗内容");
            assertThat(result).isEqualTo("包含诈骗内容");
        }

        @Test
        @DisplayName("null 词表按空词表处理，空白词被忽略")
        void filter_nullWordList_tolerated() {
            SensitiveWordFilterService noWords = new SensitiveWordFilterService(null);
            assertThat(noWords.filter("包含诈骗内容")).isEqualTo("包含诈骗内容");

            SensitiveWordFilterService withBlank = new SensitiveWordFilterService(Arrays.asList(" ", null, "诈骗"));
            assertThat(withBlank.filter("诈骗")).isEqualTo("***");
        }
    }
}
