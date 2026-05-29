package com.cartethyia.easyorange.order.adapter.outbound.persistence;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.order.OrderIntegrationTestConfig;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.port.OrderQueryCondition;
import com.cartethyia.easyorange.order.domain.repository.OrderReadRepository;
import com.cartethyia.easyorange.order.domain.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = OrderIntegrationTestConfig.class)
@Testcontainers
@Tag("integration")
@DisplayName("MybatisOrderReadRepository 集成测试")
class MybatisOrderReadRepositoryIntegrationTest {

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
    private OrderReadRepository readRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.execute("DELETE FROM eo_order_item");
        jdbcTemplate.execute("DELETE FROM eo_order");
        insertTestOrders();
    }

    private void insertTestOrders() {
        // Order 1: PENDING_PAYMENT, buyer 10, seller 20
        jdbcTemplate.update(
                "INSERT INTO eo_order (id, order_no, buyer_id, seller_id, total_amount, status, payment_status, address, phone, remark, del_flag, version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0)",
                100L, "ORD100", 10L, 20L, BigDecimal.valueOf(99.99), 0, 0, "北京市", "13800138000", "备注1"
        );

        // Order 2: PAID, buyer 10, seller 21
        jdbcTemplate.update(
                "INSERT INTO eo_order (id, order_no, buyer_id, seller_id, total_amount, status, payment_status, address, phone, remark, del_flag, version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0)",
                101L, "ORD101", 10L, 21L, BigDecimal.valueOf(199.99), 1, 1, "上海市", "13900139000", "备注2"
        );

        // Order 3: SHIPPED, buyer 11, seller 20
        jdbcTemplate.update(
                "INSERT INTO eo_order (id, order_no, buyer_id, seller_id, total_amount, status, payment_status, address, phone, remark, del_flag, version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0)",
                102L, "ORD102", 11L, 20L, BigDecimal.valueOf(299.99), 2, 1, "广州市", "13700137000", "备注3"
        );

        // Order 4: COMPLETED, buyer 12, seller 22
        jdbcTemplate.update(
                "INSERT INTO eo_order (id, order_no, buyer_id, seller_id, total_amount, status, payment_status, address, phone, remark, del_flag, version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0)",
                103L, "ORD103", 12L, 22L, BigDecimal.valueOf(399.99), 3, 1, "深圳市", "13600136000", "备注4"
        );
    }

    @Nested
    @DisplayName("findById 方法")
    class FindByIdTests {

        @Test
        @DisplayName("查询存在的订单返回 ReadModel")
        void findById_existing_returnsReadModel() {
            Optional<OrderReadModel> result = readRepository.findById(OrderId.of(100L));
            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(100L);
            assertThat(result.get().orderNo()).isEqualTo("ORD100");
            assertThat(result.get().buyerId()).isEqualTo(10L);
            assertThat(result.get().sellerId()).isEqualTo(20L);
            assertThat(result.get().status()).isEqualTo(0);
            assertThat(result.get().totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(99.99));
        }

        @Test
        @DisplayName("查询不存在的订单返回空")
        void findById_nonExistent_returnsEmpty() {
            Optional<OrderReadModel> result = readRepository.findById(OrderId.of(999L));
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findPage 方法")
    class FindPageTests {

        @Test
        @DisplayName("分页查询返回正确数据")
        void findPage_returnsPaginatedData() {
            OrderQueryCondition condition = new OrderQueryCondition(
                    null, null, null, null, 1, 2
            );

            PageResult<OrderReadModel> page = readRepository.findPage(condition);
            assertThat(page.records()).hasSize(2);
            assertThat(page.total()).isEqualTo(4);
            assertThat(page.current()).isEqualTo(1);
            assertThat(page.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("按 buyerId 过滤")
        void findPage_filterByBuyerId() {
            OrderQueryCondition condition = new OrderQueryCondition(
                    null, null, 10L, null, 1, 10
            );

            PageResult<OrderReadModel> page = readRepository.findPage(condition);
            assertThat(page.records()).hasSize(2);
            assertThat(page.records()).allMatch(r -> r.buyerId().equals(10L));
        }

        @Test
        @DisplayName("按 sellerId 过滤")
        void findPage_filterBySellerId() {
            OrderQueryCondition condition = new OrderQueryCondition(
                    null, null, null, 20L, 1, 10
            );

            PageResult<OrderReadModel> page = readRepository.findPage(condition);
            assertThat(page.records()).hasSize(2);
            assertThat(page.records()).allMatch(r -> r.sellerId().equals(20L));
        }

        @Test
        @DisplayName("按 status 过滤")
        void findPage_filterByStatus() {
            OrderQueryCondition condition = new OrderQueryCondition(
                    null, 0, null, null, 1, 10
            );

            PageResult<OrderReadModel> page = readRepository.findPage(condition);
            assertThat(page.records()).hasSize(1);
            assertThat(page.records().getFirst().status()).isEqualTo(0);
        }

        @Test
        @DisplayName("按 orderNo 过滤")
        void findPage_filterByOrderNo() {
            OrderQueryCondition condition = new OrderQueryCondition(
                    "ORD101", null, null, null, 1, 10
            );

            PageResult<OrderReadModel> page = readRepository.findPage(condition);
            assertThat(page.records()).hasSize(1);
            assertThat(page.records().getFirst().orderNo()).isEqualTo("ORD101");
        }

        @Test
        @DisplayName("没有匹配数据返回空分页")
        void findPage_noMatch_returnsEmpty() {
            OrderQueryCondition condition = new OrderQueryCondition(
                    "NONEXISTENT", null, null, null, 1, 10
            );

            PageResult<OrderReadModel> page = readRepository.findPage(condition);
            assertThat(page.records()).isEmpty();
            assertThat(page.total()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("countByStatus 方法")
    class CountByStatusTests {

        @Test
        @DisplayName("统计指定状态的订单数量")
        void countByStatus_returnsCorrectCount() {
            long count = readRepository.countByStatus(OrderStatus.PENDING_PAYMENT.getCode());
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("不存在的状态返回 0")
        void countByStatus_noMatch_returnsZero() {
            long count = readRepository.countByStatus(OrderStatus.CANCELLED.getCode());
            assertThat(count).isEqualTo(0);
        }
    }
}
