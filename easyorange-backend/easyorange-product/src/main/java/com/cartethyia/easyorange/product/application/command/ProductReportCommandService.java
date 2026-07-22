package com.cartethyia.easyorange.product.application.command;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.product.domain.enums.ReportReasonType;
import com.cartethyia.easyorange.product.domain.service.ProductReportDomainService;
import com.cartethyia.easyorange.product.domain.repository.ProductReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductReportCommandService {

    private final ProductReportDomainService productReportDomainService;
    private final ProductReportRepository productReportRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional(rollbackFor = Exception.class)
    public void handleReport(String productId, String reporterId, String reason, Integer reasonType) {
        if (!ReportReasonType.isValidCode(reasonType)) {
            throw BusinessException.of("无效的举报类型");
        }
        if (productReportRepository.existsRecentReport(productId, reporterId)) {
            throw BusinessException.of("您已在24小时内举报过该商品，请耐心等待处理");
        }
        productReportDomainService.reportProduct(productId, reporterId, reason, reasonType);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleApprove(String reportId) {
        var event = productReportDomainService.processReport(reportId, true);
        if (event.isPresent()) {
            domainEventPublisher.publish(event.get());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleReject(String reportId) {
        productReportDomainService.processReport(reportId, false);
    }
}
