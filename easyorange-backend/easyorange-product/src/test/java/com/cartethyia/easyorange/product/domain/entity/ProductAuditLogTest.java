package com.cartethyia.easyorange.product.domain.entity;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProductAuditLog 领域实体测试")
class ProductAuditLogTest {

    @Test
    @DisplayName("使用 builder 构建应正确设置所有字段的值")
    void builder_shouldBuildCorrectly() {
        var log = ProductAuditLog.builder()
                .productId("product-1")
                .operatorId("operator-1")
                .operatorName("管理员")
                .action("1")
                .reason("描述不合规")
                .auditDimensions("{\"category\":\"ok\",\"description\":\"fail\"}")
                .beforeStatus("4")
                .afterStatus("5")
                .remark("已驳回，请修改描述")
                .build();

        assertThat(log.getProductId()).isEqualTo("product-1");
        assertThat(log.getOperatorId()).isEqualTo("operator-1");
        assertThat(log.getOperatorName()).isEqualTo("管理员");
        assertThat(log.getAction()).isEqualTo("1");
        assertThat(log.getReason()).isEqualTo("描述不合规");
        assertThat(log.getAuditDimensions()).isEqualTo("{\"category\":\"ok\",\"description\":\"fail\"}");
        assertThat(log.getBeforeStatus()).isEqualTo("4");
        assertThat(log.getAfterStatus()).isEqualTo("5");
        assertThat(log.getRemark()).isEqualTo("已驳回，请修改描述");
    }

    @Test
    @DisplayName("构建后 createTime 应自动设为非空时间")
    void builder_createTime_shouldBeSet() {
        var log = ProductAuditLog.builder()
                .productId("p1")
                .operatorId("op1")
                .operatorName("admin")
                .action("1")
                .reason("合规")
                .auditDimensions("{}")
                .beforeStatus("4")
                .afterStatus("1")
                .remark("通过")
                .build();

        assertThat(log.getCreateTime()).isNotNull();
    }
}
