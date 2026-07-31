package com.cartethyia.easyorange.admin.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminReportResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ReportHandleHistoryResponse;
import com.cartethyia.easyorange.admin.domain.enums.ReportHandleAction;
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort.ProductInfo;
import com.cartethyia.easyorange.admin.domain.port.AdminUserQueryPort.UserInfo;
import com.cartethyia.easyorange.product.domain.entity.ProductReport;
import com.cartethyia.easyorange.product.domain.entity.ReportHandleHistory;
import com.cartethyia.easyorange.product.domain.enums.ProductReportStatus;
import com.cartethyia.easyorange.product.domain.enums.ReportReasonType;
import org.springframework.stereotype.Component;

@Component
public class AdminReportAssembler {

    public AdminReportResponse toAdminReportResponse(
        ProductReport report,
        UserInfo reporter,
        ProductInfo product,
        String productImage
    ) {
        return AdminReportResponse.builder()
            .reportId(report.getId())
            .productId(report.getProductId())
            .productName(product != null ? product.name() : null)
            .productImage(productImage)
            .reporterId(report.getReporterId())
            .reporterName(reporter != null ? reporter.nickName() : null)
            .reasonType(report.getReasonType() != null ? Integer.valueOf(report.getReasonType()) : null)
            .reasonTypeDesc(reasonTypeDesc(report.getReasonType()))
            .reason(report.getReason())
            .status(report.statusCode() != null ? Integer.valueOf(report.statusCode()) : null)
            .statusDesc(statusDesc(report.statusCode()))
            .handleResult(report.getRemark())
            .handleRemark(report.getRemark())
            .createTime(report.getCreateTime())
            .handleTime(report.isPending() ? null : report.getUpdateTime())
            .build();
    }

    public ReportHandleHistoryResponse toHistoryResponse(ReportHandleHistory history, UserInfo operator) {
        return ReportHandleHistoryResponse.builder()
            .id(history.getId())
            .reportId(history.getReportId())
            .operatorName(operator != null ? operator.nickName() : null)
            .action(history.getAction())
            .actionDesc(actionDesc(history.getAction()))
            .remark(history.getRemark())
            .createTime(history.getCreateTime())
            .build();
    }

    private String statusDesc(String code) {
        if (code == null) {
            return null;
        }
        try {
            return ProductReportStatus.fromCode(code).getDesc();
        } catch (IllegalArgumentException e) {
            return "未知";
        }
    }

    private String reasonTypeDesc(String code) {
        if (code == null) {
            return null;
        }
        try {
            return ReportReasonType.fromCode(code).getDesc();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String actionDesc(String action) {
        ReportHandleAction handleAction = ReportHandleAction.fromCodeOrNull(action);
        return handleAction != null ? handleAction.getDesc() : action;
    }
}
