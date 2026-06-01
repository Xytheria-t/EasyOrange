package com.cartethyia.easyorange.product.application.query.handler;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.product.domain.entity.ProductReport;
import com.cartethyia.easyorange.product.domain.repository.ProductReportRepository;
import com.cartethyia.easyorange.product.domain.enums.ProductReportStatus;
import com.cartethyia.easyorange.product.domain.enums.ReportReasonType;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductMapper;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.ProductReportDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GetReportDetailHandler {

    private final ProductReportRepository productReportRepository;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public ProductReportDetailResponse handle(Long reportId, Long currentUserId) {
        ProductReport report = productReportRepository.findById(reportId);
        BizRequire.notNull(report, "举报记录不存在");

        if (!report.getReporterId().equals(currentUserId)) {
            throw BusinessException.of("无权查看此举报记录");
        }

        String statusDesc = switch (report.statusCode()) {
            case 0 -> "待处理";
            case 1 -> "处理中";
            case 2 -> "已解决";
            case 3 -> "已驳回";
            default -> "未知";
        };

        ReportReasonType reasonType = ReportReasonType.fromCode(report.getReasonType());

        ProductDO product = productMapper.selectById(report.getProductId());
        String productName = product != null ? product.getName() : null;

        return new ProductReportDetailResponse(
                report.getId(),
                report.getProductId(),
                productName,
                report.getReasonType(),
                reasonType != null ? reasonType.getDesc() : null,
                report.getReason(),
                report.statusCode(),
                statusDesc,
                report.getRemark(),
                report.getCreateTime(),
                report.getUpdateTime()
        );
    }
}
