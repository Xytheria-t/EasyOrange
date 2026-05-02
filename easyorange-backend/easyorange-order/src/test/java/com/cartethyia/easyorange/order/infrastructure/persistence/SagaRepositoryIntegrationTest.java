package com.cartethyia.easyorange.order.infrastructure.persistence;

import com.cartethyia.easyorange.order.domain.saga.SagaRepository;
import com.cartethyia.easyorange.order.domain.saga.SagaState;
import com.cartethyia.easyorange.order.domain.saga.SagaStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = com.cartethyia.easyorange.order.OrderTestApplication.class)
@Testcontainers
@Tag("integration")
@DisplayName("SagaRepository 集成测试")
class SagaRepositoryIntegrationTest {

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
    private SagaRepository sagaRepository;

    private String sagaId;
    private SagaStatus sagaStatus;

    @BeforeEach
    void setUp() {
        sagaId = UUID.randomUUID().toString();
        sagaStatus = new SagaStatus(
            sagaId,
            "CREATE_ORDER",
            SagaState.PENDING,
            "INIT",
            "{\"productId\":100,\"buyerId\":1}",
            null,
            null,
            0,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("保存和查询测试")
    class SaveAndFindTests {

        @Test
        @DisplayName("保存 Saga 状态成功")
        void save_shouldPersistSagaStatus() {
            sagaRepository.save(sagaStatus);

            Optional<SagaStatus> found = sagaRepository.findById(sagaId);

            assertThat(found).isPresent();
            SagaStatus retrieved = found.get();
            assertThat(retrieved.sagaId()).isEqualTo(sagaId);
            assertThat(retrieved.sagaType()).isEqualTo("CREATE_ORDER");
            assertThat(retrieved.state()).isEqualTo(SagaState.PENDING);
            assertThat(retrieved.currentStep()).isEqualTo("INIT");
            assertThat(retrieved.payload()).isEqualTo("{\"productId\":100,\"buyerId\":1}");
            assertThat(retrieved.retryCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("根据 ID 查询不存在的 Saga 返回空")
        void findById_nonExistent_returnsEmpty() {
            Optional<SagaStatus> found = sagaRepository.findById("non-existent-id");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("更新测试")
    class UpdateTests {

        @Test
        @DisplayName("更新 Saga 状态成功")
        void update_shouldUpdateSagaStatus() {
            sagaRepository.save(sagaStatus);

            SagaStatus updated = sagaStatus
                .withState(SagaState.ORDER_CREATED)
                .withStep("CREATE_ORDER");

            sagaRepository.update(updated);

            Optional<SagaStatus> found = sagaRepository.findById(sagaId);

            assertThat(found).isPresent();
            assertThat(found.get().state()).isEqualTo(SagaState.ORDER_CREATED);
            assertThat(found.get().currentStep()).isEqualTo("CREATE_ORDER");
        }

        @Test
        @DisplayName("更新 Saga 错误信息和重试次数")
        void update_shouldUpdateErrorInfo() {
            sagaRepository.save(sagaStatus);

            SagaStatus failed = sagaStatus
                .withState(SagaState.FAILED)
                .withError("支付失败：余额不足")
                .withRetry();

            sagaRepository.update(failed);

            Optional<SagaStatus> found = sagaRepository.findById(sagaId);

            assertThat(found).isPresent();
            assertThat(found.get().state()).isEqualTo(SagaState.FAILED);
            assertThat(found.get().errorMessage()).isEqualTo("支付失败：余额不足");
            assertThat(found.get().retryCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("状态转换测试")
    class StateTransitionTests {

        @Test
        @DisplayName("完整的 Saga 生命周期")
        void fullLifecycle_shouldTransitionCorrectly() {
            sagaRepository.save(sagaStatus);

            SagaStatus step1 = sagaStatus.withState(SagaState.ORDER_CREATED).withStep("CREATE_ORDER");
            sagaRepository.update(step1);

            SagaStatus step2 = step1.withState(SagaState.PAYMENT_CREATED).withStep("CREATE_PAYMENT");
            sagaRepository.update(step2);

            SagaStatus completed = step2.withState(SagaState.COMPLETED).withStep("COMPLETED");
            sagaRepository.update(completed);

            Optional<SagaStatus> found = sagaRepository.findById(sagaId);

            assertThat(found).isPresent();
            assertThat(found.get().state()).isEqualTo(SagaState.COMPLETED);
            assertThat(found.get().currentStep()).isEqualTo("COMPLETED");
        }

        @Test
        @DisplayName("Saga 补偿流程")
        void compensationFlow_shouldTransitionCorrectly() {
            sagaRepository.save(sagaStatus);

            SagaStatus step1 = sagaStatus.withState(SagaState.ORDER_CREATED).withStep("CREATE_ORDER");
            sagaRepository.update(step1);

            SagaStatus compensating = step1.withState(SagaState.COMPENSATING).withStep("COMPENSATING");
            sagaRepository.update(compensating);

            SagaStatus compensated = compensating.withState(SagaState.COMPENSATED);
            sagaRepository.update(compensated);

            Optional<SagaStatus> found = sagaRepository.findById(sagaId);

            assertThat(found).isPresent();
            assertThat(found.get().state()).isEqualTo(SagaState.COMPENSATED);
        }
    }

    @Nested
    @DisplayName("并发测试")
    class ConcurrencyTests {

        @Test
        @DisplayName("多次更新 Saga 状态")
        void multipleUpdates_shouldPersistLatestState() {
            sagaRepository.save(sagaStatus);

            for (int i = 1; i <= 5; i++) {
                SagaStatus updated = sagaStatus.withRetry();
                sagaRepository.update(updated);
            }

            Optional<SagaStatus> found = sagaRepository.findById(sagaId);

            assertThat(found).isPresent();
            assertThat(found.get().retryCount()).isEqualTo(5);
        }
    }
}
