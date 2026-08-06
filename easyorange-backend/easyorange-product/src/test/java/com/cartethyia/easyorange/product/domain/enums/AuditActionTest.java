package com.cartethyia.easyorange.product.domain.enums;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AuditAction 枚举测试")
class AuditActionTest {

    @Test
    @DisplayName("有效的 code 应返回对应的枚举值")
    void fromCode_withValidCode_shouldReturnEnum() {
        assertThat(AuditAction.fromCode("1")).isEqualTo(AuditAction.APPROVED);
        assertThat(AuditAction.fromCode("2")).isEqualTo(AuditAction.REJECTED);
        assertThat(AuditAction.fromCode("3")).isEqualTo(AuditAction.RESUBMIT);
    }

    @Test
    @DisplayName("null code 应抛出 IllegalArgumentException")
    void fromCode_withNullCode_shouldThrow() {
        assertThatThrownBy(() -> AuditAction.fromCode(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("code must not be null");
    }

    @Test
    @DisplayName("无效的 code 应抛出 IllegalArgumentException")
    void fromCode_withInvalidCode_shouldThrow() {
        assertThatThrownBy(() -> AuditAction.fromCode("99"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown");
    }

    @Test
    @DisplayName("有效的 code 应返回对应的中文描述")
    void getDescByCode_withValidCode_shouldReturnDesc() {
        assertThat(AuditAction.getDescByCode("1")).isEqualTo("通过");
        assertThat(AuditAction.getDescByCode("2")).isEqualTo("拒绝");
        assertThat(AuditAction.getDescByCode("3")).isEqualTo("重提交");
    }

    @Test
    @DisplayName("无效的 code 应返回“未知”")
    void getDescByCode_withInvalidCode_shouldReturnUnknown() {
        assertThat(AuditAction.getDescByCode("99")).isEqualTo("未知");
    }
}
