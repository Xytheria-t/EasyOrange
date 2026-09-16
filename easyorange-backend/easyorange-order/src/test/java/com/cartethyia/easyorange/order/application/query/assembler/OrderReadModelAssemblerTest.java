package com.cartethyia.easyorange.order.application.query.assembler;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.order.application.dto.OrderVO;
import com.cartethyia.easyorange.order.application.query.readmodel.OrderItemReadModel;
import com.cartethyia.easyorange.order.application.query.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.valueobject.OrderItemSnapshot;
import com.cartethyia.easyorange.order.domain.valueobject.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("OrderReadModelAssembler 单元测试")
class OrderReadModelAssemblerTest {

    private final OrderReadModelAssembler assembler = new OrderReadModelAssembler();

    private static final String ORDER_ID = "100";
    private static final String ORDER_NO = "ORD100";
    private static final String BUYER_ID = "1";
    private static final String SELLER_ID = "2";
    private static final String PRODUCT_ID = "200";
    private static final BigDecimal AMOUNT = new BigDecimal("99.99");
    private static final String STATUS = OrderStatus.PENDING_PAYMENT.getCode();
    private static final String STATUS_DESC = "待付款";
    private static final String PAYMENT_STATUS = PaymentStatus.UNPAID.getCode();
    private static final String ADDRESS = "北京市朝阳区建国路88号";
    private static final String PHONE = "13800138000";
    private static final String REMARK = "尽快发货";
    private static final String CANCEL_REASON = null;
    private static final LocalDateTime CANCEL_TIME = null;
    private static final LocalDateTime CREATE_TIME = LocalDateTime.of(2026, 5, 1, 10, 0);
    private static final LocalDateTime UPDATE_TIME = LocalDateTime.of(2026, 5, 1, 12, 0);

    private static final String PRODUCT_TITLE = "测试商品";
    private static final String PRODUCT_IMAGE = "http://example.com/img1.jpg";

    /** 展示信息来自下单时的留痕快照，与商品当前状态无关。 */
    private static OrderItemSnapshot snapshot(String productId, String name, String image, BigDecimal price) {
        return OrderItemSnapshot.builder()
                .productId(productId)
                .name(name)
                .image(image)
                .price(Money.of(price))
                .build();
    }

    private static List<OrderItemReadModel> testItems() {
        return List.of(new OrderItemReadModel(
                "1", PRODUCT_ID, snapshot(PRODUCT_ID, PRODUCT_TITLE, PRODUCT_IMAGE, AMOUNT), AMOUNT, 1, AMOUNT));
    }

    private OrderReadModel createOrder() {
        return new OrderReadModel(
                ORDER_ID,
                ORDER_NO,
                BUYER_ID,
                SELLER_ID,
                testItems(),
                AMOUNT,
                STATUS,
                STATUS_DESC,
                PAYMENT_STATUS,
                ADDRESS,
                PHONE,
                REMARK,
                CANCEL_REASON,
                CANCEL_TIME,
                null,
                null,
                CREATE_TIME,
                UPDATE_TIME);
    }

    private static Map<String, String> usernames() {
        return Map.of(BUYER_ID, "认领方小明", SELLER_ID, "资产方张三");
    }

    @Nested
    @DisplayName("toOrderVO")
    class ToOrderVOTests {

        @Test
        @DisplayName("应正确映射所有字段（脱敏模式）")
        void toOrderVO_withMaskSensitive_shouldMapAllFields() {
            OrderVO vo = assembler.toOrderVO(createOrder(), usernames(), true);

            assertThat(vo.getId()).isEqualTo(ORDER_ID);
            assertThat(vo.getOrderNo()).isEqualTo(ORDER_NO);
            assertThat(vo.getBuyerId()).isEqualTo(BUYER_ID);
            assertThat(vo.getBuyerUsername()).isEqualTo("认领方小明");
            assertThat(vo.getSellerId()).isEqualTo(SELLER_ID);
            assertThat(vo.getSellerUsername()).isEqualTo("资产方张三");
            assertThat(vo.getTotalAmount()).isEqualByComparingTo(AMOUNT);
            assertThat(vo.getSingleItem()).isTrue();
            assertThat(vo.getStatus()).isEqualTo(STATUS);
            assertThat(vo.getStatusDesc()).isEqualTo(STATUS_DESC);
            assertThat(vo.getRemark()).isEqualTo(REMARK);
            assertThat(vo.getCreateTime()).isEqualTo(CREATE_TIME);
            assertThat(vo.getUpdateTime()).isEqualTo(UPDATE_TIME);

            // items 的展示信息取自留痕快照
            assertThat(vo.getItems()).hasSize(1);
            assertThat(vo.getItems().get(0).getProductId()).isEqualTo(PRODUCT_ID);
            assertThat(vo.getItems().get(0).getProductName()).isEqualTo(PRODUCT_TITLE);
            assertThat(vo.getItems().get(0).getProductImage()).isEqualTo(PRODUCT_IMAGE);
            assertThat(vo.getItems().get(0).getUnitPrice()).isEqualByComparingTo(AMOUNT);
            assertThat(vo.getItems().get(0).getQuantity()).isEqualTo(1);
            assertThat(vo.getItems().get(0).getSubtotal()).isEqualByComparingTo(AMOUNT);

            // sensitive fields masked
            assertThat(vo.getAddress()).contains("***");
            assertThat(vo.getPhone()).contains("****");
        }

        @Test
        @DisplayName("应正确映射所有字段（非脱敏模式）")
        void toOrderVO_withoutMaskSensitive_shouldMapAllFields() {
            OrderVO vo = assembler.toOrderVO(createOrder(), usernames(), false);

            assertThat(vo.getAddress()).isEqualTo(ADDRESS);
            assertThat(vo.getPhone()).contains("****");
            assertThat(vo.getItems()).hasSize(1);
            assertThat(vo.getItems().get(0).getProductName()).isEqualTo(PRODUCT_TITLE);
        }

        @Test
        @DisplayName("快照缺展示字段时名称为空串、图片为 null")
        void toOrderVO_withBlankSnapshotFields_shouldMapEmptyNameAndNullImage() {
            OrderReadModel order = createOrderWithItems(List.of(new OrderItemReadModel(
                    "1", PRODUCT_ID, snapshot(PRODUCT_ID, null, "", AMOUNT), AMOUNT, 1, AMOUNT)));

            OrderVO vo = assembler.toOrderVO(order, usernames(), true);

            assertThat(vo.getId()).isEqualTo(ORDER_ID);
            assertThat(vo.getItems()).hasSize(1);
            assertThat(vo.getItems().get(0).getProductName()).isEmpty();
            assertThat(vo.getItems().get(0).getProductImage()).isNull();
        }

        @Test
        @DisplayName("null 字段应优雅处理")
        void toOrderVO_withNullFields_shouldHandleGracefully() {
            OrderReadModel order = new OrderReadModel(
                    ORDER_ID,
                    ORDER_NO,
                    BUYER_ID,
                    SELLER_ID,
                    testItems(),
                    AMOUNT,
                    OrderStatus.PENDING_PAYMENT.getCode(),
                    STATUS_DESC,
                    PaymentStatus.UNPAID.getCode(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    CREATE_TIME,
                    UPDATE_TIME);

            OrderVO vo = assembler.toOrderVO(order, usernames(), true);

            assertThat(vo.getAddress()).isNull();
            assertThat(vo.getPhone()).isNull();
            assertThat(vo.getRemark()).isNull();
            assertThat(vo.getItems()).hasSize(1);
            assertThat(vo.getItems().get(0).getProductName()).isEqualTo(PRODUCT_TITLE);
        }
    }

    private OrderReadModel createOrderWithItems(List<OrderItemReadModel> items) {
        return new OrderReadModel(
                ORDER_ID,
                ORDER_NO,
                BUYER_ID,
                SELLER_ID,
                items,
                AMOUNT,
                STATUS,
                STATUS_DESC,
                PAYMENT_STATUS,
                ADDRESS,
                PHONE,
                REMARK,
                CANCEL_REASON,
                CANCEL_TIME,
                null,
                null,
                CREATE_TIME,
                UPDATE_TIME);
    }

    @Nested
    @DisplayName("toOrderVOs")
    class ToOrderVOsTests {

        @Test
        @DisplayName("多个订单应全部映射")
        void toOrderVOs_withMultipleOrders_shouldMapAll() {
            OrderReadModel order1 = createOrder();
            OrderReadModel order2 = new OrderReadModel(
                    "101",
                    "ORD101",
                    "3",
                    "4",
                    List.of(new OrderItemReadModel(
                            "2",
                            "201",
                            snapshot("201", "商品2", "img2.jpg", new BigDecimal("49.99")),
                            new BigDecimal("49.99"),
                            1,
                            new BigDecimal("49.99"))),
                    new BigDecimal("49.99"),
                    OrderStatus.PAID.getCode(),
                    "已付款",
                    PaymentStatus.PAID.getCode(),
                    "上海市浦东新区",
                    "13900139000",
                    "备注2",
                    null,
                    null,
                    null,
                    null,
                    LocalDateTime.now(),
                    LocalDateTime.now());

            List<OrderVO> vos = assembler.toOrderVOs(List.of(order1, order2), usernames());

            assertThat(vos).hasSize(2);
            assertThat(vos.get(0).getId()).isEqualTo(ORDER_ID);
            assertThat(vos.get(0).getItems().get(0).getProductName()).isEqualTo(PRODUCT_TITLE);
            assertThat(vos.get(1).getId()).isEqualTo("101");
            assertThat(vos.get(1).getItems().get(0).getProductName()).isEqualTo("商品2");
            assertThat(vos.get(1).getTotalAmount()).isEqualByComparingTo(new BigDecimal("49.99"));
        }

        @Test
        @DisplayName("空列表应返回空列表")
        void toOrderVOs_withEmptyList_shouldReturnEmptyList() {
            assertThat(assembler.toOrderVOs(List.of(), usernames())).isEmpty();
        }

        @Test
        @DisplayName("null 输入应返回空列表")
        void toOrderVOs_withNullList_shouldReturnEmptyList() {
            assertThat(assembler.toOrderVOs(null, usernames())).isEmpty();
        }
    }
}
