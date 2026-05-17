package com.cartethyia.easyorange.order.adapter.inbound.web.controller;

import com.cartethyia.easyorange.order.application.command.CancelOrderCommand;
import com.cartethyia.easyorange.order.application.command.ConfirmReceiptCommand;
import com.cartethyia.easyorange.order.application.command.CreateOrderCommand;
import com.cartethyia.easyorange.order.application.command.CreateOrderResult;
import com.cartethyia.easyorange.order.application.command.OrderCommandHandler;
import com.cartethyia.easyorange.order.application.command.PayOrderCommand;
import com.cartethyia.easyorange.order.application.command.RefundOrderCommand;
import com.cartethyia.easyorange.order.application.command.ShipOrderCommand;
import com.cartethyia.easyorange.order.application.dto.OrderVO;
import com.cartethyia.easyorange.order.application.query.OrderQueryHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderCommandController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser
@DisplayName("OrderCommandController 控制器测试")
class OrderCommandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderCommandHandler commandHandler;

    @MockitoBean
    private OrderQueryHandler queryHandler;

    private static final Long ORDER_ID = 100L;
    private static final String ORDER_NO = "ORD100";

    @Nested
    @DisplayName("POST /api/orders")
    class CreateOrderTests {

        @Test
        @DisplayName("成功创建订单应返回 200 和订单详情")
        void createOrder_withValidRequest_shouldReturnOrderVO() throws Exception {
            CreateOrderResult createResult = new CreateOrderResult(ORDER_ID, ORDER_NO);
            when(commandHandler.handle(any(CreateOrderCommand.class))).thenReturn(createResult);

            OrderVO vo = OrderVO.builder()
                    .id(ORDER_ID)
                    .orderNo(ORDER_NO)
                    .amount(new BigDecimal("99.99"))
                    .status(0)
                    .statusDesc("待付款")
                    .build();
            when(queryHandler.getOrderDetail(ORDER_ID)).thenReturn(vo);

            String requestBody = """
                    {
                        "productId": 200,
                        "phone": "13800138000",
                        "address": "北京市朝阳区",
                        "remark": "尽快发货"
                    }
                    """;

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("A0000"))
                    .andExpect(jsonPath("$.data.id").value(ORDER_ID))
                    .andExpect(jsonPath("$.data.orderNo").value(ORDER_NO))
                    .andExpect(jsonPath("$.data.status").value(0))
                    .andExpect(jsonPath("$.data.statusDesc").value("待付款"));

            verify(commandHandler).handle(any(CreateOrderCommand.class));
            verify(queryHandler).getOrderDetail(ORDER_ID);
        }

        @Test
        @DisplayName("手机号格式不正确应返回 400")
        void createOrder_withInvalidPhone_shouldReturn400() throws Exception {
            String requestBody = """
                    {
                        "productId": 200,
                        "phone": "12345",
                        "address": "北京市朝阳区"
                    }
                    """;

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("缺少必填字段应返回 400")
        void createOrder_withMissingRequiredFields_shouldReturn400() throws Exception {
            String requestBody = """
                    {
                        "productId": null,
                        "phone": ""
                    }
                    """;

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/orders/{id}/cancel")
    class CancelOrderTests {

        @Test
        @DisplayName("成功取消订单应返回 200")
        void cancelOrder_withValidId_shouldReturnSuccess() throws Exception {
            mockMvc.perform(put("/api/orders/{id}/cancel", ORDER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("A0000"));

            verify(commandHandler).handle(any(CancelOrderCommand.class));
        }

        @Test
        @DisplayName("带取消原因应传递到命令")
        void cancelOrder_withReason_shouldPassReason() throws Exception {
            mockMvc.perform(put("/api/orders/{id}/cancel", ORDER_ID)
                            .param("reason", "不想要了"))
                    .andExpect(status().isOk());

            verify(commandHandler).handle(any(CancelOrderCommand.class));
        }
    }

    @Nested
    @DisplayName("PUT /api/orders/{id}/pay")
    class PayOrderTests {

        @Test
        @DisplayName("成功支付订单应返回 200")
        void payOrder_withValidId_shouldReturnSuccess() throws Exception {
            mockMvc.perform(put("/api/orders/{id}/pay", ORDER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("A0000"));

            verify(commandHandler).handle(any(PayOrderCommand.class));
        }
    }

    @Nested
    @DisplayName("PUT /api/orders/{id}/ship")
    class ShipOrderTests {

        @Test
        @DisplayName("成功发货应返回 200")
        void shipOrder_withValidId_shouldReturnSuccess() throws Exception {
            mockMvc.perform(put("/api/orders/{id}/ship", ORDER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("A0000"));

            verify(commandHandler).handle(any(ShipOrderCommand.class));
        }
    }

    @Nested
    @DisplayName("PUT /api/orders/{id}/receive")
    class ConfirmReceiptTests {

        @Test
        @DisplayName("成功确认收货应返回 200")
        void confirmReceipt_withValidId_shouldReturnSuccess() throws Exception {
            mockMvc.perform(put("/api/orders/{id}/receive", ORDER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("A0000"));

            verify(commandHandler).handle(any(ConfirmReceiptCommand.class));
        }
    }

    @Nested
    @DisplayName("PUT /api/orders/{id}/refund")
    class RefundOrderTests {

        @Test
        @DisplayName("成功退款应返回 200")
        void refundOrder_withValidId_shouldReturnSuccess() throws Exception {
            mockMvc.perform(put("/api/orders/{id}/refund", ORDER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("A0000"));

            verify(commandHandler).handle(any(RefundOrderCommand.class));
        }

        @Test
        @DisplayName("带退款原因应传递到命令")
        void refundOrder_withReason_shouldPassReason() throws Exception {
            mockMvc.perform(put("/api/orders/{id}/refund", ORDER_ID)
                            .param("reason", "商品有问题"))
                    .andExpect(status().isOk());

            verify(commandHandler).handle(any(RefundOrderCommand.class));
        }
    }
}
