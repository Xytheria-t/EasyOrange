package com.cartethyia.easyorange.product.application.command;

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
@DisplayName("ProductReportCommandService 测试")
class ProductReportCommandServiceTest {

    @Mock
    private ProductReportDomainService productReportDomainService;

    @Mock
    private ProductReportRepository productReportRepository;

    private ProductReportCommandService service;

    @BeforeEach
    void setUp() {
        service = new ProductReportCommandService(productReportDomainService, productReportRepository);
    }

    @Test
    @DisplayName("正常举报应调用领域服务")
    void handleReport_shouldDelegateToDomainService() {
        when(productReportRepository.existsRecentReport("1", "2")).thenReturn(false);

        service.handleReport("1", "2", "假货", 1);

        verify(productReportRepository).existsRecentReport("1", "2");
        verify(productReportDomainService).reportProduct("1", "2", "假货", 1);
    }

    @Test
    @DisplayName("24小时内重复举报应抛出异常")
    void handleReport_whenRecentReportExists_shouldThrow() {
        when(productReportRepository.existsRecentReport("1", "2")).thenReturn(true);

        assertThatThrownBy(() -> service.handleReport("1", "2", "假货", 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已在24小时内举报过");

        verify(productReportDomainService, never()).reportProduct(any(), any(), any(), any());
    }
}
