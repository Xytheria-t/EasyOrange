package com.cartethyia.easyorange.product.domain.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ProductReportStatus 枚举测试")
class ProductReportStatusTest {

    // ==================== fromCode ====================

    @Test
    @DisplayName("有效的 code 应返回对应的枚举值")
    void fromCode_withValidCode_shouldReturnEnum() {
        assertThat(ProductReportStatus.fromCode("0")).isEqualTo(ProductReportStatus.PENDING);
        assertThat(ProductReportStatus.fromCode("1")).isEqualTo(ProductReportStatus.PROCESSING);
        assertThat(ProductReportStatus.fromCode("2")).isEqualTo(ProductReportStatus.RESOLVED);
        assertThat(ProductReportStatus.fromCode("3")).isEqualTo(ProductReportStatus.DISMISSED);
    }

    @Test
    @DisplayName("无效的 code 应抛出 IllegalArgumentException")
    void fromCode_withInvalidCode_shouldThrow() {
        assertThatThrownBy(() -> ProductReportStatus.fromCode("99"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown");
    }

    // ==================== isPending / isProcessing ====================

    @Test
    @DisplayName("PENDING 状态应返回 isPending = true")
    void isPending_whenPending_shouldReturnTrue() {
        assertThat(ProductReportStatus.PENDING.isPending()).isTrue();
    }

    @Test
    @DisplayName("PROCESSING 状态应返回 isProcessing = true")
    void isProcessing_whenProcessing_shouldReturnTrue() {
        assertThat(ProductReportStatus.PROCESSING.isProcessing()).isTrue();
    }

    // ==================== canProcess ====================

    @Test
    @DisplayName("PENDING 状态的举报可以处理")
    void canProcess_whenPending_shouldReturnTrue() {
        assertThat(ProductReportStatus.PENDING.canProcess()).isTrue();
    }

    @Test
    @DisplayName("PROCESSING 状态的举报不能再次处理")
    void canProcess_whenProcessing_shouldReturnFalse() {
        assertThat(ProductReportStatus.PROCESSING.canProcess()).isFalse();
    }

    @Test
    @DisplayName("RESOLVED 状态的举报不能处理")
    void canProcess_whenResolved_shouldReturnFalse() {
        assertThat(ProductReportStatus.RESOLVED.canProcess()).isFalse();
    }

    @Test
    @DisplayName("DISMISSED 状态的举报不能处理")
    void canProcess_whenDismissed_shouldReturnFalse() {
        assertThat(ProductReportStatus.DISMISSED.canProcess()).isFalse();
    }

    // ==================== canResolve ====================

    @Test
    @DisplayName("PROCESSING 状态的举报可以解决")
    void canResolve_whenProcessing_shouldReturnTrue() {
        assertThat(ProductReportStatus.PROCESSING.canResolve()).isTrue();
    }

    @Test
    @DisplayName("PENDING 状态的举报不能直接解决")
    void canResolve_whenPending_shouldReturnFalse() {
        assertThat(ProductReportStatus.PENDING.canResolve()).isFalse();
    }

    // ==================== canDismiss ====================

    @Test
    @DisplayName("PENDING 状态的举报可以驳回")
    void canDismiss_whenPending_shouldReturnTrue() {
        assertThat(ProductReportStatus.PENDING.canDismiss()).isTrue();
    }

    @Test
    @DisplayName("PROCESSING 状态的举报不能驳回")
    void canDismiss_whenProcessing_shouldReturnFalse() {
        assertThat(ProductReportStatus.PROCESSING.canDismiss()).isFalse();
    }
}
