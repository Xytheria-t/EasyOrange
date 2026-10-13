package com.cartethyia.easyorange.product.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cartethyia.easyorange.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ContactMethodTest {

    @Test
    @DisplayName("null 原样通过：联系方式是选填，空值不是在构造期炸掉的理由")
    void create_withNull_shouldPassThrough() {
        assertThat(new ContactMethod(null).value()).isNull();
    }

    @Test
    @DisplayName("创建时应 trim 值")
    void create_shouldTrimValue() {
        assertThat(new ContactMethod("  微信  ").value()).isEqualTo("微信");
    }

    @Test
    @DisplayName("长度等于上限 100 时放行")
    void create_withMaxLength_shouldPass() {
        assertThat(new ContactMethod("a".repeat(ContactMethod.MAX_LENGTH)).value())
                .hasSize(ContactMethod.MAX_LENGTH);
    }

    @Test
    @DisplayName("长度超过上限 1 个字符时应抛异常")
    void create_withOverMaxLength_shouldThrow() {
        assertThatThrownBy(() -> new ContactMethod("a".repeat(ContactMethod.MAX_LENGTH + 1)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("null / 空白（含 trim 后空白）isBlank 为 true")
    void isBlank_whenNullOrBlank_shouldReturnTrue() {
        assertThat(new ContactMethod(null).isBlank()).isTrue();
        assertThat(new ContactMethod("   ").isBlank()).isTrue();
    }

    @Test
    @DisplayName("有值 isBlank 为 false、isNotBlank 为 true")
    void isNotBlank_whenPresent_shouldReturnTrue() {
        var contact = new ContactMethod("微信");

        assertThat(contact.isBlank()).isFalse();
        assertThat(contact.isNotBlank()).isTrue();
    }
}
