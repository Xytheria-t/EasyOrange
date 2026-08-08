package com.cartethyia.easyorange.order.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.order.domain.aggregate.Order;
import com.cartethyia.easyorange.order.domain.aggregate.OrderReconstructSpec;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.readmodel.OrderItemReadModel;
import com.cartethyia.easyorange.order.domain.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.valueobject.Address;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.OrderItem;
import com.cartethyia.easyorange.order.domain.valueobject.OrderNo;
import com.cartethyia.easyorange.order.domain.valueobject.PaymentStatus;
import com.cartethyia.easyorange.order.domain.valueobject.Phone;
import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.order.domain.valueobject.UserId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

@DisplayName("OrderDataMapper 单元测试")
class OrderDataMapperTest {

    private final OrderDataMapper mapper = new OrderDataMapper(new ObjectMapper());

    private static final String ID = "100";
    private static final String ORDER_NO = "ORD100";
    private static final String BUYER_ID = "1";
    private static final String SELLER_ID = "2";
    private static final String PRODUCT_ID = "200";
    private static final BigDecimal AMOUNT = new BigDecimal("99.99");
    private static final OrderStatus STATUS = OrderStatus.PENDING_PAYMENT;
    private static final PaymentStatus PAYMENT_STATUS = PaymentStatus.UNPAID;
    private static final String ADDRESS = "北京市朝阳区建国路88号";
    private static final String PHONE = "13800138000";
    private static final String REMARK = "尽快发货";
    private static final LocalDateTime CREATE_TIME = LocalDateTime.of(2026, 5, 1, 10, 0);
    private static final LocalDateTime UPDATE_TIME = LocalDateTime.of(2026, 5, 1, 12, 0);
    private static final String PRODUCT_SNAPSHOT = "{\"productId\":\"200\",\"name\":\"测试商品\",\"price\":99.99}";

    private OrderDO createOrderDO() {
        OrderDO orderDO = OrderDO.builder()
                .id(ID)
                .orderNo(ORDER_NO)
                .buyerId(BUYER_ID)
                .sellerId(SELLER_ID)
                .totalAmount(AMOUNT)
                .status(STATUS)
                .paymentStatus(PAYMENT_STATUS)
                .address(ADDRESS)
                .phone(PHONE)
                .remark(REMARK)
                .build();
        orderDO.setCreateTime(CREATE_TIME);
        orderDO.setUpdateTime(UPDATE_TIME);
        return orderDO;
    }

    private static List<OrderItem> itemForTest() {
        return List.of(OrderItem.builder()
                .id("1")
                .productId(ProductId.of(PRODUCT_ID))
                .unitPrice(Money.of(AMOUNT))
                .quantity(1)
                .subtotal(Money.of(AMOUNT))
                .build());
    }

    private Order createAggregate() {
        return Order.from(new OrderReconstructSpec(
                OrderId.of(ID),
                OrderNo.of(ORDER_NO),
                UserId.of(BUYER_ID),
                UserId.of(SELLER_ID),
                itemForTest(),
                Money.of(AMOUNT),
                STATUS,
                PAYMENT_STATUS,
                Address.of(ADDRESS),
                Phone.of(PHONE),
                REMARK,
                null,
                null));
    }

    @Nested
    @DisplayName("toDataObject")
    class ToDataObjectTests {

        @Test
        @DisplayName("应将聚合根正确映射为数据对象")
        void toDataObject_shouldMapAllFields() {
            Order aggregate = createAggregate();

            OrderDO orderDO = mapper.toDataObject(aggregate);

            assertThat(orderDO).isNotNull();
            assertThat(orderDO.getId()).isEqualTo(ID);
            assertThat(orderDO.getOrderNo()).isEqualTo(ORDER_NO);
            assertThat(orderDO.getBuyerId()).isEqualTo(BUYER_ID);
            assertThat(orderDO.getSellerId()).isEqualTo(SELLER_ID);
            assertThat(orderDO.getTotalAmount()).isEqualByComparingTo(AMOUNT);
            assertThat(orderDO.getStatus()).isEqualTo(STATUS);
            assertThat(orderDO.getPaymentStatus()).isEqualTo(PAYMENT_STATUS);
            assertThat(orderDO.getAddress()).isEqualTo(ADDRESS);
            assertThat(orderDO.getPhone()).isEqualTo(PHONE);
            assertThat(orderDO.getRemark()).isEqualTo(REMARK);
            assertThat(orderDO.getCancelReason()).isNull();
            assertThat(orderDO.getCancelTime()).isNull();
        }
    }

    @Nested
    @DisplayName("toAggregate")
    class ToAggregateTests {

        @Test
        @DisplayName("应将数据对象正确还原为聚合根")
        void toAggregate_shouldReconstructFullAggregate() {
            OrderDO orderDO = createOrderDO();

            Order aggregate = mapper.toAggregate(orderDO);

            assertThat(aggregate).isNotNull();
            assertThat(aggregate.id().value()).isEqualTo(ID);
            assertThat(aggregate.orderNo().value()).isEqualTo(ORDER_NO);
            assertThat(aggregate.buyerId().value()).isEqualTo(BUYER_ID);
            assertThat(aggregate.sellerId().value()).isEqualTo(SELLER_ID);
            assertThat(aggregate.items()).isEmpty();
            assertThat(aggregate.totalAmount().value()).isEqualByComparingTo(AMOUNT);
            assertThat(aggregate.status()).isEqualTo(STATUS);
            assertThat(aggregate.paymentStatus()).isEqualTo(PAYMENT_STATUS);
            assertThat(aggregate.address().value()).isEqualTo(ADDRESS);
            assertThat(aggregate.phone().value()).isEqualTo(PHONE);
            assertThat(aggregate.remark()).isEqualTo(REMARK);
            assertThat(aggregate.cancelReason()).isNull();
            assertThat(aggregate.cancelTime()).isNull();
        }

        @Test
        @DisplayName("携带行项重建应保留行项")
        void toAggregate_withItems_shouldKeepItems() {
            OrderDO orderDO = createOrderDO();

            Order aggregate = mapper.toAggregate(orderDO, itemForTest());

            assertThat(aggregate.items()).hasSize(1);
            assertThat(aggregate.items().getFirst().productId().value()).isEqualTo(PRODUCT_ID);
        }
    }

    @Nested
    @DisplayName("toReadModel")
    class ToReadModelTests {

        @Test
        @DisplayName("应将数据对象正确映射为读模型")
        void toReadModel_shouldMapToReadModel() {
            OrderDO orderDO = createOrderDO();

            OrderReadModel readModel = mapper.toReadModel(orderDO);

            assertThat(readModel).isNotNull();
            assertThat(readModel.id()).isEqualTo(ID);
            assertThat(readModel.orderNo()).isEqualTo(ORDER_NO);
            assertThat(readModel.buyerId()).isEqualTo(BUYER_ID);
            assertThat(readModel.sellerId()).isEqualTo(SELLER_ID);
            assertThat(readModel.items()).isEmpty();
            assertThat(readModel.totalAmount()).isEqualByComparingTo(AMOUNT);
            assertThat(readModel.status()).isEqualTo(STATUS.getCode());
            assertThat(readModel.statusDesc()).isEqualTo(STATUS.getDesc());
            assertThat(readModel.paymentStatus()).isEqualTo(PAYMENT_STATUS.getCode());
            assertThat(readModel.address()).isEqualTo(ADDRESS);
            assertThat(readModel.phone()).isEqualTo(PHONE);
            assertThat(readModel.remark()).isEqualTo(REMARK);
            assertThat(readModel.createTime()).isEqualTo(CREATE_TIME);
            assertThat(readModel.updateTime()).isEqualTo(UPDATE_TIME);
        }

        @Test
        @DisplayName("携带行项的读模型重建应包含行项")
        void toReadModel_withItems_shouldIncludeItems() {
            OrderDO orderDO = createOrderDO();
            var items = List.of(new OrderItemReadModel("1", PRODUCT_ID, PRODUCT_SNAPSHOT, AMOUNT, 1, AMOUNT));

            OrderReadModel readModel = mapper.toReadModel(orderDO, items);

            assertThat(readModel.items()).hasSize(1);
            assertThat(readModel.items().getFirst().productId()).isEqualTo(PRODUCT_ID);
        }

        @Test
        @DisplayName("已取消订单的状态描述应正确")
        void toReadModel_withCancelledOrder_shouldHaveCorrectStatusDesc() {
            OrderDO orderDO = createOrderDO();
            orderDO.setStatus(OrderStatus.CANCELLED);

            OrderReadModel readModel = mapper.toReadModel(orderDO);

            assertThat(readModel.statusDesc()).isEqualTo("已取消");
        }
    }

    @Nested
    @DisplayName("item 转换方法")
    class ItemConversionTests {

        @Test
        @DisplayName("toItemDO 应将 OrderItem 正确映射")
        void toItemDO_shouldMapAllFields() {
            OrderItem item = itemForTest().getFirst();
            OrderItemDO itemDO = mapper.toItemDO(ID, item);

            assertThat(itemDO).isNotNull();
            assertThat(itemDO.getId()).isEqualTo(item.id());
            assertThat(itemDO.getOrderId()).isEqualTo(ID);
            assertThat(itemDO.getProductId()).isEqualTo(item.productId().value());
            assertThat(itemDO.getUnitPrice())
                    .isEqualByComparingTo(item.unitPrice().value());
            assertThat(itemDO.getQuantity()).isEqualTo(item.quantity());
            assertThat(itemDO.getSubtotal())
                    .isEqualByComparingTo(item.subtotal().value());
            assertThat(itemDO.getProductSnapshot()).isNotNull();
        }

        @Test
        @DisplayName("toItemReadModel 应将 OrderItemDO 正确映射")
        void toItemReadModel_shouldMapAllFields() {
            OrderItemDO itemDO = OrderItemDO.builder()
                    .id("1")
                    .orderId(ID)
                    .productId(PRODUCT_ID)
                    .productSnapshot("{}")
                    .unitPrice(AMOUNT)
                    .quantity(1)
                    .subtotal(AMOUNT)
                    .build();

            OrderItemReadModel readModel = mapper.toItemReadModel(itemDO);

            assertThat(readModel).isNotNull();
            assertThat(readModel.itemId()).isEqualTo("1");
            assertThat(readModel.productId()).isEqualTo(PRODUCT_ID);
            assertThat(readModel.unitPrice()).isEqualByComparingTo(AMOUNT);
            assertThat(readModel.quantity()).isEqualTo(1);
            assertThat(readModel.subtotal()).isEqualByComparingTo(AMOUNT);
        }

        @Test
        @DisplayName("toOrderItem 应将 OrderItemDO 正确映射为领域对象")
        void toOrderItem_shouldMapToDomain() {
            OrderItemDO itemDO = OrderItemDO.builder()
                    .id("1")
                    .orderId(ID)
                    .productId(PRODUCT_ID)
                    .productSnapshot("{}")
                    .unitPrice(AMOUNT)
                    .quantity(1)
                    .subtotal(AMOUNT)
                    .build();

            OrderItem item = mapper.toOrderItem(itemDO);

            assertThat(item).isNotNull();
            assertThat(item.id()).isEqualTo("1");
            assertThat(item.productId().value()).isEqualTo(PRODUCT_ID);
            assertThat(item.unitPrice().value()).isEqualByComparingTo(AMOUNT);
            assertThat(item.quantity()).isEqualTo(1);
            assertThat(item.subtotal().value()).isEqualByComparingTo(AMOUNT);
        }
    }

    @Nested
    @DisplayName("roundtrip")
    class RoundtripTests {

        @Test
        @DisplayName("聚合根 → DO → 聚合根 应保持数据一致")
        void roundtrip_shouldPreserveAllData() {
            Order original = createAggregate();

            OrderDO orderDO = mapper.toDataObject(original);
            Order restored = mapper.toAggregate(orderDO);

            assertThat(restored.id().value()).isEqualTo(original.id().value());
            assertThat(restored.orderNo().value()).isEqualTo(original.orderNo().value());
            assertThat(restored.buyerId().value()).isEqualTo(original.buyerId().value());
            assertThat(restored.sellerId().value())
                    .isEqualTo(original.sellerId().value());
            assertThat(restored.items()).isEmpty();
            assertThat(restored.totalAmount().value())
                    .isEqualByComparingTo(original.totalAmount().value());
            assertThat(restored.status()).isEqualTo(original.status());
            assertThat(restored.paymentStatus()).isEqualTo(original.paymentStatus());
            assertThat(restored.address().value()).isEqualTo(original.address().value());
            assertThat(restored.phone().value()).isEqualTo(original.phone().value());
            assertThat(restored.remark()).isEqualTo(original.remark());
            assertThat(restored.cancelReason()).isEqualTo(original.cancelReason());
            assertThat(restored.cancelTime()).isEqualTo(original.cancelTime());
        }

        @Test
        @DisplayName("DO → 聚合根 → DO 应保持数据一致")
        void roundtrip_fromDO_shouldPreserveAllData() {
            OrderDO original = createOrderDO();

            Order aggregate = mapper.toAggregate(original);
            OrderDO converted = mapper.toDataObject(aggregate);

            assertThat(converted.getId()).isEqualTo(original.getId());
            assertThat(converted.getOrderNo()).isEqualTo(original.getOrderNo());
            assertThat(converted.getBuyerId()).isEqualTo(original.getBuyerId());
            assertThat(converted.getSellerId()).isEqualTo(original.getSellerId());
            assertThat(converted.getTotalAmount()).isEqualByComparingTo(original.getTotalAmount());
            assertThat(converted.getStatus()).isEqualTo(original.getStatus());
            assertThat(converted.getPaymentStatus()).isEqualTo(original.getPaymentStatus());
            assertThat(converted.getAddress()).isEqualTo(original.getAddress());
            assertThat(converted.getPhone()).isEqualTo(original.getPhone());
            assertThat(converted.getRemark()).isEqualTo(original.getRemark());
            assertThat(converted.getCancelReason()).isEqualTo(original.getCancelReason());
            assertThat(converted.getCancelTime()).isEqualTo(original.getCancelTime());
        }
    }
}
