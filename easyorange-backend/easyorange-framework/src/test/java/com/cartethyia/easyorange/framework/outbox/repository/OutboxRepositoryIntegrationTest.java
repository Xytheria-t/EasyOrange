package com.cartethyia.easyorange.framework.outbox.repository;

import com.cartethyia.easyorange.framework.outbox.entity.OutboxMessage;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = FrameworkTestApplication.class)
@Testcontainers
@Tag("integration")
@DisplayName("OutboxRepository 集成测试")
class OutboxRepositoryIntegrationTest {

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
    private OutboxRepository outboxRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.execute("DELETE FROM eo_domain_event");
    }

    private OutboxMessage createPendingMessage() {
        return OutboxMessage.builder()
                .eventId(UUID.randomUUID())
                .aggregateType("Order")
                .aggregateId(100L)
                .eventType("OrderCreatedEvent")
                .payload("{\"orderId\":100}")
                .status(OutboxMessage.STATUS_PENDING)
                .createdAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("save 方法")
    class SaveTests {

        @Test
        @DisplayName("保存消息成功")
        void save_persistsMessage() {
            OutboxMessage message = createPendingMessage();
            outboxRepository.save(message);

            List<OutboxMessage> pending = outboxRepository.findPending(10);
            assertThat(pending).hasSize(1);
            assertThat(pending.getFirst().getEventId()).isEqualTo(message.getEventId());
            assertThat(pending.getFirst().getStatus()).isEqualTo(OutboxMessage.STATUS_PENDING);
        }

        @Test
        @DisplayName("保存多条消息")
        void save_multipleMessages() {
            outboxRepository.save(createPendingMessage());
            outboxRepository.save(createPendingMessage());
            outboxRepository.save(createPendingMessage());

            List<OutboxMessage> pending = outboxRepository.findPending(10);
            assertThat(pending).hasSize(3);
        }
    }

    @Nested
    @DisplayName("findPending 方法")
    class FindPendingTests {

        @Test
        @DisplayName("只返回 PENDING 状态的消息")
        void findPending_onlyReturnsPending() {
            OutboxMessage pending = createPendingMessage();
            outboxRepository.save(pending);

            // Mark one as published
            UUID publishedId = UUID.randomUUID();
            outboxRepository.save(OutboxMessage.builder()
                    .eventId(publishedId)
                    .aggregateType("Order")
                    .aggregateId(200L)
                    .eventType("OrderPaidEvent")
                    .payload("{}")
                    .status(OutboxMessage.STATUS_PUBLISHED)
                    .createdAt(Instant.now())
                    .build());

            List<OutboxMessage> result = outboxRepository.findPending(10);
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getStatus()).isEqualTo(OutboxMessage.STATUS_PENDING);
        }

        @Test
        @DisplayName("limit 限制返回数量")
        void findPending_respectsLimit() {
            for (int i = 0; i < 5; i++) {
                outboxRepository.save(createPendingMessage());
            }

            List<OutboxMessage> result = outboxRepository.findPending(3);
            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("没有待处理消息时返回空列表")
        void findPending_noPending_returnsEmpty() {
            List<OutboxMessage> result = outboxRepository.findPending(10);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("消息按创建时间升序排列")
        void findPending_orderedByCreatedAt() throws InterruptedException {
            OutboxMessage first = createPendingMessage();
            outboxRepository.save(first);

            Thread.sleep(10);

            OutboxMessage second = createPendingMessage();
            outboxRepository.save(second);

            List<OutboxMessage> result = outboxRepository.findPending(10);
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getEventId()).isEqualTo(first.getEventId());
            assertThat(result.get(1).getEventId()).isEqualTo(second.getEventId());
        }
    }

    @Nested
    @DisplayName("markAsPublished 方法")
    class MarkAsPublishedTests {

        @Test
        @DisplayName("标记后状态变为 PUBLISHED")
        void markAsPublished_updatesStatus() {
            OutboxMessage message = createPendingMessage();
            outboxRepository.save(message);

            outboxRepository.markAsPublished(message.getEventId());

            List<OutboxMessage> pending = outboxRepository.findPending(10);
            assertThat(pending).isEmpty();
        }

        @Test
        @DisplayName("标记不存在的 ID 不抛异常")
        void markAsPublished_nonExistent_noException() {
            outboxRepository.markAsPublished(UUID.randomUUID());
        }
    }

    @Nested
    @DisplayName("markAsFailed 方法")
    class MarkAsFailedTests {

        @Test
        @DisplayName("标记后状态变为 FAILED")
        void markAsFailed_updatesStatus() {
            OutboxMessage message = createPendingMessage();
            outboxRepository.save(message);

            outboxRepository.markAsFailed(message.getEventId(), "处理失败: 连接超时");

            List<OutboxMessage> pending = outboxRepository.findPending(10);
            assertThat(pending).isEmpty();
        }

        @Test
        @DisplayName("标记不存在的 ID 不抛异常")
        void markAsFailed_nonExistent_noException() {
            outboxRepository.markAsFailed(UUID.randomUUID(), "error");
        }
    }

    @Nested
    @DisplayName("完整生命周期")
    class FullLifecycleTests {

        @Test
        @DisplayName("保存→发布→不再出现以待处理")
        void fullLifecycle() {
            OutboxMessage message = createPendingMessage();
            outboxRepository.save(message);

            assertThat(outboxRepository.findPending(10)).hasSize(1);

            outboxRepository.markAsPublished(message.getEventId());

            assertThat(outboxRepository.findPending(10)).isEmpty();
        }

        @Test
        @DisplayName("保存→失败→不再出现以待处理")
        void saveThenFail() {
            OutboxMessage message = createPendingMessage();
            outboxRepository.save(message);

            assertThat(outboxRepository.findPending(10)).hasSize(1);

            outboxRepository.markAsFailed(message.getEventId(), "error");

            assertThat(outboxRepository.findPending(10)).isEmpty();
        }
    }
}
