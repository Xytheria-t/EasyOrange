package com.cartethyia.easyorange.product.application.command.handler;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.product.domain.service.ProductReportDomainService;
import com.cartethyia.easyorange.product.domain.repository.ProductReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateProductReportHandler 测试")
class CreateProductReportHandlerTest {

    @Mock
    private ProductReportDomainService productReportDomainService;

    @Mock
    private ProductReportRepository productReportRepository;

    private CreateProductReportHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CreateProductReportHandler(productReportDomainService, productReportRepository);
    }

    @Test
    @DisplayName("正常举报应调用领域服务")
    void handleReport_shouldDelegateToDomainService() {
        when(productReportRepository.existsRecentReport(1L, 2L)).thenReturn(false);

        handler.handleReport(1L, 2L, "假货", 1);

        verify(productReportRepository).existsRecentReport(1L, 2L);
        verify(productReportDomainService).reportProduct(1L, 2L, "假货", 1);
    }

    @Test
    @DisplayName("24小时内重复举报应抛出异常")
    void handleReport_whenRecentReportExists_shouldThrow() {
        when(productReportRepository.existsRecentReport(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> handler.handleReport(1L, 2L, "假货", 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已在24小时内举报过");

        verify(productReportDomainService, never()).reportProduct(any(), any(), any(), any());
    }
}
