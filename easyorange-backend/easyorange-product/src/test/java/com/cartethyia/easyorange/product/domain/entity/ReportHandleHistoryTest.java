package com.cartethyia.easyorange.product.domain.entity;

import static org.assertj.core.api.Assertions.*;

import com.cartethyia.easyorange.product.domain.exception.ProductDomainException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ReportHandleHistory 领域实体测试")
class ReportHandleHistoryTest {

    @Test
    @DisplayName("创建有效的处理历史应成功")
    void create_shouldCreateHistory() {
        ReportHandleHistory history = ReportHandleHistory.create("100", "1", "2", "RESOLVE", "违规商品已下架");

        assertThat(history).isNotNull();
        assertThat(history.getId()).as("主键由调用方传入（应用层 IdGenerator）").isEqualTo("100");
        assertThat(history.getReportId()).isEqualTo("1");
        assertThat(history.getOperatorId()).isEqualTo("2");
        assertThat(history.getAction()).isEqualTo("RESOLVE");
        assertThat(history.getRemark()).isEqualTo("违规商品已下架");
        assertThat(history.getCreateTime()).isNotNull();
    }

    @Test
    @DisplayName("创建时 reportId 为空应抛出异常")
    void create_withNullReportId_shouldThrow() {
        assertThatThrownBy(() -> ReportHandleHistory.create("100", null, "1", "RESOLVE", "备注"))
                .isInstanceOf(ProductDomainException.class)
                .hasMessageContaining("举报ID不能为空");
    }

    @Test
    @DisplayName("创建时 operatorId 为空应抛出异常")
    void create_withNullOperatorId_shouldThrow() {
        assertThatThrownBy(() -> ReportHandleHistory.create("100", "1", null, "RESOLVE", "备注"))
                .isInstanceOf(ProductDomainException.class)
                .hasMessageContaining("操作人ID不能为空");
    }

    @Test
    @DisplayName("创建时 action 为空应抛出异常")
    void create_withNullAction_shouldThrow() {
        assertThatThrownBy(() -> ReportHandleHistory.create("100", "1", "2", null, "备注"))
                .isInstanceOf(ProductDomainException.class)
                .hasMessageContaining("动作类型不能为空");
    }

    @Test
    @DisplayName("创建时 action 为空白字符串应抛出异常")
    void create_withBlankAction_shouldThrow() {
        assertThatThrownBy(() -> ReportHandleHistory.create("100", "1", "2", "  ", "备注"))
                .isInstanceOf(ProductDomainException.class)
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
}
