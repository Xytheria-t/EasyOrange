package com.cartethyia.easyorange.product.domain.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConditionLevelTest {

    @Test
    @DisplayName("四个成色档位按 code 往返")
    void fromCode_shouldRoundTrip() {
        assertThat(ConditionLevel.fromCode("1")).isEqualTo(ConditionLevel.NEW);
        assertThat(ConditionLevel.fromCode("2")).isEqualTo(ConditionLevel.LIKE_NEW);
        assertThat(ConditionLevel.fromCode("3")).isEqualTo(ConditionLevel.GOOD);
        assertThat(ConditionLevel.fromCode("4")).isEqualTo(ConditionLevel.FAIR);
    }

    @Test
    @DisplayName("无效 / null code 抛 IllegalArgumentException（禁裸字符串进库）")
    void fromCode_withInvalid_shouldThrow() {
        assertThatThrownBy(() -> ConditionLevel.fromCode("9")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConditionLevel.fromCode(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
