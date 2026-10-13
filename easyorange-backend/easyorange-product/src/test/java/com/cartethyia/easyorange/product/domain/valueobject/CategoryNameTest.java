package com.cartethyia.easyorange.product.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cartethyia.easyorange.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CategoryNameTest {

    @Test
    @DisplayName("null / 空白名称都应抛异常（必填）")
    void create_withNullOrBlank_shouldThrow() {
        assertThatThrownBy(() -> new CategoryName(null)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> new CategoryName("  ")).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("长度等于上限 20 时放行（与 DTO 层 @Size(max = 20) 对齐）")
    void create_withMaxLength_shouldPass() {
        assertThat(new CategoryName("a".repeat(20)).value()).hasSize(20);
    }

    @Test
    @DisplayName("长度超过上限 1 个字符时应抛异常")
    void create_withOverMaxLength_shouldThrow() {
        assertThatThrownBy(() -> new CategoryName("a".repeat(21))).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("of 工厂与构造器等价")
    void of_shouldBuildSameValue() {
        assertThat(CategoryName.of("电子数码").value()).isEqualTo("电子数码");
    }
}
