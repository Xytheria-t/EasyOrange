package com.cartethyia.easyorange.order.application.query.assembler;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.order.application.dto.OrderVO;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.port.ProductQueryPort.ProductDetail;
import com.cartethyia.easyorange.order.domain.readmodel.OrderItemReadModel;
import com.cartethyia.easyorange.order.domain.readmodel.OrderReadModel;
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
    private static final BigDecimal PRODUCT_PRICE = new BigDecimal("99.99");
    private static final String PRODUCT_STATUS = "1";
    private static final List<String> PRODUCT_IMAGES = List.of("http://example.com/img1.jpg");

    private static List<OrderItemReadModel> testItems() {
        return List.of(new OrderItemReadModel("1", PRODUCT_ID, "{}", AMOUNT, 1, AMOUNT));
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
                CREATE_TIME,
                UPDATE_TIME);
    }

    private ProductDetail createProductDetail() {
        return new ProductDetail(PRODUCT_ID, PRODUCT_TITLE, PRODUCT_PRICE, PRODUCT_STATUS, PRODUCT_IMAGES, null, null);
    }

    @Nested
    @DisplayName("toOrderVO")
    class ToOrderVOTests {

        @Test
        @DisplayName("应正确映射所有字段（脱敏模式）")
        void toOrderVO_withMaskSensitive_shouldMapAllFields() {
            OrderReadModel order = createOrder();
            ProductDetail product = createProductDetail();
            Map<String, ProductDetail> productMap = Map.of(PRODUCT_ID, product);

            OrderVO vo = assembler.toOrderVO(order, productMap, true);

            assertThat(vo.getId()).isEqualTo(ORDER_ID);
            assertThat(vo.getOrderNo()).isEqualTo(ORDER_NO);
            assertThat(vo.getBuyerId()).isEqualTo(BUYER_ID);
            assertThat(vo.getSellerId()).isEqualTo(SELLER_ID);
            assertThat(vo.getTotalAmount()).isEqualByComparingTo(AMOUNT);
            assertThat(vo.getSingleItem()).isTrue();
            assertThat(vo.getStatus()).isEqualTo(STATUS);
            assertThat(vo.getStatusDesc()).isEqualTo(STATUS_DESC);
            assertThat(vo.getRemark()).isEqualTo(REMARK);
            assertThat(vo.getCreateTime()).isEqualTo(CREATE_TIME);
            assertThat(vo.getUpdateTime()).isEqualTo(UPDATE_TIME);

            // items
            assertThat(vo.getItems()).hasSize(1);
            assertThat(vo.getItems().get(0).getProductId()).isEqualTo(PRODUCT_ID);
            assertThat(vo.getItems().get(0).getProductName()).isEqualTo(PRODUCT_TITLE);
            assertThat(vo.getItems().get(0).getProductImage()).isEqualTo(PRODUCT_IMAGES.getFirst());
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
            OrderReadModel order = createOrder();
            Map<String, ProductDetail> productMap = Map.of(PRODUCT_ID, createProductDetail());

            OrderVO vo = assembler.toOrderVO(order, productMap, false);

            assertThat(vo.getAddress()).isEqualTo(ADDRESS);
            assertThat(vo.getPhone()).contains("****");
            assertThat(vo.getItems()).hasSize(1);
            assertThat(vo.getItems().get(0).getProductName()).isEqualTo(PRODUCT_TITLE);
        }

        @Test
        @DisplayName("商品不存在时应映射基础字段而不填充商品信息")
        void toOrderVO_withMissingProduct_shouldMapWithoutProductInfo() {
            OrderReadModel order = createOrder();

            OrderVO vo = assembler.toOrderVO(order, Map.of(), true);

            assertThat(vo.getId()).isEqualTo(ORDER_ID);
            assertThat(vo.getItems()).hasSize(1);
            assertThat(vo.getItems().get(0).getProductName()).isEmpty();
            assertThat(vo.getItems().get(0).getProductImage()).isNull();
        }

        @Test
        @DisplayName("商品无图片时应仅设置标题不设图片")
        void toOrderVO_withProductNoImages_shouldSetTitleOnly() {
            OrderReadModel order = createOrder();
            ProductDetail product =
                    new ProductDetail(PRODUCT_ID, PRODUCT_TITLE, PRODUCT_PRICE, PRODUCT_STATUS, List.of(), null, null);
            Map<String, ProductDetail> productMap = Map.of(PRODUCT_ID, product);

            OrderVO vo = assembler.toOrderVO(order, productMap, true);

            assertThat(vo.getItems()).hasSize(1);
            assertThat(vo.getItems().get(0).getProductName()).isEqualTo(PRODUCT_TITLE);
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
                    CREATE_TIME,
                    UPDATE_TIME);

            OrderVO vo = assembler.toOrderVO(order, Map.of(), true);

            assertThat(vo.getAddress()).isNull();
            assertThat(vo.getPhone()).isNull();
            assertThat(vo.getRemark()).isNull();
            assertThat(vo.getItems()).hasSize(1);
            assertThat(vo.getItems().get(0).getProductName()).isEmpty();
        }
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
                            "2", "201", "{}", new BigDecimal("49.99"), 1, new BigDecimal("49.99"))),
                    new BigDecimal("49.99"),
                    OrderStatus.PAID.getCode(),
                    "已付款",
                    PaymentStatus.PAID.getCode(),
                    "上海市浦东新区",
                    "13900139000",
                    "备注2",
                    null,
                    null,
                    LocalDateTime.now(),
                    LocalDateTime.now());

            ProductDetail product1 = createProductDetail();
            ProductDetail product2 =
                    new ProductDetail("201", "商品2", new BigDecimal("49.99"), "1", List.of("img2.jpg"), null, null);
            Map<String, ProductDetail> productMap = Map.of(PRODUCT_ID, product1, "201", product2);

            List<OrderVO> vos = assembler.toOrderVOs(List.of(order1, order2), productMap);

            assertThat(vos).hasSize(2);
            assertThat(vos.get(0).getId()).isEqualTo(ORDER_ID);
            assertThat(vos.get(1).getId()).isEqualTo("101");
            assertThat(vos.get(1).getTotalAmount()).isEqualByComparingTo(new BigDecimal("49.99"));
        }

        @Test
        @DisplayName("空列表应返回空列表")
        void toOrderVOs_withEmptyList_shouldReturnEmptyList() {
            assertThat(assembler.toOrderVOs(List.of(), Map.of())).isEmpty();
        }

        @Test
        @DisplayName("null 输入应返回空列表")
        void toOrderVOs_withNullList_shouldReturnEmptyList() {
            assertThat(assembler.toOrderVOs(null, Map.of())).isEmpty();
        }
    }

    @Nested
    @DisplayName("buildProductMap")
    class BuildProductMapTests {

        @Test
        @DisplayName("应正确构建商品映射")
        void buildProductMap_shouldMapById() {
            ProductDetail p1 = new ProductDetail("1", "商品1", BigDecimal.TEN, "1", List.of(), null, null);
            ProductDetail p2 = new ProductDetail("2", "商品2", BigDecimal.valueOf(20), "1", List.of(), null, null);

            Map<String, ProductDetail> map = assembler.buildProductMap(List.of(p1, p2));

            assertThat(map).hasSize(2);
            assertThat(map.get("1").title()).isEqualTo("商品1");
            assertThat(map.get("2").title()).isEqualTo("商品2");
        }

        @Test
        @DisplayName("空列表应返回空映射")
        void buildProductMap_withEmptyList_shouldReturnEmptyMap() {
            assertThat(assembler.buildProductMap(List.of())).isEmpty();
        }

        @Test
        @DisplayName("null 输入应返回空映射")
        void buildProductMap_withNullList_shouldReturnEmptyMap() {
            assertThat(assembler.buildProductMap(null)).isEmpty();
        }
    }
}
