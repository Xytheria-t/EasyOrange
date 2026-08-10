package com.cartethyia.easyorange.order.adapter.inbound.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cartethyia.easyorange.order.adapter.inbound.web.assembler.OrderCommandAssembler;
import com.cartethyia.easyorange.order.adapter.inbound.web.dto.request.CancelOrderRequest;
import com.cartethyia.easyorange.order.application.command.CancelOrderCommand;
import com.cartethyia.easyorange.order.application.command.ConfirmReceiptCommand;
import com.cartethyia.easyorange.order.application.command.CreateOrderCommand;
import com.cartethyia.easyorange.order.application.command.CreateOrderResult;
import com.cartethyia.easyorange.order.application.command.OrderCommandHandler;
import com.cartethyia.easyorange.order.application.command.PayOrderCommand;
import com.cartethyia.easyorange.order.application.command.RefundOrderCommand;
import com.cartethyia.easyorange.order.application.command.ShipOrderCommand;
import com.cartethyia.easyorange.order.application.query.OrderQueryHandler;
import com.cartethyia.easyorange.order.application.service.OrderCreationService;
import java.util.List;
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
    private OrderCommandAssembler assembler;

    @MockitoBean
    private OrderCreationService orderCreationService;

    @MockitoBean
    private OrderQueryHandler queryHandler;

    private static final String ORDER_ID = "100";
    private static final String ORDER_NO = "ORD100";

    @Nested
    @DisplayName("POST /api/orders")
    class CreateOrderTests {

        @Test
        @DisplayName("成功创建订单应返回 200 和订单 ID")
        void createOrder_withValidRequest_shouldReturnOrderId() throws Exception {
            CreateOrderResult createResult = new CreateOrderResult(ORDER_ID, ORDER_NO);
            var items = List.of(new CreateOrderCommand.CreateOrderItem("200", 1));
            var command = new CreateOrderCommand(items, "北京市朝阳区", "13800138000", "尽快发货", null);
            when(assembler.toCreateCommand(any())).thenReturn(command);
            when(orderCreationService.createOrder(command)).thenReturn(createResult);

            String requestBody = """
                    {
                        "items": [{"productId": 200, "quantity": 1}],
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
                    .andExpect(jsonPath("$.data").value(ORDER_ID));

            verify(orderCreationService).createOrder(any(CreateOrderCommand.class));
        }

        @Test
        @DisplayName("手机号格式不正确应返回 400")
        void createOrder_withInvalidPhone_shouldReturn400() throws Exception {
            String requestBody = """
                    {
                        "items": [{"productId": 200, "quantity": 1}],
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
                        "items": null,
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
        @DisplayName("缺少取消原因应返回 400")
        void cancelOrder_withoutReason_shouldReturn400() throws Exception {
            mockMvc.perform(put("/api/orders/{id}/cancel", ORDER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("带取消原因应传递到命令")
        void cancelOrder_withReason_shouldPassReason() throws Exception {
            var request = new CancelOrderRequest("不想要了");
            when(assembler.toCancelCommand(eq(ORDER_ID), any())).thenReturn(new CancelOrderCommand(ORDER_ID, "不想要了"));

            mockMvc.perform(put("/api/orders/{id}/cancel", ORDER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"不想要了\"}"))
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
        @DisplayName("缺少退款原因应返回 400")
        void refundOrder_withoutReason_shouldReturn400() throws Exception {
            mockMvc.perform(put("/api/orders/{id}/refund", ORDER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("带退款原因应传递到命令")
        void refundOrder_withReason_shouldPassReason() throws Exception {
            when(assembler.toRefundCommand(eq(ORDER_ID), any())).thenReturn(new RefundOrderCommand(ORDER_ID, "商品有问题"));

            mockMvc.perform(put("/api/orders/{id}/refund", ORDER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"商品有问题\"}"))
                    .andExpect(status().isOk());

            verify(commandHandler).handle(any(RefundOrderCommand.class));
        }
    }
}
