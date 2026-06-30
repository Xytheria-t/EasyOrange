package com.cartethyia.easyorange.product.application.query.handler;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductMapper;
import com.cartethyia.easyorange.product.domain.entity.ProductReport;
import com.cartethyia.easyorange.product.domain.repository.ProductReportRepository;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.ProductReportDetailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetReportDetailHandler 测试")
class GetReportDetailHandlerTest {

    @Mock
    private ProductReportRepository productReportRepository;

    @Mock
    private ProductMapper productMapper;

    private GetReportDetailHandler handler;

    private ProductReport report;
    private static final String REPORTER_ID = "2";
    private static final String PRODUCT_OWNER_ID = "1";

    @BeforeEach
    void setUp() {
        handler = new GetReportDetailHandler(productReportRepository, productMapper);

        report = ProductReport.create("1", "2", "假货", 1);
        report = report.assignId("100");
    }

    @Test
    @DisplayName("查询举报详情应返回完整信息")
    void handle_shouldReturnDetail() {
        when(productReportRepository.findById("100")).thenReturn(report);
        ProductDO product = new ProductDO();
        product.setName("测试商品");
        product.setId("1");
        when(productMapper.selectById("1")).thenReturn(product);

        ProductReportDetailResponse response = handler.handle("100", "2");

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("100");
        assertThat(response.productId()).isEqualTo("1");
        assertThat(response.productName()).isEqualTo("测试商品");
        assertThat(response.reason()).isEqualTo("假货");
        assertThat(response.reasonType()).isEqualTo(1);
        assertThat(response.status()).isEqualTo(0);
        assertThat(response.statusDesc()).isEqualTo("待处理");
    }

    @Test
    @DisplayName("查询不存在的举报应抛出异常")
    void handle_whenNotFound_shouldThrow() {
        when(productReportRepository.findById("999")).thenReturn(null);

        assertThatThrownBy(() -> handler.handle("999", "2"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("举报记录不存在");
    }

    @Test
    @DisplayName("非举报人查询应抛出异常")
    void handle_whenNotOwner_shouldThrow() {
        when(productReportRepository.findById("100")).thenReturn(report);

        assertThatThrownBy(() -> handler.handle("100", "999"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权查看");
    }

    @Test
    @DisplayName("商品不存在时 productName 应为 null")
    void handle_whenProductNotFound_shouldReturnNullProductName() {
        when(productReportRepository.findById("100")).thenReturn(report);
        when(productMapper.selectById("1")).thenReturn(null);

        ProductReportDetailResponse response = handler.handle("100", "2");

        assertThat(response.productName()).isNull();
    }

    @Test
    @DisplayName("已处理的举报应显示正确的状态描述")
    void handle_shouldReturnCorrectStatusDesc() {
        report = report.reject("证据不足");
        when(productReportRepository.findById("100")).thenReturn(report);
        when(productMapper.selectById("1")).thenReturn(null);

        ProductReportDetailResponse response = handler.handle("100", "2");

        assertThat(response.status()).isEqualTo(3);
        assertThat(response.statusDesc()).isEqualTo("已驳回");
    }
}
