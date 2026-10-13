package com.cartethyia.easyorange.product.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cartethyia.easyorange.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TradeLocationTest {

    @Test
    @DisplayName("null 原样通过：交易地点是选填")
    void create_withNull_shouldPassThrough() {
        assertThat(new TradeLocation(null).value()).isNull();
    }

    @Test
    @DisplayName("创建时应 trim 值")
    void create_shouldTrimValue() {
        assertThat(new TradeLocation("  北京海淀  ").value()).isEqualTo("北京海淀");
    }

    @Test
    @DisplayName("长度等于上限 200 时放行")
    void create_withMaxLength_shouldPass() {
        assertThat(new TradeLocation("a".repeat(TradeLocation.MAX_LENGTH)).value())
                .hasSize(TradeLocation.MAX_LENGTH);
    }

    @Test
    @DisplayName("长度超过上限 1 个字符时应抛异常")
    void create_withOverMaxLength_shouldThrow() {
        assertThatThrownBy(() -> new TradeLocation("a".repeat(TradeLocation.MAX_LENGTH + 1)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("of 工厂与构造器等价")
    void of_shouldBuildSameValue() {
        assertThat(TradeLocation.of("上海").value()).isEqualTo("上海");
    }
}
