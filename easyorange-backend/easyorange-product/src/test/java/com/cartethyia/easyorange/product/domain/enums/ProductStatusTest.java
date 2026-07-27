package com.cartethyia.easyorange.product.domain.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ProductStatus 状态机与枚举测试")
class ProductStatusTest {

    // ==================== canTransitionTo ====================

    @Test
    @DisplayName("DRAFT → PENDING_REVIEW 是允许的转换")
    void canTransitionTo_draftToPendingReview() {
        assertThat(ProductStatus.DRAFT.canTransitionTo(ProductStatus.PENDING_REVIEW)).isTrue();
    }

    @Test
    @DisplayName("DRAFT → ONLINE 是允许的转换（管理员绕过审核直接上架）")
    void canTransitionTo_draftToOnline() {
        assertThat(ProductStatus.DRAFT.canTransitionTo(ProductStatus.ONLINE)).isTrue();
    }

    @Test
    @DisplayName("DRAFT → SOLD 是不允许的转换")
    void canTransitionTo_draftToSold() {
        assertThat(ProductStatus.DRAFT.canTransitionTo(ProductStatus.SOLD)).isFalse();
    }

    @Test
    @DisplayName("PENDING_REVIEW → ONLINE 是允许的转换（审核通过）")
    void canTransitionTo_pendingReviewToOnline() {
        assertThat(ProductStatus.PENDING_REVIEW.canTransitionTo(ProductStatus.ONLINE)).isTrue();
    }

    @Test
    @DisplayName("PENDING_REVIEW → REJECTED 是允许的转换（审核拒绝）")
    void canTransitionTo_pendingReviewToRejected() {
        assertThat(ProductStatus.PENDING_REVIEW.canTransitionTo(ProductStatus.REJECTED)).isTrue();
    }

    @Test
    @DisplayName("REJECTED → PENDING_REVIEW 是允许的转换（重新提交审核）")
    void canTransitionTo_rejectedToPendingReview() {
        assertThat(ProductStatus.REJECTED.canTransitionTo(ProductStatus.PENDING_REVIEW)).isTrue();
    }

    @Test
    @DisplayName("ONLINE → OFFLINE 是允许的转换（下架）")
    void canTransitionTo_onlineToOffline() {
        assertThat(ProductStatus.ONLINE.canTransitionTo(ProductStatus.OFFLINE)).isTrue();
    }

    @Test
    @DisplayName("ONLINE → SOLD 是允许的转换（标记售出）")
    void canTransitionTo_onlineToSold() {
        assertThat(ProductStatus.ONLINE.canTransitionTo(ProductStatus.SOLD)).isTrue();
    }

    @Test
    @DisplayName("OFFLINE → ONLINE 是允许的转换（重新上架）")
    void canTransitionTo_offlineToOnline() {
        assertThat(ProductStatus.OFFLINE.canTransitionTo(ProductStatus.ONLINE)).isTrue();
    }

    @Test
    @DisplayName("SOLD 是终端状态，不允许任何转换")
    void canTransitionTo_soldToAnything() {
        assertThat(ProductStatus.SOLD.canTransitionTo(ProductStatus.DRAFT)).isFalse();
        assertThat(ProductStatus.SOLD.canTransitionTo(ProductStatus.ONLINE)).isFalse();
        assertThat(ProductStatus.SOLD.canTransitionTo(ProductStatus.OFFLINE)).isFalse();
        assertThat(ProductStatus.SOLD.canTransitionTo(ProductStatus.PENDING_REVIEW)).isFalse();
        assertThat(ProductStatus.SOLD.canTransitionTo(ProductStatus.REJECTED)).isFalse();
        assertThat(ProductStatus.SOLD.canTransitionTo(ProductStatus.SOLD)).isFalse();
    }

    // ==================== canDelete ====================

    @Test
    @DisplayName("SOLD 状态的商品不能删除")
    void canDelete_sold_shouldReturnFalse() {
        assertThat(ProductStatus.SOLD.canDelete()).isFalse();
    }

    @Test
    @DisplayName("非 SOLD 状态的商品可以删除")
    void canDelete_nonSold_shouldReturnTrue() {
        assertThat(ProductStatus.DRAFT.canDelete()).isTrue();
        assertThat(ProductStatus.ONLINE.canDelete()).isTrue();
        assertThat(ProductStatus.OFFLINE.canDelete()).isTrue();
        assertThat(ProductStatus.PENDING_REVIEW.canDelete()).isTrue();
        assertThat(ProductStatus.REJECTED.canDelete()).isTrue();
    }

    // ==================== fromCode ====================

    @Test
    @DisplayName("有效的 code 应返回对应的枚举值")
    void fromCode_withValidCode_shouldReturnEnum() {
        assertThat(ProductStatus.fromCode("0")).isEqualTo(ProductStatus.DRAFT);
        assertThat(ProductStatus.fromCode("1")).isEqualTo(ProductStatus.ONLINE);
        assertThat(ProductStatus.fromCode("2")).isEqualTo(ProductStatus.SOLD);
        assertThat(ProductStatus.fromCode("3")).isEqualTo(ProductStatus.OFFLINE);
        assertThat(ProductStatus.fromCode("4")).isEqualTo(ProductStatus.PENDING_REVIEW);
        assertThat(ProductStatus.fromCode("5")).isEqualTo(ProductStatus.REJECTED);
    }

    @Test
    @DisplayName("无效的 code 应抛出 IllegalArgumentException")
    void fromCode_withInvalidCode_shouldThrow() {
        assertThatThrownBy(() -> ProductStatus.fromCode("99"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown");
    }

    @Test
    @DisplayName("null code 应抛出 IllegalArgumentException")
    void fromCode_withNullCode_shouldThrow() {
        assertThatThrownBy(() -> ProductStatus.fromCode(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("code must not be null");
    }
}
