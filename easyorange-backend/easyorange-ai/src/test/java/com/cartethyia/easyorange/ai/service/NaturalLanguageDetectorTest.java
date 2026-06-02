package com.cartethyia.easyorange.ai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NaturalLanguageDetector 测试")
class NaturalLanguageDetectorTest {

    private NaturalLanguageDetector detector;

    @BeforeEach
    void setUp() {
        detector = new NaturalLanguageDetector();
    }

    @Nested
    @DisplayName("自然语言 — 返回 true")
    class NaturalLanguageCases {

        @ParameterizedTest(name = "[{index}] \"{0}\" 是自然语言")
        @ValueSource(strings = {
            "找一台5000以内的笔记本",
            "推荐一款适合编程的电脑",
            "可以帮我看看有什么好的手机吗",
            "预算3000左右买个二手相机",
            "哪个牌子的耳机比较好用",
            "怎么选一个好的机械键盘",
            "有什么好手机推荐吗"
        })
        void isNaturalLanguage_true(String keyword) {
            assertThat(detector.isNaturalLanguage(keyword)).isTrue();
        }
    }

    @Nested
    @DisplayName("非自然语言 — 返回 false")
    class NotNaturalLanguageCases {

        @Test
        @DisplayName("null 输入")
        void isNaturalLanguage_null() {
            assertThat(detector.isNaturalLanguage(null)).isFalse();
        }

        @ParameterizedTest(name = "[{index}] 空白输入: \"{0}\"")
        @NullAndEmptySource
        void isNaturalLanguage_blank(String input) {
            assertThat(detector.isNaturalLanguage(input)).isFalse();
        }

        @ParameterizedTest(name = "[{index}] \"{0}\" 不是自然语言")
        @ValueSource(strings = {
            "MacBook Pro",
            "iPhone 14",
            "笔记本",
            "手机",
            "abc",
            "12345"
        })
        void isNaturalLanguage_tooShortOrKeywordOnly(String keyword) {
            assertThat(detector.isNaturalLanguage(keyword)).isFalse();
        }

        @Test
        @DisplayName("刚好4个字符 — 不满足最小长度")
        void isNaturalLanguage_exactlyFourChars() {
            assertThat(detector.isNaturalLanguage("找电脑")).isFalse();
        }

        @Test
        @DisplayName("刚好5个字符且含意图词 — 满足")
        void isNaturalLanguage_exactlyFiveCharsWithIntentWord() {
            assertThat(detector.isNaturalLanguage("找好电脑")).isFalse();
            assertThat(detector.isNaturalLanguage("找好电脑啊")).isTrue();
        }
    }

    @Nested
    @DisplayName("边界值")
    class EdgeCases {

        @ParameterizedTest(name = "[{index}] 长度={0}, 含意图词={1}, 结果={2}")
        @CsvSource({
            "5, true, true",
            "5, false, false",
            "4, true, false",
            "100, true, true"
        })
        void isNaturalLanguage_boundary(int length, boolean hasIntentWord, boolean expected) {
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                sb.append("字");
            }
            String keyword = sb.toString();
            if (hasIntentWord && length >= 5) {
                keyword = "找" + sb.substring(1);
            }
            assertThat(detector.isNaturalLanguage(keyword)).isEqualTo(expected);
        }
    }
}
