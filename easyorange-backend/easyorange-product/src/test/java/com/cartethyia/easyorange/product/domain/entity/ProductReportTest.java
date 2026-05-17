package com.cartethyia.easyorange.product.domain.entity;

import com.cartethyia.easyorange.product.domain.enums.ProductReportStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ProductReport 领域实体测试")
class ProductReportTest {

    @Test
    @DisplayName("创建有效举报应生成 PENDING 状态的举报")
    void create_shouldCreatePendingReport() {
        ProductReport report = ProductReport.create(1L, 2L, "假货", 1);

        assertThat(report).isNotNull();
        assertThat(report.getProductId()).isEqualTo(1L);
        assertThat(report.getReporterId()).isEqualTo(2L);
        assertThat(report.getReason()).isEqualTo("假货");
        assertThat(report.getReasonType()).isEqualTo(1);
        assertThat(report.getStatus()).isEqualTo(ProductReportStatus.PENDING);
        assertThat(report.isPending()).isTrue();
        assertThat(report.statusCode()).isEqualTo(0);
    }

    @Test
    @DisplayName("创建举报时 productId 为空应抛出异常")
    void create_withNullProductId_shouldThrow() {
        assertThatThrownBy(() -> ProductReport.create(null, 1L, "假货", 1))
                .isInstanceOf(ProductReport.ReportDomainException.class)
                .hasMessageContaining("商品ID不能为空");
    }

    @Test
    @DisplayName("创建举报时 reporterId 为空应抛出异常")
    void create_withNullReporterId_shouldThrow() {
        assertThatThrownBy(() -> ProductReport.create(1L, null, "假货", 1))
                .isInstanceOf(ProductReport.ReportDomainException.class)
                .hasMessageContaining("举报人ID不能为空");
    }

    @Test
    @DisplayName("创建举报时 reason 为空应抛出异常")
    void create_withNullReason_shouldThrow() {
        assertThatThrownBy(() -> ProductReport.create(1L, 2L, null, 1))
                .isInstanceOf(ProductReport.ReportDomainException.class)
                .hasMessageContaining("举报原因不能为空");
    }

    @Test
    @DisplayName("创建举报时 reason 为空白字符串应抛出异常")
    void create_withBlankReason_shouldThrow() {
        assertThatThrownBy(() -> ProductReport.create(1L, 2L, "   ", 1))
                .isInstanceOf(ProductReport.ReportDomainException.class)
                .hasMessageContaining("举报原因不能为空");
    }

    @Test
    @DisplayName("批准待处理的举报应变为 RESOLVED")
    void approve_shouldChangeStatusToResolved() {
        ProductReport report = ProductReport.create(1L, 2L, "假货", 1);

        report.approve("已处理");

        assertThat(report.getStatus()).isEqualTo(ProductReportStatus.RESOLVED);
        assertThat(report.getRemark()).isEqualTo("已处理");
        assertThat(report.isPending()).isFalse();
    }

    @Test
    @DisplayName("批准非待处理的举报应抛出异常")
    void approve_whenNotPending_shouldThrow() {
        ProductReport report = ProductReport.create(1L, 2L, "假货", 1);
        report.approve("已处理");

        assertThatThrownBy(() -> report.approve("再次处理"))
                .isInstanceOf(ProductReport.ReportDomainException.class)
                .hasMessageContaining("只有待处理的举报才能被批准");
    }

    @Test
    @DisplayName("驳回待处理的举报应变为 DISMISSED")
    void reject_shouldChangeStatusToDismissed() {
        ProductReport report = ProductReport.create(1L, 2L, "假货", 1);

        report.reject("证据不足");

        assertThat(report.getStatus()).isEqualTo(ProductReportStatus.DISMISSED);
        assertThat(report.getRemark()).isEqualTo("证据不足");
        assertThat(report.isPending()).isFalse();
    }

    @Test
    @DisplayName("驳回非待处理的举报应抛出异常")
    void reject_whenNotPending_shouldThrow() {
        ProductReport report = ProductReport.create(1L, 2L, "假货", 1);
        report.reject("证据不足");

        assertThatThrownBy(() -> report.reject("再次驳回"))
                .isInstanceOf(ProductReport.ReportDomainException.class)
                .hasMessageContaining("只有待处理的举报才能被驳回");
    }

    @Test
    @DisplayName("reconstitute 应恢复完整实体状态")
    void reconstitute_shouldRestoreFullState() {
        LocalDateTime now = LocalDateTime.now();

        ProductReport report = ProductReport.reconstitute(
                100L, 1L, 2L, "假货",
                ProductReportStatus.RESOLVED, "已处理",
                now, now, 2
        );

        assertThat(report.getId()).isEqualTo(100L);
        assertThat(report.getProductId()).isEqualTo(1L);
        assertThat(report.getReporterId()).isEqualTo(2L);
        assertThat(report.getReason()).isEqualTo("假货");
        assertThat(report.getStatus()).isEqualTo(ProductReportStatus.RESOLVED);
        assertThat(report.getRemark()).isEqualTo("已处理");
        assertThat(report.getCreateTime()).isEqualTo(now);
        assertThat(report.getUpdateTime()).isEqualTo(now);
        assertThat(report.getReasonType()).isEqualTo(2);
        assertThat(report.isPending()).isFalse();
    }

    @Test
    @DisplayName("assignId 仅在 id 为空时设置")
    void assignId_shouldOnlySetWhenNull() {
        ProductReport report = ProductReport.create(1L, 2L, "假货", 1);

        report.assignId(100L);
        assertThat(report.getId()).isEqualTo(100L);

        report.assignId(200L);
        assertThat(report.getId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("statusCode 应返回状态码")
    void statusCode_shouldReturnStatusCode() {
        ProductReport pending = ProductReport.create(1L, 2L, "假货", 1);
        assertThat(pending.statusCode()).isEqualTo(0);

        pending.approve("处理完成");
        assertThat(pending.statusCode()).isEqualTo(2);
    }
}
