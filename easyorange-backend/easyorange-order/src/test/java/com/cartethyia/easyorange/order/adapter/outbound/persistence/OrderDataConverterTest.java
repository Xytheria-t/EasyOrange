package com.cartethyia.easyorange.order.adapter.outbound.persistence;

import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.readmodel.OrderItemReadModel;
import com.cartethyia.easyorange.order.domain.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.valueobject.Address;
import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.OrderItem;
import com.cartethyia.easyorange.order.domain.valueobject.OrderNo;
import com.cartethyia.easyorange.order.domain.valueobject.PaymentStatus;
import com.cartethyia.easyorange.order.domain.valueobject.Phone;
import com.cartethyia.easyorange.order.domain.valueobject.ProductId;
import com.cartethyia.easyorange.order.domain.valueobject.UserId;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OrderDataConverter 单元测试")
class OrderDataConverterTest {

    private final OrderDataConverter converter = new OrderDataConverter(createObjectMapper());

    private static JsonMapper createObjectMapper() {
        return JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();
    }

    private static final String ID = "100";
    private static final String ORDER_NO = "ORD100";
    private static final String BUYER_ID = "1";
    private static final String SELLER_ID = "2";
    private static final String PRODUCT_ID = "200";
    private static final BigDecimal AMOUNT = new BigDecimal("99.99");
    private static final Integer STATUS = OrderStatus.PENDING_PAYMENT.getCode();
    private static final String ADDRESS = "北京市朝阳区建国路88号";
    private static final String PHONE = "13800138000";
    private static final String REMARK = "尽快发货";
    private static final String CANCEL_REASON = null;
    private static final LocalDateTime CANCEL_TIME = null;
    private static final LocalDateTime CREATE_TIME = LocalDateTime.of(2026, 5, 1, 10, 0);
    private static final LocalDateTime UPDATE_TIME = LocalDateTime.of(2026, 5, 1, 12, 0);

    private OrderDO createOrderDO() {
        OrderDO orderDO = OrderDO.builder()
                .id(ID)
                .orderNo(ORDER_NO)
                .buyerId(BUYER_ID)
                .sellerId(SELLER_ID)
                .totalAmount(AMOUNT)
                .status(STATUS)
                .paymentStatus(0)
                .address(ADDRESS)
                .phone(PHONE)
                .remark(REMARK)
                .cancelReason(CANCEL_REASON)
                .cancelTime(CANCEL_TIME)
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

    private OrderAggregate createAggregate() {
        return OrderAggregate.from(
                OrderId.of(ID), OrderNo.of(ORDER_NO),
                UserId.of(BUYER_ID), UserId.of(SELLER_ID), itemForTest(),
                Money.of(AMOUNT), OrderStatus.PENDING_PAYMENT, PaymentStatus.UNPAID,
                Address.of(ADDRESS), Phone.of(PHONE), REMARK, CANCEL_REASON, CANCEL_TIME
        );
    }

    @Nested
    @DisplayName("toDataObject")
    class ToDataObjectTests {

        @Test
        @DisplayName("应将聚合根正确映射为数据对象")
        void toDataObject_shouldMapAllFields() {
            OrderAggregate aggregate = createAggregate();

            OrderDO orderDO = converter.toDataObject(aggregate);

            assertThat(orderDO).isNotNull();
            assertThat(orderDO.getId()).isEqualTo(ID);
            assertThat(orderDO.getOrderNo()).isEqualTo(ORDER_NO);
            assertThat(orderDO.getBuyerId()).isEqualTo(BUYER_ID);
            assertThat(orderDO.getSellerId()).isEqualTo(SELLER_ID);
            assertThat(orderDO.getTotalAmount()).isEqualByComparingTo(AMOUNT);
            assertThat(orderDO.getStatus()).isEqualTo(STATUS);
            assertThat(orderDO.getPaymentStatus()).isEqualTo(0);
            assertThat(orderDO.getAddress()).isEqualTo(ADDRESS);
            assertThat(orderDO.getPhone()).isEqualTo(PHONE);
            assertThat(orderDO.getRemark()).isEqualTo(REMARK);
            assertThat(orderDO.getCancelReason()).isNull();
            assertThat(orderDO.getCancelTime()).isNull();
        }

        @Test
        @DisplayName("null 输入应快速失败")
        void toDataObject_withNull_shouldThrow() {
            assertThatThrownBy(() -> converter.toDataObject(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("toAggregate")
    class ToAggregateTests {

        @Test
        @DisplayName("应将数据对象正确还原为聚合根")
        void toAggregate_shouldReconstructFullAggregate() {
            OrderDO orderDO = createOrderDO();

            OrderAggregate aggregate = converter.toAggregate(orderDO);

            assertThat(aggregate).isNotNull();
            assertThat(aggregate.id().value()).isEqualTo(ID);
            assertThat(aggregate.orderNo().value()).isEqualTo(ORDER_NO);
            assertThat(aggregate.buyerId().value()).isEqualTo(BUYER_ID);
            assertThat(aggregate.sellerId().value()).isEqualTo(SELLER_ID);
            assertThat(aggregate.items()).isEmpty();
            assertThat(aggregate.totalAmount().value()).isEqualByComparingTo(AMOUNT);
            assertThat(aggregate.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
            assertThat(aggregate.paymentStatus()).isEqualTo(PaymentStatus.UNPAID);
            assertThat(aggregate.address().value()).isEqualTo(ADDRESS);
            assertThat(aggregate.phone().value()).isEqualTo(PHONE);
            assertThat(aggregate.remark()).isEqualTo(REMARK);
            assertThat(aggregate.cancelReason()).isNull();
            assertThat(aggregate.cancelTime()).isNull();
        }

        @Test
        @DisplayName("null 输入应快速失败")
        void toAggregate_withNull_shouldThrow() {
            assertThatThrownBy(() -> converter.toAggregate(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("toReadModel")
    class ToReadModelTests {

        @Test
        @DisplayName("应将数据对象正确映射为读模型")
        void toReadModel_shouldMapToReadModel() {
            OrderDO orderDO = createOrderDO();

            OrderReadModel readModel = converter.toReadModel(orderDO);

            assertThat(readModel).isNotNull();
            assertThat(readModel.id()).isEqualTo(ID);
            assertThat(readModel.orderNo()).isEqualTo(ORDER_NO);
            assertThat(readModel.buyerId()).isEqualTo(BUYER_ID);
            assertThat(readModel.sellerId()).isEqualTo(SELLER_ID);
            assertThat(readModel.items()).isEmpty();
            assertThat(readModel.totalAmount()).isEqualByComparingTo(AMOUNT);
            assertThat(readModel.status()).isEqualTo(STATUS);
            assertThat(readModel.statusDesc()).isEqualTo(OrderStatus.getDescByCode(STATUS));
            assertThat(readModel.paymentStatus()).isEqualTo(0);
            assertThat(readModel.address()).isEqualTo(ADDRESS);
            assertThat(readModel.phone()).isEqualTo(PHONE);
            assertThat(readModel.remark()).isEqualTo(REMARK);
            assertThat(readModel.createTime()).isEqualTo(CREATE_TIME);
            assertThat(readModel.updateTime()).isEqualTo(UPDATE_TIME);
        }

        @Test
        @DisplayName("null 输入应快速失败")
        void toReadModel_withNull_shouldThrow() {
            assertThatThrownBy(() -> converter.toReadModel(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("已取消订单的状态描述应正确")
        void toReadModel_withCancelledOrder_shouldHaveCorrectStatusDesc() {
            OrderDO orderDO = createOrderDO();
            orderDO.setStatus(OrderStatus.CANCELLED.getCode());

            OrderReadModel readModel = converter.toReadModel(orderDO);

            assertThat(readModel.statusDesc()).isEqualTo("已取消");
        }
    }

    @Nested
    @DisplayName("item 转换方法")
    class ItemConversionTests {

        @Test
        @DisplayName("toItemDO 应将 OrderItem 正确映射")
        void toItemDO_shouldMapAllFields() {
            OrderItem item = itemForTest().get(0);
            OrderItemDO itemDO = converter.toItemDO(ID, item);

            assertThat(itemDO).isNotNull();
            assertThat(itemDO.getId()).isEqualTo(item.id());
            assertThat(itemDO.getOrderId()).isEqualTo(ID);
            assertThat(itemDO.getProductId()).isEqualTo(item.productId().value());
            assertThat(itemDO.getUnitPrice()).isEqualByComparingTo(item.unitPrice().value());
            assertThat(itemDO.getQuantity()).isEqualTo(item.quantity());
            assertThat(itemDO.getSubtotal()).isEqualByComparingTo(item.subtotal().value());
            assertThat(itemDO.getProductSnapshot()).isNotNull();
        }

        @Test
        @DisplayName("toItemDO null 输入应快速失败")
        void toItemDO_withNull_shouldThrow() {
            assertThatThrownBy(() -> converter.toItemDO(ID, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("toItemReadModel 应将 OrderItemDO 正确映射")
        void toItemReadModel_shouldMapAllFields() {
            OrderItemDO itemDO = OrderItemDO.builder()
                    .id("1").orderId(ID).productId(PRODUCT_ID)
                    .productSnapshot("{}")
                    .unitPrice(AMOUNT).quantity(1).subtotal(AMOUNT)
                    .build();

            OrderItemReadModel readModel = converter.toItemReadModel(itemDO);

            assertThat(readModel).isNotNull();
            assertThat(readModel.itemId()).isEqualTo("1");
            assertThat(readModel.productId()).isEqualTo(PRODUCT_ID);
            assertThat(readModel.unitPrice()).isEqualByComparingTo(AMOUNT);
            assertThat(readModel.quantity()).isEqualTo(1);
            assertThat(readModel.subtotal()).isEqualByComparingTo(AMOUNT);
        }

        @Test
        @DisplayName("toItemReadModel null 输入应快速失败")
        void toItemReadModel_withNull_shouldThrow() {
            assertThatThrownBy(() -> converter.toItemReadModel(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("roundtrip")
    class RoundtripTests {

        @Test
        @DisplayName("聚合根 → DO → 聚合根 应保持数据一致")
        void roundtrip_shouldPreserveAllData() {
            OrderAggregate original = createAggregate();

            OrderDO orderDO = converter.toDataObject(original);
            OrderAggregate restored = converter.toAggregate(orderDO);

            assertThat(restored.id().value()).isEqualTo(original.id().value());
            assertThat(restored.orderNo().value()).isEqualTo(original.orderNo().value());
            assertThat(restored.buyerId().value()).isEqualTo(original.buyerId().value());
            assertThat(restored.sellerId().value()).isEqualTo(original.sellerId().value());
            assertThat(restored.items()).isEmpty();
            assertThat(restored.totalAmount().value()).isEqualByComparingTo(original.totalAmount().value());
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

            OrderAggregate aggregate = converter.toAggregate(original);
            OrderDO converted = converter.toDataObject(aggregate);

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
