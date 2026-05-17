package com.cartethyia.easyorange.product.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ReportHandleHistory 领域实体测试")
class ReportHandleHistoryTest {

    @Test
    @DisplayName("创建有效的处理历史应成功")
    void create_shouldCreateHistory() {
        ReportHandleHistory history = ReportHandleHistory.create(1L, 2L, "RESOLVE", "违规商品已下架");

        assertThat(history).isNotNull();
        assertThat(history.getReportId()).isEqualTo(1L);
        assertThat(history.getOperatorId()).isEqualTo(2L);
        assertThat(history.getAction()).isEqualTo("RESOLVE");
        assertThat(history.getRemark()).isEqualTo("违规商品已下架");
        assertThat(history.getCreateTime()).isNotNull();
    }

    @Test
    @DisplayName("创建时 reportId 为空应抛出异常")
    void create_withNullReportId_shouldThrow() {
        assertThatThrownBy(() -> ReportHandleHistory.create(null, 1L, "RESOLVE", "备注"))
                .isInstanceOf(ReportHandleHistory.HistoryDomainException.class)
                .hasMessageContaining("举报ID不能为空");
    }

    @Test
    @DisplayName("创建时 operatorId 为空应抛出异常")
    void create_withNullOperatorId_shouldThrow() {
        assertThatThrownBy(() -> ReportHandleHistory.create(1L, null, "RESOLVE", "备注"))
                .isInstanceOf(ReportHandleHistory.HistoryDomainException.class)
                .hasMessageContaining("操作人ID不能为空");
    }

    @Test
    @DisplayName("创建时 action 为空应抛出异常")
    void create_withNullAction_shouldThrow() {
        assertThatThrownBy(() -> ReportHandleHistory.create(1L, 2L, null, "备注"))
                .isInstanceOf(ReportHandleHistory.HistoryDomainException.class)
                .hasMessageContaining("动作类型不能为空");
    }

    @Test
    @DisplayName("创建时 action 为空白字符串应抛出异常")
    void create_withBlankAction_shouldThrow() {
        assertThatThrownBy(() -> ReportHandleHistory.create(1L, 2L, "  ", "备注"))
                .isInstanceOf(ReportHandleHistory.HistoryDomainException.class)
                .hasMessageContaining("动作类型不能为空");
    }

    @Test
    @DisplayName("reconstitute 应恢复完整实体状态")
    void reconstitute_shouldRestoreFullState() {
        LocalDateTime now = LocalDateTime.now();

        ReportHandleHistory history = ReportHandleHistory.reconstitute(
                100L, 1L, 2L, "RESOLVE", "已处理", now
        );

        assertThat(history.getId()).isEqualTo(100L);
        assertThat(history.getReportId()).isEqualTo(1L);
        assertThat(history.getOperatorId()).isEqualTo(2L);
        assertThat(history.getAction()).isEqualTo("RESOLVE");
        assertThat(history.getRemark()).isEqualTo("已处理");
        assertThat(history.getCreateTime()).isEqualTo(now);
    }

    @Test
    @DisplayName("assignId 仅在 id 为空时设置")
    void assignId_shouldOnlySetWhenNull() {
        ReportHandleHistory history = ReportHandleHistory.create(1L, 2L, "RESOLVE", "备注");

        history.assignId(100L);
        assertThat(history.getId()).isEqualTo(100L);

        history.assignId(200L);
        assertThat(history.getId()).isEqualTo(100L);
    }
}
