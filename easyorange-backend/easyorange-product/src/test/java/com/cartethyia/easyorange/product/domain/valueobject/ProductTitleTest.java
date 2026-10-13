package com.cartethyia.easyorange.product.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cartethyia.easyorange.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductTitleTest {

    @Test
    @DisplayName("null / 空白名称都应抛异常（必填）")
    void create_withNullOrBlank_shouldThrow() {
        assertThatThrownBy(() -> new ProductTitle(null)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> new ProductTitle("   ")).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("创建时应 trim 值")
    void create_shouldTrimValue() {
        assertThat(new ProductTitle("  机械键盘  ").value()).isEqualTo("机械键盘");
    }

    @Test
    @DisplayName("长度等于上限 200 时放行")
    void create_withMaxLength_shouldPass() {
        assertThat(new ProductTitle("a".repeat(ProductTitle.MAX_LENGTH)).value())
                .hasSize(ProductTitle.MAX_LENGTH);
    }

    @Test
    @DisplayName("长度超过上限 1 个字符时应抛异常")
    void create_withOverMaxLength_shouldThrow() {
        assertThatThrownBy(() -> new ProductTitle("a".repeat(ProductTitle.MAX_LENGTH + 1)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("of 工厂（Jackson @JsonCreator 入口）与构造器等价")
    void of_shouldBuildSameValue() {
        assertThat(ProductTitle.of("显示器").value()).isEqualTo("显示器");
    }
}
