package com.cartethyia.easyorange.product.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReviewContentTest {

    @Test
    @DisplayName("传入 null 值时不应抛出异常")
    void create_withNullValue_shouldBeOk() {
        var review = new ReviewContent(null);

        assertThat(review.value()).isNull();
    }

    @Test
    @DisplayName("传入空白值时应为空字符串")
    void create_withBlankValue_shouldBeEmptyString() {
        var review = new ReviewContent("");

        assertThat(review.value()).isEqualTo("");
    }

    @Test
    @DisplayName("传入超过最大长度的值时应抛出异常")
    void create_withTooLongValue_shouldThrow() {
        var longStr = "a".repeat(2001);

        assertThatThrownBy(() -> new ReviewContent(longStr)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("of 应创建 ReviewContent")
    void of_shouldCreateReviewContent() {
        var review = ReviewContent.of("good");

        assertThat(review.value()).isEqualTo("good");
    }

    @Test
    @DisplayName("value 为 null 时 isBlank 应返回 true")
    void isBlank_whenNull_shouldReturnTrue() {
        var review = new ReviewContent(null);

        assertThat(review.isBlank()).isTrue();
    }
}
