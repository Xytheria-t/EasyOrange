package com.cartethyia.easyorange.order.adapter.outbound.persistence;

import com.cartethyia.easyorange.order.OrderIntegrationTestConfig;
import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.port.output.OrderRepository;
import com.cartethyia.easyorange.order.domain.valueobject.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = OrderIntegrationTestConfig.class)
@Testcontainers
@Tag("integration")
@DisplayName("MybatisOrderRepository 集成测试")
class MybatisOrderRepositoryIntegrationTest {

    @Container
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>(
            DockerImageName.parse("mysql:8.0")
    )
            .withDatabaseName("easyorange_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private OrderAggregate createPendingOrder(Long orderId, Long buyerId, Long sellerId) {
        return OrderAggregate.fromRaw(
                orderId,
                "ORD" + orderId,
                buyerId,
                sellerId,
                1L,
                BigDecimal.valueOf(99.99),
                OrderStatus.PENDING_PAYMENT.getCode(),
                0,
                "北京市朝阳区",
                "13800138000",
                "测试备注",
                null,
                null
        );
    }

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.execute("DELETE FROM eo_order");
    }

    @Nested
    @DisplayName("save 方法")
    class SaveTests {

        @Test
        @DisplayName("保存订单成功")
        void save_persistsOrder() {
            OrderAggregate order = createPendingOrder(100L, 10L, 20L);
            orderRepository.save(order);

            Optional<OrderAggregate> found = orderRepository.findById(OrderId.of(100L));
            assertThat(found).isPresent();
            assertThat(found.get().status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
            assertThat(found.get().buyerId().value()).isEqualTo(10L);
            assertThat(found.get().sellerId().value()).isEqualTo(20L);
            assertThat(found.get().amount().amount()).isEqualByComparingTo(BigDecimal.valueOf(99.99));
        }
    }

    @Nested
    @DisplayName("update 方法")
    class UpdateTests {

        @Test
        @DisplayName("更新订单状态成功")
        void update_changesOrderStatus() {
            OrderAggregate order = createPendingOrder(200L, 10L, 20L);
            orderRepository.save(order);

            OrderAggregate.OrderPaidResult result = order.pay();
            orderRepository.update(result.aggregate());

            Optional<OrderAggregate> found = orderRepository.findById(OrderId.of(200L));
            assertThat(found).isPresent();
            assertThat(found.get().status()).isEqualTo(OrderStatus.PAID);
        }
    }

    @Nested
    @DisplayName("findById 方法")
    class FindByIdTests {

        @Test
        @DisplayName("查询存在的订单")
        void findById_existing_returnsOrder() {
            orderRepository.save(createPendingOrder(300L, 10L, 20L));
            Optional<OrderAggregate> found = orderRepository.findById(OrderId.of(300L));
            assertThat(found).isPresent();
        }

        @Test
        @DisplayName("查询不存在的订单返回空")
        void findById_nonExistent_returnsEmpty() {
            Optional<OrderAggregate> found = orderRepository.findById(OrderId.of(999L));
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByBuyerId 方法")
    class FindByBuyerIdTests {

        @Test
        @DisplayName("查询买家的所有订单")
        void findByBuyerId_returnsOrders() {
            orderRepository.save(createPendingOrder(400L, 10L, 20L));
            orderRepository.save(createPendingOrder(401L, 10L, 30L));
            orderRepository.save(createPendingOrder(402L, 11L, 20L));

            List<OrderAggregate> orders = orderRepository.findByBuyerId(UserId.of(10L));
            assertThat(orders).hasSize(2);
        }

        @Test
        @DisplayName("买家无订单返回空列表")
        void findByBuyerId_noOrders_returnsEmpty() {
            List<OrderAggregate> orders = orderRepository.findByBuyerId(UserId.of(999L));
            assertThat(orders).isEmpty();
        }
    }

    @Nested
    @DisplayName("findBySellerId 方法")
    class FindBySellerIdTests {

        @Test
        @DisplayName("查询卖家的所有订单")
        void findBySellerId_returnsOrders() {
            orderRepository.save(createPendingOrder(500L, 10L, 20L));
            orderRepository.save(createPendingOrder(501L, 11L, 20L));

            List<OrderAggregate> orders = orderRepository.findBySellerId(UserId.of(20L));
            assertThat(orders).hasSize(2);
        }
    }

    @Nested
    @DisplayName("findExpiredOrders 方法")
    class FindExpiredOrdersTests {

        @Test
        @DisplayName("查询过期的待付款订单")
        void findExpiredOrders_returnsExpired() {
            // Save an old pending order by directly manipulating the timestamp
            OrderAggregate order = createPendingOrder(600L, 10L, 20L);
            orderRepository.save(order);

            // Create an order with a past create_time via SQL
            jdbcTemplate.update(
                    "UPDATE eo_order SET create_time = DATE_SUB(NOW(), INTERVAL 2 HOUR) WHERE id = 600"
            );

            List<OrderAggregate> expired = orderRepository.findExpiredOrders(60);
            assertThat(expired).isNotEmpty();
            assertThat(expired.getFirst().id().value()).isEqualTo(600L);
        }

        @Test
        @DisplayName("没有过期订单返回空列表")
        void findExpiredOrders_none_returnsEmpty() {
            OrderAggregate order = createPendingOrder(601L, 10L, 20L);
            orderRepository.save(order);

            List<OrderAggregate> expired = orderRepository.findExpiredOrders(60);
            assertThat(expired).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByStatus 方法")
    class FindByStatusTests {

        @Test
        @DisplayName("查询指定状态的订单")
        void findByStatus_returnsMatchingOrders() {
            orderRepository.save(createPendingOrder(700L, 10L, 20L));

            OrderAggregate paidOrder = OrderAggregate.fromRaw(
                    701L, "ORD701", 11L, 21L, 2L,
                    BigDecimal.valueOf(199), OrderStatus.PAID.getCode(), 1,
                    "上海市", "13900139000", null, null, null
            );
            orderRepository.save(paidOrder);

            List<OrderAggregate> pending = orderRepository.findByStatus(OrderStatus.PENDING_PAYMENT.getCode());
            assertThat(pending).hasSize(1);
            assertThat(pending.getFirst().id().value()).isEqualTo(700L);

            List<OrderAggregate> paid = orderRepository.findByStatus(OrderStatus.PAID.getCode());
            assertThat(paid).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findShippedOrdersBefore 方法")
    class FindShippedOrdersBeforeTests {

        @Test
        @DisplayName("查询指定时间之前已发货的订单")
        void findShippedOrdersBefore_returnsMatching() {
            OrderAggregate shipped = OrderAggregate.fromRaw(
                    800L, "ORD800", 10L, 20L, 1L,
                    BigDecimal.valueOf(99), OrderStatus.SHIPPED.getCode(), 1,
                    "地址", "13800138000", null, null, null
            );
            orderRepository.save(shipped);

            // Set update_time to 10 days ago
            jdbcTemplate.update(
                    "UPDATE eo_order SET update_time = DATE_SUB(NOW(), INTERVAL 10 DAY) WHERE id = 800"
            );

            List<OrderAggregate> result = orderRepository.findShippedOrdersBefore(
                    LocalDateTime.now().minusDays(7)
            );
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().id().value()).isEqualTo(800L);
        }

        @Test
        @DisplayName("没有匹配的已发货订单返回空列表")
        void findShippedOrdersBefore_none_returnsEmpty() {
            OrderAggregate shipped = OrderAggregate.fromRaw(
                    801L, "ORD801", 10L, 20L, 1L,
                    BigDecimal.valueOf(99), OrderStatus.SHIPPED.getCode(), 1,
                    "地址", "13800138000", null, null, null
            );
            orderRepository.save(shipped);

            List<OrderAggregate> result = orderRepository.findShippedOrdersBefore(
                    LocalDateTime.now().minusDays(7)
            );
            assertThat(result).isEmpty();
        }
    }
}
