package com.cartethyia.easyorange.admin.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminReportResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ReportHandleHistoryResponse;
import com.cartethyia.easyorange.admin.domain.enums.ReportHandleAction;
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort.ProductInfo;
import com.cartethyia.easyorange.admin.domain.port.AdminReportQueryPort.ReportHistoryRecord;
import com.cartethyia.easyorange.admin.domain.port.AdminReportQueryPort.ReportRecord;
import com.cartethyia.easyorange.admin.domain.port.AdminUserQueryPort.UserInfo;
import org.springframework.stereotype.Component;

@Component
public class AdminReportAssembler {

    public AdminReportResponse toAdminReportResponse(
            ReportRecord report, UserInfo reporter, ProductInfo product, String productImage) {
        return AdminReportResponse.builder()
                .reportId(report.id())
                .productId(report.productId())
                .productName(product != null ? product.name() : null)
                .productImage(productImage)
                .reporterId(report.reporterId())
                .reporterName(reporter != null ? reporter.nickName() : null)
                .reasonType(report.reasonType() != null ? Integer.valueOf(report.reasonType()) : null)
                .reasonTypeDesc(report.reasonTypeDesc())
                .reason(report.reason())
                .status(report.status() != null ? Integer.valueOf(report.status()) : null)
                .statusDesc(report.statusDesc())
                .handleResult(report.remark())
                .handleRemark(report.remark())
                .createTime(report.createTime())
                .handleTime(report.pending() ? null : report.updateTime())
                .build();
    }

    public ReportHandleHistoryResponse toHistoryResponse(ReportHistoryRecord history, UserInfo operator) {
        return ReportHandleHistoryResponse.builder()
                .id(history.id())
                .reportId(history.reportId())
                .operatorName(operator != null ? operator.nickName() : null)
                .action(history.action())
                .actionDesc(actionDesc(history.action()))
                .remark(history.remark())
                .createTime(history.createTime())
                .build();
    }

    private String actionDesc(String action) {
        ReportHandleAction handleAction = ReportHandleAction.fromCodeOrNull(action);
        return handleAction != null ? handleAction.getDesc() : action;
    }
}
