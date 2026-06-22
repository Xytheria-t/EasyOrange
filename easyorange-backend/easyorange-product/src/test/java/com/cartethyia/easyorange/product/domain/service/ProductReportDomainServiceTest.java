package com.cartethyia.easyorange.product.domain.service;

import com.cartethyia.easyorange.product.domain.entity.ProductReport;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.port.ProductCachePort;
import com.cartethyia.easyorange.product.domain.repository.ProductReportRepository;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductReportDomainService 测试")
class ProductReportDomainServiceTest {

    @Mock
    private ProductReportRepository productReportRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductCachePort productCachePort;

    private ProductReportDomainService domainService;

    @BeforeEach
    void setUp() {
        domainService = new ProductReportDomainService(productReportRepository, productRepository, productCachePort);
    }

    @Test
    @DisplayName("举报商品时应创建并保存举报")
    void reportProduct_shouldCreateAndSave() {
        domainService.reportProduct(1L, 2L, "假货", 1);

        verify(productReportRepository).save(any(ProductReport.class));
    }

    @Test
    @DisplayName("批准举报后应将商品下架并清除缓存")
    void processReport_withApprove_shouldOfflineProductAndEvictCache() {
        ProductReport report = ProductReport.create(1L, 2L, "假货", 1);
        report = report.assignId(100L);
        when(productReportRepository.findById(100L)).thenReturn(report);

        domainService.processReport(100L, true);

        verify(productRepository).updateStatus(ProductId.of(1L), ProductStatus.OFFLINE);
        verify(productCachePort).evictProductCache(1L);
        verify(productReportRepository).update(argThat(r -> r != null && !r.isPending()));
    }

    @Test
    @DisplayName("驳回举报不应操作商品状态和缓存")
    void processReport_withReject_shouldNotTouchProduct() {
        ProductReport report = ProductReport.create(1L, 2L, "假货", 1);
        report = report.assignId(100L);
        when(productReportRepository.findById(100L)).thenReturn(report);

        domainService.processReport(100L, false);

        verify(productRepository, never()).updateStatus(any(), any());
        verify(productCachePort, never()).evictProductCache(any());
        verify(productReportRepository).update(argThat(r -> r != null && !r.isPending()));
    }

    @Test
    @DisplayName("处理不存在的举报应抛出异常")
    void processReport_whenNotFound_shouldThrow() {
        when(productReportRepository.findById(999L)).thenReturn(null);

        assertThatThrownBy(() -> domainService.processReport(999L, true))
                .isInstanceOf(ProductReportDomainService.ReportNotFoundException.class)
                .hasMessageContaining("举报记录不存在");
    }

    @Test
    @DisplayName("Processing report 更新后应保持 remark 正确")
    void processReport_withApprove_shouldSetCorrectRemark() {
        ProductReport report = ProductReport.create(1L, 2L, "假货", 1);
        report = report.assignId(100L);
        when(productReportRepository.findById(100L)).thenReturn(report);

        domainService.processReport(100L, true);

        ArgumentCaptor<ProductReport> captor = ArgumentCaptor.forClass(ProductReport.class);
        verify(productReportRepository).update(captor.capture());
        ProductReport updated = captor.getValue();
        assertThat(updated.getRemark()).isNull();
        assertThat(updated.isPending()).isFalse();
    }
}
