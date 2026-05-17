package com.cartethyia.easyorange.order.adapter.outbound.persistence;

import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.readmodel.OrderReadModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderDataConverter 单元测试")
class OrderDataConverterTest {

    private final OrderDataConverter converter = new OrderDataConverter();

    private static final Long ID = 100L;
    private static final String ORDER_NO = "ORD100";
    private static final Long BUYER_ID = 1L;
    private static final Long SELLER_ID = 2L;
    private static final Long PRODUCT_ID = 200L;
    private static final BigDecimal AMOUNT = new BigDecimal("99.99");
    private static final Integer STATUS = OrderStatus.PENDING_PAYMENT.getCode();
    private static final Integer PAYMENT_STATUS = 0;
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
                .productId(PRODUCT_ID)
                .amount(AMOUNT)
                .status(STATUS)
                .paymentStatus(PAYMENT_STATUS)
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

    private OrderAggregate createAggregate() {
        return OrderAggregate.fromRaw(
                ID, ORDER_NO, BUYER_ID, SELLER_ID, PRODUCT_ID,
                AMOUNT, STATUS, PAYMENT_STATUS,
                ADDRESS, PHONE, REMARK, CANCEL_REASON, CANCEL_TIME
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
            assertThat(orderDO.getProductId()).isEqualTo(PRODUCT_ID);
            assertThat(orderDO.getAmount()).isEqualByComparingTo(AMOUNT);
            assertThat(orderDO.getStatus()).isEqualTo(STATUS);
            assertThat(orderDO.getPaymentStatus()).isEqualTo(PAYMENT_STATUS);
            assertThat(orderDO.getAddress()).isEqualTo(ADDRESS);
            assertThat(orderDO.getPhone()).isEqualTo(PHONE);
            assertThat(orderDO.getRemark()).isEqualTo(REMARK);
            assertThat(orderDO.getCancelReason()).isNull();
            assertThat(orderDO.getCancelTime()).isNull();
        }

        @Test
        @DisplayName("null 输入应返回 null")
        void toDataObject_withNull_shouldReturnNull() {
            assertThat(converter.toDataObject(null)).isNull();
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
            assertThat(aggregate.productId().value()).isEqualTo(PRODUCT_ID);
            assertThat(aggregate.amount().amount()).isEqualByComparingTo(AMOUNT);
            assertThat(aggregate.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
            assertThat(aggregate.paymentStatus()).isEqualTo(PAYMENT_STATUS);
            assertThat(aggregate.address().value()).isEqualTo(ADDRESS);
            assertThat(aggregate.phone().value()).isEqualTo(PHONE);
            assertThat(aggregate.remark()).isEqualTo(REMARK);
            assertThat(aggregate.cancelReason()).isNull();
            assertThat(aggregate.cancelTime()).isNull();
        }

        @Test
        @DisplayName("null 输入应返回 null")
        void toAggregate_withNull_shouldReturnNull() {
            assertThat(converter.toAggregate(null)).isNull();
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
            assertThat(readModel.productId()).isEqualTo(PRODUCT_ID);
            assertThat(readModel.amount()).isEqualByComparingTo(AMOUNT);
            assertThat(readModel.status()).isEqualTo(STATUS);
            assertThat(readModel.statusDesc()).isEqualTo(OrderStatus.getDescByCode(STATUS));
            assertThat(readModel.paymentStatus()).isEqualTo(PAYMENT_STATUS);
            assertThat(readModel.address()).isEqualTo(ADDRESS);
            assertThat(readModel.phone()).isEqualTo(PHONE);
            assertThat(readModel.remark()).isEqualTo(REMARK);
            assertThat(readModel.createTime()).isEqualTo(CREATE_TIME);
            assertThat(readModel.updateTime()).isEqualTo(UPDATE_TIME);
        }

        @Test
        @DisplayName("null 输入应返回 null")
        void toReadModel_withNull_shouldReturnNull() {
            assertThat(converter.toReadModel(null)).isNull();
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
            assertThat(restored.productId().value()).isEqualTo(original.productId().value());
            assertThat(restored.amount().amount()).isEqualByComparingTo(original.amount().amount());
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
            assertThat(converted.getProductId()).isEqualTo(original.getProductId());
            assertThat(converted.getAmount()).isEqualByComparingTo(original.getAmount());
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
