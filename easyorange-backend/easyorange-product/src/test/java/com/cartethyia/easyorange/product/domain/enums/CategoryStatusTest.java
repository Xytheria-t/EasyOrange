package com.cartethyia.easyorange.product.domain.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CategoryStatusTest {

    @Test
    @DisplayName("禁用 / 启用按 code 往返")
    void fromCode_shouldRoundTrip() {
        assertThat(CategoryStatus.fromCode("0")).isEqualTo(CategoryStatus.DISABLED);
        assertThat(CategoryStatus.fromCode("1")).isEqualTo(CategoryStatus.ENABLED);
    }

    @Test
    @DisplayName("无效 / null code 抛 IllegalArgumentException")
    void fromCode_withInvalid_shouldThrow() {
        assertThatThrownBy(() -> CategoryStatus.fromCode("2")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CategoryStatus.fromCode(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
