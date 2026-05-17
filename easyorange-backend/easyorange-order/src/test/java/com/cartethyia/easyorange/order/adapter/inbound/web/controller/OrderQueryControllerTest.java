package com.cartethyia.easyorange.order.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.order.application.command.OrderCommandHandler;
import com.cartethyia.easyorange.order.application.dto.OrderVO;
import com.cartethyia.easyorange.order.application.query.OrderQueryHandler;
import com.cartethyia.easyorange.order.application.query.QueryOrderRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderQueryController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser
@DisplayName("OrderQueryController 控制器测试")
class OrderQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderCommandHandler commandHandler;

    @MockitoBean
    private OrderQueryHandler queryHandler;

    private OrderVO createOrderVO(Long id, String orderNo, Integer status, String statusDesc) {
        return OrderVO.builder()
                .id(id)
                .orderNo(orderNo)
                .buyerId(1L)
                .sellerId(2L)
                .productId(100L)
                .amount(new BigDecimal("99.99"))
                .status(status)
                .statusDesc(statusDesc)
                .address("北京市朝阳区")
                .phone("138****8000")
                .remark("尽快发货")
                .createTime(LocalDateTime.of(2026, 5, 1, 10, 0))
                .updateTime(LocalDateTime.of(2026, 5, 1, 12, 0))
                .build();
    }

    @Nested
    @DisplayName("GET /api/orders/{id}")
    class GetOrderDetailTests {

        @Test
        @DisplayName("存在的订单应返回订单详情")
        void getOrderDetail_withExistingId_shouldReturnOrder() throws Exception {
            OrderVO vo = createOrderVO(100L, "ORD100", 0, "待付款");
            when(queryHandler.getOrderDetailForOwner(100L)).thenReturn(vo);

            mockMvc.perform(get("/api/orders/{id}", 100L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("A0000"))
                    .andExpect(jsonPath("$.data.id").value(100))
                    .andExpect(jsonPath("$.data.orderNo").value("ORD100"))
                    .andExpect(jsonPath("$.data.status").value(0))
                    .andExpect(jsonPath("$.data.statusDesc").value("待付款"));
        }

        @Test
        @DisplayName("不存在的订单应返回空 data")
        void getOrderDetail_withNonExistentId_shouldReturnNullData() throws Exception {
            when(queryHandler.getOrderDetailForOwner(999L)).thenReturn(null);

            mockMvc.perform(get("/api/orders/{id}", 999L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("A0000"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }

        @Test
        @DisplayName("handler 异常应包装为 ServletException")
        void getOrderDetail_whenHandlerThrows_shouldThrowServletException() {
            when(queryHandler.getOrderDetailForOwner(100L))
                    .thenThrow(new RuntimeException("订单不存在"));

            org.junit.jupiter.api.Assertions.assertThrows(
                    jakarta.servlet.ServletException.class,
                    () -> mockMvc.perform(get("/api/orders/{id}", 100L))
            );
        }
    }

    @Nested
    @DisplayName("GET /api/orders/my")
    class GetMyOrdersTests {

        @Test
        @DisplayName("有订单数据应返回分页结果")
        void getMyOrders_withData_shouldReturnPage() throws Exception {
            List<OrderVO> records = List.of(
                    createOrderVO(100L, "ORD100", 0, "待付款"),
                    createOrderVO(101L, "ORD101", 1, "已付款")
            );
            PageResult<OrderVO> pageResult = PageResult.of(records, 2L, 1, 10);
            when(queryHandler.getMyOrders(any(QueryOrderRequest.class))).thenReturn(pageResult);

            mockMvc.perform(get("/api/orders/my")
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("A0000"))
                    .andExpect(jsonPath("$.data.records.length()").value(2))
                    .andExpect(jsonPath("$.data.total").value(2))
                    .andExpect(jsonPath("$.data.current").value(1))
                    .andExpect(jsonPath("$.data.size").value(10))
                    .andExpect(jsonPath("$.data.records[0].orderNo").value("ORD100"))
                    .andExpect(jsonPath("$.data.records[1].orderNo").value("ORD101"));
        }

        @Test
        @DisplayName("无订单时应返回空分页")
        void getMyOrders_withNoData_shouldReturnEmptyPage() throws Exception {
            PageResult<OrderVO> emptyResult = PageResult.empty(1, 10);
            when(queryHandler.getMyOrders(any(QueryOrderRequest.class))).thenReturn(emptyResult);

            mockMvc.perform(get("/api/orders/my"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("A0000"))
                    .andExpect(jsonPath("$.data.records").isArray())
                    .andExpect(jsonPath("$.data.records.length()").value(0))
                    .andExpect(jsonPath("$.data.total").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/orders/sold")
    class GetSoldOrdersTests {

        @Test
        @DisplayName("有售出订单应返回分页结果")
        void getSoldOrders_withData_shouldReturnPage() throws Exception {
            List<OrderVO> records = List.of(
                    createOrderVO(100L, "ORD100", 2, "已发货")
            );
            PageResult<OrderVO> pageResult = PageResult.of(records, 1L, 1, 10);
            when(queryHandler.getSoldOrders(any(QueryOrderRequest.class))).thenReturn(pageResult);

            mockMvc.perform(get("/api/orders/sold"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("A0000"))
                    .andExpect(jsonPath("$.data.records.length()").value(1))
                    .andExpect(jsonPath("$.data.records[0].orderNo").value("ORD100"))
                    .andExpect(jsonPath("$.data.records[0].status").value(2));
        }

        @Test
        @DisplayName("无售出订单应返回空分页")
        void getSoldOrders_withNoData_shouldReturnEmptyPage() throws Exception {
            when(queryHandler.getSoldOrders(any(QueryOrderRequest.class)))
                    .thenReturn(PageResult.empty(1, 10));

            mockMvc.perform(get("/api/orders/sold"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("A0000"))
                    .andExpect(jsonPath("$.data.records.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/orders/list")
    class QueryOrdersTests {

        @Test
        @DisplayName("通用查询应返回分页结果")
        void queryOrders_withFilters_shouldReturnPage() throws Exception {
            List<OrderVO> records = List.of(
                    createOrderVO(100L, "ORD100", 0, "待付款")
            );
            PageResult<OrderVO> pageResult = PageResult.of(records, 1L, 1, 10);
            when(queryHandler.handle(any(QueryOrderRequest.class))).thenReturn(pageResult);

            mockMvc.perform(get("/api/orders/list")
                            .param("status", "0")
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("A0000"))
                    .andExpect(jsonPath("$.data.records.length()").value(1))
                    .andExpect(jsonPath("$.data.records[0].status").value(0));
        }

        @Test
        @DisplayName("无结果时应返回空分页")
        void queryOrders_withNoResults_shouldReturnEmptyPage() throws Exception {
            when(queryHandler.handle(any(QueryOrderRequest.class)))
                    .thenReturn(PageResult.empty(1, 10));

            mockMvc.perform(get("/api/orders/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("A0000"))
                    .andExpect(jsonPath("$.data.records.length()").value(0));
        }
    }
}
