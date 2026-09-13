package com.cartethyia.easyorange.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.admin.domain.port.AdminReportPort;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.application.command.ProductReportCommandHandler;
import com.cartethyia.easyorange.product.application.port.query.ProductReportQueryRepository;
import com.cartethyia.easyorange.product.domain.entity.ProductReport;
import com.cartethyia.easyorange.product.domain.entity.ReportHandleHistory;
import com.cartethyia.easyorange.product.domain.enums.ReportReasonType;
import com.cartethyia.easyorange.product.domain.repository.ProductReportRepository;
import com.cartethyia.easyorange.product.domain.repository.ReportHandleHistoryRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 举报写路径落库集成测试 —— 兜住全 Mockito 单测覆盖不到的 SQL 语义：{@code eo_product_report.id} /
 * {@code eo_report_handle_history.id} 为 {@code IdType.INPUT}（数据库不回填），主键必须由应用层或出站适配器
 * 在持久化前生成，否则 insert 直接报「Column 'id' cannot be null」。
 * <p>
 * 评价写路径见 {@link ReviewPersistenceIT}（其 {@code order_id} 为 NOT NULL，需先造一笔成交订单）。
 */
@DisplayName("举报写路径落库集成测试（真实 MySQL）")
class ReportPersistenceIT extends AbstractIntegrationTest {

    @Autowired
    private ProductReportCommandHandler productReportCommandHandler;

    @Autowired
    private ProductReportQueryRepository productReportQueryRepository;

    @Autowired
    private ProductReportRepository productReportRepository;

    @Autowired
    private ReportHandleHistoryRepository reportHandleHistoryRepository;

    @Autowired
    private AdminReportPort adminReportPort;

    @Autowired
    private IdGenerator idGenerator;

    @Test
    @DisplayName("举报落库：主键由应用层生成，可按举报人回读")
    void report_persistedWithGeneratedId() {
        String productId = UUID.randomUUID().toString();
        String reporterId = UUID.randomUUID().toString();

        productReportCommandHandler.handleReport(productId, reporterId, "假货", ReportReasonType.FAKE_INFO.getCode());

        PageResult<ProductReport> page = productReportQueryRepository.findByReporterId(reporterId, 1, 10);
        assertThat(page.records()).as("举报必须落库").hasSize(1);

        ProductReport saved = page.records().getFirst();
        assertThat(saved.getId()).as("主键由应用层生成").isNotBlank();
        assertThat(UUID.fromString(saved.getId()).version()).as("主键为 UUID v7").isEqualTo(7);
        assertThat(saved.getProductId()).isEqualTo(productId);
    }

    @Test
    @DisplayName("举报处置历史落库：主键由出站适配器生成，可按举报回读")
    void reportHandleHistory_persistedWithGeneratedId() {
        String reportId = idGenerator.generateId();
        productReportRepository.save(ProductReport.create(
                reportId,
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "假货",
                ReportReasonType.FAKE_INFO.getCode()));

        adminReportPort.handleReport(
                reportId, "resolve", "已核实", UUID.randomUUID().toString());

        List<ReportHandleHistory> histories = reportHandleHistoryRepository.findByReportId(reportId);
        assertThat(histories).as("处置历史必须落库").hasSize(1);

        ReportHandleHistory history = histories.getFirst();
        assertThat(history.getId()).as("主键由出站适配器生成").isNotBlank();
        assertThat(UUID.fromString(history.getId()).version()).as("主键为 UUID v7").isEqualTo(7);
        assertThat(history.getAction()).isEqualTo("resolve");
    }
}
