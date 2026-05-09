package com.cartethyia.easyorange.payment.integration;

import com.cartethyia.easyorange.framework.outbox.entity.OutboxMessage;
import com.cartethyia.easyorange.payment.domain.port.output.DomainEventStorePort;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = com.cartethyia.easyorange.payment.PaymentTestApplication.class)
@Testcontainers
@Tag("integration")
@DisplayName("DomainEventStore 集成测试 - UuidTypeHandler 端到端验证")
class DomainEventStoreIntegrationTest {

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

    @MockitoBean
    private com.cartethyia.easyorange.payment.domain.port.output.PaymentGatewayPort paymentGatewayPort;

    @Autowired
    private DomainEventStorePort eventStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.execute("DELETE FROM eo_domain_event WHERE 1=1");
    }

    @Nested
    @DisplayName("store() - UUID 写入验证")
    class StoreTests {

        @Test
        @DisplayName("存储事件时 UUID eventId 应正确写入 CHAR(36) 列")
        void store_shouldPersistUuidToChar36Column() {
            OutboxMessage event = OutboxMessage.builder()
                    .eventId(UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"))
                    .aggregateType("PaymentAggregate")
                    .aggregateId(100L)
                    .eventType("PaymentCreated")
                    .payload("{\"amount\":\"100.00\"}")
                    .status(OutboxMessage.STATUS_PENDING)
                    .createdAt(Instant.now())
                    .build();

            eventStore.store(event);

            String storedEventId = jdbcTemplate.queryForObject(
                    "SELECT event_id FROM eo_domain_event WHERE event_id = ?",
                    String.class,
                    "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
            );
            assertThat(storedEventId).isEqualTo("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
        }

        @Test
        @DisplayName("存储多个事件应各自保留独立 UUID")
        void store_multipleEvents_shouldPersistDistinctUuids() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();

            eventStore.store(buildEvent(id1, "agg", 1L, "E1"));
            eventStore.store(buildEvent(id2, "agg", 2L, "E2"));

            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM eo_domain_event WHERE event_id IN (?, ?)",
                    Long.class, id1.toString(), id2.toString()
            );
            assertThat(count).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("findUnpublished() - UUID 读取验证")
    class FindUnpublishedTests {

        @Test
        @DisplayName("查询未发布事件时应正确还原 UUID")
        void findUnpublished_shouldRestoreUuidCorrectly() {
            UUID expectedId = UUID.randomUUID();
            eventStore.store(buildEvent(expectedId, "agg", 200L, "PaymentCreated"));

            List<OutboxMessage> events = eventStore.findUnpublished(10);

            assertThat(events).hasSize(1);
            assertThat(events.get(0).getEventId()).isEqualTo(expectedId);
        }

        @Test
        @DisplayName("查询结果按创建时间升序排列")
        void findUnpublished_shouldOrderByCreatedAtAsc() {
            Instant earlier = Instant.now().minusSeconds(10);
            Instant later = Instant.now();
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();

            eventStore.store(OutboxMessage.builder()
                    .eventId(id1).aggregateType("agg1").aggregateId(1L)
                    .eventType("E1").payload("{}").status(OutboxMessage.STATUS_PENDING)
                    .createdAt(earlier).build());
            eventStore.store(OutboxMessage.builder()
                    .eventId(id2).aggregateType("agg2").aggregateId(2L)
                    .eventType("E2").payload("{}").status(OutboxMessage.STATUS_PENDING)
                    .createdAt(later).build());

            List<OutboxMessage> events = eventStore.findUnpublished(10);

            assertThat(events).hasSize(2);
            assertThat(events.get(0).getEventId()).isEqualTo(id1);
            assertThat(events.get(1).getEventId()).isEqualTo(id2);
        }

        @Test
        @DisplayName("limit 参数应限制返回数量")
        void findUnpublished_withLimit_shouldReturnAtMostLimitResults() {
            for (int i = 0; i < 5; i++) {
                eventStore.store(buildEvent(UUID.randomUUID(), "agg", (long) i, "E" + i));
            }

            List<OutboxMessage> events = eventStore.findUnpublished(3);

            assertThat(events).hasSizeLessThanOrEqualTo(3);
        }
    }

    @Nested
    @DisplayName("markAsPublished() / markAsFailed() - UUID 条件更新验证")
    class MarkStatusTests {

        @Test
        @DisplayName("标记已发布后状态应为 PUBLISHED")
        void markAsPublished_shouldUpdateStatus() {
            UUID eventId = UUID.randomUUID();
            eventStore.store(buildEvent(eventId, "agg", 1L, "TestEvent"));

            eventStore.markAsPublished(eventId);

            String status = jdbcTemplate.queryForObject(
                    "SELECT status FROM eo_domain_event WHERE event_id = ?",
                    String.class, eventId.toString()
            );
            assertThat(status).isEqualTo(OutboxMessage.STATUS_PUBLISHED);
        }

        @Test
        @DisplayName("标记失败后应记录错误信息")
        void markAsFailed_shouldUpdateStatusAndError() {
            UUID eventId = UUID.randomUUID();
            eventStore.store(buildEvent(eventId, "agg", 1L, "TestEvent"));

            eventStore.markAsFailed(eventId, "connection timeout");

            String status = jdbcTemplate.queryForObject(
                    "SELECT status FROM eo_domain_event WHERE event_id = ?",
                    String.class, eventId.toString()
            );
            String errorMsg = jdbcTemplate.queryForObject(
                    "SELECT error_message FROM eo_domain_event WHERE event_id = ?",
                    String.class, eventId.toString()
            );
            assertThat(status).isEqualTo(OutboxMessage.STATUS_FAILED);
            assertThat(errorMsg).isEqualTo("connection timeout");
        }

        @Test
        @DisplayName("对不存在的 UUID 操作不应抛异常")
        void markAsPublished_nonExistent_shouldNotThrow() {
            UUID nonExistent = UUID.fromString("00000000-0000-0000-0000-000000000000");

            eventStore.markAsPublished(nonExistent);
        }
    }

    private OutboxMessage buildEvent(UUID eventId, String aggregateType, Long aggregateId, String eventType) {
        return OutboxMessage.builder()
                .eventId(eventId)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload("{}")
                .status(OutboxMessage.STATUS_PENDING)
                .createdAt(Instant.now())
                .build();
    }
}
