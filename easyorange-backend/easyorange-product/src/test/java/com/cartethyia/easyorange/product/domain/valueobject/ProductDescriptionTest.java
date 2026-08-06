package com.cartethyia.easyorange.product.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cartethyia.easyorange.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductDescriptionTest {

    @Test
    @DisplayName("传入 null 值时 value 应为 null")
    void create_withNullValue_shouldSetNull() {
        var desc = new ProductDescription(null);

        assertThat(desc.value()).isNull();
    }

    @Test
    @DisplayName("传入空白值时 value 应为 null")
    void create_withBlankValue_shouldSetNull() {
        var desc = new ProductDescription("  ");

        assertThat(desc.value()).isNull();
    }

    @Test
    @DisplayName("创建时应 trim 值")
    void create_shouldTrimValue() {
        var desc = new ProductDescription("  hello  ");

        assertThat(desc.value()).isEqualTo("hello");
    }

    @Test
    @DisplayName("传入超过最大长度的值时应抛出异常")
    void create_withTooLongValue_shouldThrow() {
        var longStr = "a".repeat(5001);

        assertThatThrownBy(() -> new ProductDescription(longStr)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("value 为 null 时 isBlank 应返回 true")
    void isBlank_whenNull_shouldReturnTrue() {
        var desc = new ProductDescription(null);

        assertThat(desc.isBlank()).isTrue();
    }

    @Test
    @DisplayName("value 有效时 isBlank 应返回 false")
    void isBlank_whenValid_shouldReturnFalse() {
        var desc = ProductDescription.of("有效描述");

        assertThat(desc.isBlank()).isFalse();
    }

    @Test
    @DisplayName("value 为 null 时 isPresent 应返回 false")
    void isPresent_whenNull_shouldReturnFalse() {
        var desc = new ProductDescription(null);

        assertThat(desc.isPresent()).isFalse();
    }

    @Test
    @DisplayName("value 有效时 isPresent 应返回 true")
    void isPresent_whenValid_shouldReturnTrue() {
        var desc = ProductDescription.of("有效描述");

        assertThat(desc.isPresent()).isTrue();
    }
}
