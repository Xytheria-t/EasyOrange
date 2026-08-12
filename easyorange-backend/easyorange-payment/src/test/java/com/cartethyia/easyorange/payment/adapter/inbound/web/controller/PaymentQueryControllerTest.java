package com.cartethyia.easyorange.payment.adapter.inbound.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.security.AuthUser;
import com.cartethyia.easyorange.payment.adapter.inbound.web.assembler.PaymentViewAssembler;
import com.cartethyia.easyorange.payment.adapter.inbound.web.request.QueryPaymentRequest;
import com.cartethyia.easyorange.payment.adapter.inbound.web.response.PaymentResponse;
import com.cartethyia.easyorange.payment.application.query.PaymentListQuery;
import com.cartethyia.easyorange.payment.application.query.PaymentQueryHandler;
import com.cartethyia.easyorange.payment.domain.aggregate.Payment;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentReconstructSpec;
import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentQueryController 测试")
class PaymentQueryControllerTest {

    @Mock
    private PaymentQueryHandler queryHandler;

    private final PaymentViewAssembler assembler = new PaymentViewAssembler();

    private PaymentQueryController controller;

    private static final String USER_ID = "3001";

    private static AuthUser currentUser() {
        return new AuthUser(USER_ID, "tester");
    }

    @BeforeEach
    void setUp() {
        controller = new PaymentQueryController(queryHandler, assembler);
    }

    private Payment aggregate() {
        var spec = new PaymentReconstructSpec(
                "1001",
                "PAY123",
                "2001",
                "3001",
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                PaymentMethod.WECHAT,
                PaymentStatus.SUCCESS,
                "TXN_1",
                null,
                null,
                null,
                null,
                null,
                0);
        return Payment.from(spec);
    }

    @Nested
    @DisplayName("getById")
    class GetByIdTests {

        @Test
        @DisplayName("按 ID 查询返回支付响应")
        void getById_returnsResponse() {
            when(queryHandler.getPaymentById("1001")).thenReturn(aggregate());

            Result<PaymentResponse> result = controller.getById("1001");

            assertThat(result.data().getId()).isEqualTo("1001");
            assertThat(result.data().getStatus()).isEqualTo("SUCCESS");
        }
    }

    @Nested
    @DisplayName("getByOrderId")
    class GetByOrderIdTests {

        @Test
        @DisplayName("按订单 ID 查询")
        void getByOrderId_returnsResponse() {
            when(queryHandler.getPaymentByOrderId("2001")).thenReturn(aggregate());

            Result<PaymentResponse> result = controller.getByOrderId("2001");

            assertThat(result.data().getOrderId()).isEqualTo("2001");
        }
    }

    @Nested
    @DisplayName("getStatus")
    class GetStatusTests {

        @Test
        @DisplayName("返回状态响应")
        void getStatus_returnsStatus() {
            when(queryHandler.getPaymentById("1001")).thenReturn(aggregate());

            Result<PaymentQueryController.PaymentStatusResponse> result = controller.getStatus("1001");

            assertThat(result.data().status()).isEqualTo("已支付");
            assertThat(result.data().paymentMethod()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("getMyPayments / queryPayments")
    class ListPaymentsTests {

        @Test
        @DisplayName("我的支付记录 - 空状态解析为 null 查询全部")
        void getMyPayments_blankStatus_queriesAll() {
            QueryPaymentRequest request =
                    QueryPaymentRequest.builder().pageNum(1).pageSize(10).build();
            when(queryHandler.getMyPayments(eq(USER_ID), any(PaymentListQuery.class)))
                    .thenReturn(PageResult.of(List.of(aggregate()), 1L, 1, 10));

            Result<PageResult<PaymentResponse>> result = controller.getMyPayments(currentUser(), request);

            assertThat(result.data().records()).hasSize(1);
            assertThat(result.data().total()).isEqualTo(1L);
        }

        @Test
        @DisplayName("管理端查询 - 状态码解析")
        void queryPayments_resolvesStatus() {
            QueryPaymentRequest request = QueryPaymentRequest.builder()
                    .userId("3001")
                    .status("SUCCESS")
                    .pageNum(1)
                    .pageSize(10)
                    .build();
            when(queryHandler.queryPayments(any(PaymentListQuery.class)))
                    .thenReturn(PageResult.of(List.of(aggregate()), 5L, 1, 10));

            Result<PageResult<PaymentResponse>> result = controller.queryPayments(request);

            assertThat(result.data().total()).isEqualTo(5L);
        }
    }
}
