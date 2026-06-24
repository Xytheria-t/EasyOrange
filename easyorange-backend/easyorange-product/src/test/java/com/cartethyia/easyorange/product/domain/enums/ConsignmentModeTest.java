package com.cartethyia.easyorange.product.domain.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConsignmentModeTest {

    @Test
    @DisplayName("fromCode(0) 应返回 MANUAL")
    void fromCode_0_shouldReturnManual() {
        assertThat(ConsignmentMode.fromCode(0)).isEqualTo(ConsignmentMode.MANUAL);
    }

    @Test
    @DisplayName("fromCode(1) 应返回 AI_MANAGED")
    void fromCode_1_shouldReturnAiManaged() {
        assertThat(ConsignmentMode.fromCode(1)).isEqualTo(ConsignmentMode.AI_MANAGED);
    }

    @Test
    @DisplayName("fromCode(null) 应返回 MANUAL（默认兜底）")
    void fromCode_null_shouldReturnManual() {
        assertThat(ConsignmentMode.fromCode(null)).isEqualTo(ConsignmentMode.MANUAL);
    }

    @Test
    @DisplayName("fromCode(99) 应返回 MANUAL（未知值兜底）")
    void fromCode_unknown_shouldReturnManual() {
        assertThat(ConsignmentMode.fromCode(99)).isEqualTo(ConsignmentMode.MANUAL);
    }

    @Test
    @DisplayName("枚举值应包含正确的 code 和 desc")
    void enum_shouldHaveCorrectValues() {
        assertThat(ConsignmentMode.MANUAL.getCode()).isEqualTo(0);
        assertThat(ConsignmentMode.MANUAL.getDesc()).isEqualTo("手动管理");
        assertThat(ConsignmentMode.AI_MANAGED.getCode()).isEqualTo(1);
        assertThat(ConsignmentMode.AI_MANAGED.getDesc()).isEqualTo("AI托管");
    }
}
