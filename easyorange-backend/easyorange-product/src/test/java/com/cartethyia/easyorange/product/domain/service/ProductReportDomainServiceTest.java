package com.cartethyia.easyorange.product.domain.service;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.entity.ProductReport;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.port.ProductCacheEvictionPort;
import com.cartethyia.easyorange.product.domain.repository.ProductReportRepository;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.valueobject.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductReportDomainService 测试")
class ProductReportDomainServiceTest {

    private static final String PRODUCT_ID = "1";

    @Mock
    private ProductReportRepository productReportRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductCacheEvictionPort productCacheEvictionPort;

    private ProductReportDomainService domainService;

    @BeforeEach
    void setUp() {
        domainService = new ProductReportDomainService(productReportRepository, productRepository, productCacheEvictionPort);
    }

    @Test
    @DisplayName("举报商品时应创建并保存举报")
    void reportProduct_shouldCreateAndSave() {
        domainService.reportProduct("1", "2", "假货", "1");

        verify(productReportRepository).save(any(ProductReport.class));
    }

    @Test
    @DisplayName("批准举报后应将商品下架、清除缓存并返回事件")
    void processReport_withApprove_shouldOfflineProductAndEvictCache() {
        ProductReport report = ProductReport.create(PRODUCT_ID, "2", "假货", "1");
        report = report.assignId("100");
        when(productReportRepository.findById("100")).thenReturn(report);
        when(productRepository.findById(ProductId.of(PRODUCT_ID)))
                .thenReturn(Optional.of(createOnlineProduct()));

        var event = domainService.processReport("100", true);

        assertThat(event).isPresent();
        assertThat(event.get().productId()).isEqualTo(PRODUCT_ID);
        verify(productRepository).update(argThat(p ->
                p.getId().value().equals(PRODUCT_ID) && p.getStatus() == ProductStatus.OFFLINE));
        verify(productCacheEvictionPort).evictProductCache(PRODUCT_ID);
        verify(productReportRepository).update(argThat(r -> r != null && !r.isPending()));
    }

    @Test
    @DisplayName("驳回举报不应操作商品状态和缓存")
    void processReport_withReject_shouldNotTouchProduct() {
        ProductReport report = ProductReport.create(PRODUCT_ID, "2", "假货", "1");
        report = report.assignId("100");
        when(productReportRepository.findById("100")).thenReturn(report);

        var event = domainService.processReport("100", false);

        assertThat(event).isEmpty();
        verify(productRepository, never()).update(any());
        verify(productCacheEvictionPort, never()).evictProductCache(any());
        verify(productReportRepository).update(argThat(r -> r != null && !r.isPending()));
    }

    @Test
    @DisplayName("处理不存在的举报应抛出异常")
    void processReport_whenNotFound_shouldThrow() {
        when(productReportRepository.findById("999")).thenReturn(null);

        assertThatThrownBy(() -> domainService.processReport("999", true))
                .isInstanceOf(ProductReportDomainService.ReportNotFoundException.class)
                .hasMessageContaining("举报记录不存在");
    }

    @Test
    @DisplayName("批准举报后应保持 remark 正确")
    void processReport_withApprove_shouldSetCorrectRemark() {
        ProductReport report = ProductReport.create(PRODUCT_ID, "2", "假货", "1");
        report = report.assignId("100");
        when(productReportRepository.findById("100")).thenReturn(report);
        when(productRepository.findById(ProductId.of(PRODUCT_ID)))
                .thenReturn(Optional.of(createOnlineProduct()));

        domainService.processReport("100", true);

        ArgumentCaptor<ProductReport> captor = ArgumentCaptor.forClass(ProductReport.class);
        verify(productReportRepository).update(captor.capture());
        ProductReport updated = captor.getValue();
        assertThat(updated.getRemark()).isNull();
        assertThat(updated.isPending()).isFalse();
    }

    private Product createOnlineProduct() {
        return Product.builder()
                .id(ProductId.of(PRODUCT_ID))
                .sellerId(SellerId.of("2"))
                .categoryId(CategoryId.of("1"))
                .title(ProductTitle.of("测试商品"))
                .price(Money.of(new BigDecimal("100")))
                .stock(StockQuantity.of(10))
                .version(Version.INITIAL)
                .status(ProductStatus.ONLINE)
                .viewCount(0)
                .conditionLevel(ConditionLevel.NEW)
                .location(TradeLocation.of("北京"))
                .contactMethod(ContactMethod.of("微信"))
                .description(ProductDescription.of("描述"))
                .images(ImageSet.of(List.of("http://img/1.jpg")))
                .tags(TagSet.empty())
                .build();
    }
}
