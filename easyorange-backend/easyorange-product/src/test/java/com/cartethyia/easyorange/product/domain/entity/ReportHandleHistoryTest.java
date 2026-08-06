package com.cartethyia.easyorange.product.domain.entity;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ReportHandleHistory 领域实体测试")
class ReportHandleHistoryTest {

    @Test
    @DisplayName("创建有效的处理历史应成功")
    void create_shouldCreateHistory() {
        ReportHandleHistory history = ReportHandleHistory.create("1", "2", "RESOLVE", "违规商品已下架");

        assertThat(history).isNotNull();
        assertThat(history.getReportId()).isEqualTo("1");
        assertThat(history.getOperatorId()).isEqualTo("2");
        assertThat(history.getAction()).isEqualTo("RESOLVE");
        assertThat(history.getRemark()).isEqualTo("违规商品已下架");
        assertThat(history.getCreateTime()).isNotNull();
    }

    @Test
    @DisplayName("创建时 reportId 为空应抛出异常")
    void create_withNullReportId_shouldThrow() {
        assertThatThrownBy(() -> ReportHandleHistory.create(null, "1", "RESOLVE", "备注"))
                .isInstanceOf(ReportHandleHistory.HistoryDomainException.class)
                .hasMessageContaining("举报ID不能为空");
    }

    @Test
    @DisplayName("创建时 operatorId 为空应抛出异常")
    void create_withNullOperatorId_shouldThrow() {
        assertThatThrownBy(() -> ReportHandleHistory.create("1", null, "RESOLVE", "备注"))
                .isInstanceOf(ReportHandleHistory.HistoryDomainException.class)
                .hasMessageContaining("操作人ID不能为空");
    }

    @Test
    @DisplayName("创建时 action 为空应抛出异常")
    void create_withNullAction_shouldThrow() {
        assertThatThrownBy(() -> ReportHandleHistory.create("1", "2", null, "备注"))
                .isInstanceOf(ReportHandleHistory.HistoryDomainException.class)
                .hasMessageContaining("动作类型不能为空");
    }

    @Test
    @DisplayName("创建时 action 为空白字符串应抛出异常")
    void create_withBlankAction_shouldThrow() {
        assertThatThrownBy(() -> ReportHandleHistory.create("1", "2", "  ", "备注"))
                .isInstanceOf(ReportHandleHistory.HistoryDomainException.class)
                .hasMessageContaining("动作类型不能为空");
    }

    @Test
    @DisplayName("reconstitute 应恢复完整实体状态")
    void reconstitute_shouldRestoreFullState() {
        LocalDateTime now = LocalDateTime.now();

        ReportHandleHistory history = ReportHandleHistory.reconstitute("100", "1", "2", "RESOLVE", "已处理", now);

        assertThat(history.getId()).isEqualTo("100");
        assertThat(history.getReportId()).isEqualTo("1");
        assertThat(history.getOperatorId()).isEqualTo("2");
        assertThat(history.getAction()).isEqualTo("RESOLVE");
        assertThat(history.getRemark()).isEqualTo("已处理");
        assertThat(history.getCreateTime()).isEqualTo(now);
    }

    @Test
    @DisplayName("withId 应返回包含指定 ID 的新实例（不可变模式）")
    void withId_shouldReturnNewInstance() {
        ReportHandleHistory history = ReportHandleHistory.create("1", "2", "RESOLVE", "备注");

        ReportHandleHistory withId1 = history.withId("100");
        assertThat(withId1.getId()).isEqualTo("100");
        assertThat(history.getId()).isNull();

        ReportHandleHistory withId2 = withId1.withId("200");
        assertThat(withId2.getId()).isEqualTo("200");
        assertThat(withId1.getId()).isEqualTo("100");
    }
}
