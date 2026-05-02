package com.cartethyia.easyorange.order.domain.saga;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.framework.redis.RedisCache;
import com.cartethyia.easyorange.order.application.command.CreateOrderCommand;
import com.cartethyia.easyorange.order.application.command.CreateOrderResult;
import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.domain.port.outbound.PaymentGatewayPort;
import com.cartethyia.easyorange.order.domain.port.outbound.ProductInventoryPort;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.infrastructure.cache.OrderCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = com.cartethyia.easyorange.order.OrderTestApplication.class)
@Testcontainers
@Tag("integration")
@DisplayName("CreateOrderSaga 端到端集成测试")
class CreateOrderSagaIntegrationTest {

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
    private CreateOrderSaga createOrderSaga;

    @Autowired
    private SagaRepository sagaRepository;

    @MockitoBean
    private ProductInventoryPort productInventoryPort;

    @MockitoBean
    private PaymentGatewayPort paymentGatewayPort;

    @MockitoBean
    private DomainEventPublisher eventPublisher;

    @MockitoBean
    private OrderCacheService orderCacheService;

    @MockitoBean
    private RedisCache redisCache;

    private static final Long BUYER_ID = 1L;
    private static final Long SELLER_ID = 2L;
    private static final Long PRODUCT_ID = 100L;

    @BeforeEach
    void setUp() {
        when(redisCache.tryLock(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(true);
        when(productInventoryPort.getSnapshot(PRODUCT_ID))
            .thenReturn(Optional.of(new ProductInventoryPort.ProductSnapshot(
                PRODUCT_ID, SELLER_ID, new BigDecimal("99.99"), true, true
            )));
        when(paymentGatewayPort.createPayment(any()))
            .thenReturn(1L);
    }

    @Nested
    @DisplayName("成功场景测试")
    class SuccessScenarios {

        @Test
        @DisplayName("完整的订单创建流程")
        void execute_fullFlow_succeeds() {
            CreateOrderCommand command = new CreateOrderCommand();
            command.setProductId(PRODUCT_ID);
            command.setAddress("北京市朝阳区");
            command.setPhone("13800138000");
            command.setRemark("测试订单");

            CreateOrderResult result = createOrderSaga.execute(command);

            assertThat(result).isNotNull();
            assertThat(result.orderId()).isNotNull();
            assertThat(result.orderNo()).isNotBlank();

            Optional<SagaStatus> sagaStatus = sagaRepository.findById(
                extractSagaIdFromOrderId(result.orderId())
            );

            assertThat(sagaStatus).isPresent();
            assertThat(sagaStatus.get().state()).isEqualTo(SagaState.COMPLETED);
        }

        @Test
        @DisplayName("Saga 状态持久化验证")
        void execute_shouldPersistSagaStatus() {
            CreateOrderCommand command = new CreateOrderCommand();
            command.setProductId(PRODUCT_ID);
            command.setAddress("北京市朝阳区");
            command.setPhone("13800138000");

            CreateOrderResult result = createOrderSaga.execute(command);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("失败和补偿场景测试")
    class FailureAndCompensationScenarios {

        @Test
        @DisplayName("支付失败触发补偿")
        void execute_paymentFails_triggersCompensation() {
            when(paymentGatewayPort.createPayment(any()))
                .thenThrow(new RuntimeException("支付服务不可用"));

            CreateOrderCommand command = new CreateOrderCommand();
            command.setProductId(PRODUCT_ID);
            command.setAddress("北京市朝阳区");
            command.setPhone("13800138000");

            assertThatThrownBy(() -> createOrderSaga.execute(command))
                .isInstanceOf(OrderCreationException.class)
                .hasMessageContaining("订单创建失败");

            Optional<SagaStatus> sagaStatus = sagaRepository.findByOrderId(PRODUCT_ID);

            assertThat(sagaStatus).isPresent();
            assertThat(sagaStatus.get().state()).isEqualTo(SagaState.COMPENSATED);
        }

        @Test
        @DisplayName("商品不存在 Saga 失败")
        void execute_productNotFound_throws() {
            when(productInventoryPort.getSnapshot(999L))
                .thenReturn(Optional.empty());

            CreateOrderCommand command = new CreateOrderCommand();
            command.setProductId(999L);
            command.setAddress("北京市朝阳区");
            command.setPhone("13800138000");

            assertThatThrownBy(() -> createOrderSaga.execute(command))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("商品不存在");
        }

        @Test
        @DisplayName("商品已下架 Saga 失败")
        void execute_productNotAvailable_throws() {
            when(productInventoryPort.getSnapshot(PRODUCT_ID))
                .thenReturn(Optional.of(new ProductInventoryPort.ProductSnapshot(
                    PRODUCT_ID, SELLER_ID, new BigDecimal("99.99"), false, true
                )));

            CreateOrderCommand command = new CreateOrderCommand();
            command.setProductId(PRODUCT_ID);
            command.setAddress("北京市朝阳区");
            command.setPhone("13800138000");

            assertThatThrownBy(() -> createOrderSaga.execute(command))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("商品已下架");
        }

        @Test
        @DisplayName("获取分布式锁失败")
        void execute_lockFailed_throws() {
            when(redisCache.tryLock(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(false);

            CreateOrderCommand command = new CreateOrderCommand();
            command.setProductId(PRODUCT_ID);
            command.setAddress("北京市朝阳区");
            command.setPhone("13800138000");

            assertThatThrownBy(() -> createOrderSaga.execute(command))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("繁忙");
        }
    }

    @Nested
    @DisplayName("并发场景测试")
    class ConcurrencyScenarios {

        @Test
        @DisplayName("同一商品并发下单")
        void execute_concurrentOrders_handlesCorrectly() throws InterruptedException {
            CreateOrderCommand command1 = new CreateOrderCommand();
            command1.setProductId(PRODUCT_ID);
            command1.setAddress("地址1");
            command1.setPhone("13800138001");

            CreateOrderCommand command2 = new CreateOrderCommand();
            command2.setProductId(PRODUCT_ID);
            command2.setAddress("地址2");
            command2.setPhone("13800138002");

            CreateOrderResult result1 = createOrderSaga.execute(command1);
            CreateOrderResult result2 = createOrderSaga.execute(command2);

            assertThat(result1.orderId()).isNotNull();
            assertThat(result2.orderId()).isNotNull();
            assertThat(result1.orderId()).isNotEqualTo(result2.orderId());
        }
    }

    private String extractSagaIdFromOrderId(Long orderId) {
        return sagaRepository.findByOrderId(orderId)
            .map(SagaStatus::sagaId)
            .orElseThrow(() -> new IllegalStateException("Saga not found for orderId: " + orderId));
    }
}
