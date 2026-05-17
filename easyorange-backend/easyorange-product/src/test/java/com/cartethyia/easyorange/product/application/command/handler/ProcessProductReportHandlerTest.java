package com.cartethyia.easyorange.product.application.command.handler;

import com.cartethyia.easyorange.product.domain.service.ProductReportDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessProductReportHandler 测试")
class ProcessProductReportHandlerTest {

    @Mock
    private ProductReportDomainService productReportDomainService;

    private ProcessProductReportHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ProcessProductReportHandler(productReportDomainService);
    }

    @Test
    @DisplayName("批准举报应委托给领域服务")
    void handleApprove_shouldDelegateToDomainService() {
        handler.handleApprove(100L);

        verify(productReportDomainService).processReport(100L, true);
    }

    @Test
    @DisplayName("驳回举报应委托给领域服务")
    void handleReject_shouldDelegateToDomainService() {
        handler.handleReject(100L);

        verify(productReportDomainService).processReport(100L, false);
    }
}
