package com.cartethyia.easyorange.product.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cartethyia.easyorange.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImageUrlTest {

    @Test
    @DisplayName("null / 空白 / 非 http(s) 协议都应抛异常")
    void create_withInvalidUrl_shouldThrow() {
        assertThatThrownBy(() -> new ImageUrl(null)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> new ImageUrl("   ")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> new ImageUrl("ftp://img/1.jpg")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> new ImageUrl("img/1.jpg")).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("http / https 放行，创建时 trim")
    void create_withHttpOrHttps_shouldPassAndTrim() {
        assertThat(new ImageUrl("  https://img/1.jpg  ").value()).isEqualTo("https://img/1.jpg");
        assertThat(new ImageUrl("http://img/1.jpg").value()).isEqualTo("http://img/1.jpg");
    }

    @Test
    @DisplayName("of 工厂与构造器等价")
    void of_shouldBuildSameValue() {
        assertThat(ImageUrl.of("https://img/2.jpg").value()).isEqualTo("https://img/2.jpg");
    }
}
