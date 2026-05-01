package com.cartethyia.easyorange.order.application.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.order.domain.port.outbound.ProductQueryPort;
import com.cartethyia.easyorange.order.domain.port.outbound.ProductQueryPort.ProductDetail;
import com.cartethyia.easyorange.order.infrastructure.cache.OrderCacheService;
import com.cartethyia.easyorange.order.domain.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.repository.OrderQueryCondition;
import com.cartethyia.easyorange.order.domain.repository.OrderReadRepository;
import com.cartethyia.easyorange.order.interfaces.assembler.OrderVOAssembler;
import com.cartethyia.easyorange.order.interfaces.dto.response.OrderVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrderQueryHandler 单元测试")
class OrderQueryHandlerTest {

    @Mock
    private OrderReadRepository orderReadRepository;

    @Mock
    private ProductQueryPort productQueryPort;

    @Mock
    private OrderCacheService orderCacheService;

    @Mock
    private OrderVOAssembler orderVOAssembler;

    private OrderQueryHandler handler;

    private OrderReadModel testOrderReadModel;
    private ProductDetail testProductDetail;

    @BeforeEach
    void setUp() {
        handler = new OrderQueryHandler(orderReadRepository, productQueryPort, orderCacheService, orderVOAssembler);

        testOrderReadModel = new OrderReadModel(
                1L, "ORD001", 100L, 200L, 300L,
                new BigDecimal("99.99"), 0, "待付款", 0,
                "北京市朝阳区", "13800138000", "备注", null, null,
                LocalDateTime.now(), LocalDateTime.now()
        );

        testProductDetail = new ProductDetail(
                300L, "测试商品", new BigDecimal("99.99"), 1, List.of("http://img.jpg")
        );
        
        OrderVO mockOrderVO = OrderVO.builder()
                .id(1L)
                .orderNo("ORD001")
                .productTitle("测试商品")
                .build();
        
        when(orderVOAssembler.toOrderVO(any(OrderReadModel.class), anyMap(), anyBoolean()))
                .thenReturn(mockOrderVO);
        when(orderVOAssembler.toOrderVOs(any(), anyMap()))
                .thenReturn(List.of(mockOrderVO));
        when(orderVOAssembler.buildProductMap(any()))
                .thenReturn(Map.of(300L, testProductDetail));
    }

    @Test
    @DisplayName("getOrderDetail 返回订单详情")
    void getOrderDetail_existingOrder_returnsVO() {
        when(orderReadRepository.findById(any())).thenReturn(Optional.of(testOrderReadModel));
        when(productQueryPort.getProductsByIds(any())).thenReturn(List.of(testProductDetail));

        OrderVO result = handler.getOrderDetail(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getOrderNo()).isEqualTo("ORD001");
        assertThat(result.getProductTitle()).isEqualTo("测试商品");
    }

    @Test
    @DisplayName("getOrderDetail 订单不存在返回 null")
    void getOrderDetail_nonExistingOrder_returnsNull() {
        when(orderReadRepository.findById(any())).thenReturn(Optional.empty());

        OrderVO result = handler.getOrderDetail(999L);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("handle 分页查询返回结果")
    void handle_pageQuery_returnsPageResult() {
        PageResult<OrderReadModel> pageResult = PageResult.of(List.of(testOrderReadModel), 1L, 1, 10);
        when(orderReadRepository.findPage(any(OrderQueryCondition.class))).thenReturn(pageResult);
        when(productQueryPort.getProductsByIds(any())).thenReturn(List.of(testProductDetail));

        QueryOrderRequest request = new QueryOrderRequest();
        request.setPageNum(1);
        request.setPageSize(10);
        PageResult<OrderVO> result = handler.handle(request);

        assertThat(result).isNotNull();
        assertThat(result.records()).hasSize(1);
        assertThat(result.records().getFirst().getOrderNo()).isEqualTo("ORD001");
    }

    @Test
    @DisplayName("handle 空结果返回空列表")
    void handle_emptyResult_returnsEmptyPage() {
        PageResult<OrderReadModel> emptyPage = PageResult.of(List.of(), 0L, 1, 10);
        when(orderReadRepository.findPage(any(OrderQueryCondition.class))).thenReturn(emptyPage);

        QueryOrderRequest request = new QueryOrderRequest();
        request.setPageNum(1);
        request.setPageSize(10);
        PageResult<OrderVO> result = handler.handle(request);

        assertThat(result).isNotNull();
        assertThat(result.records()).isEmpty();
    }
}
