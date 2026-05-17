package com.cartethyia.easyorange.framework.event.idempotency;

import com.cartethyia.easyorange.framework.config.redis.RedisConfig;
import com.cartethyia.easyorange.framework.redis.RedisCache;
import com.cartethyia.easyorange.framework.redis.impl.RedisCacheImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {RedisConfig.class, RedisCacheImpl.class, EventIdempotencyChecker.class})
@Testcontainers
@Tag("integration")
@DisplayName("EventIdempotencyChecker 集成测试")
class EventIdempotencyCheckerIntegrationTest {

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
    }

    @Autowired
    private EventIdempotencyChecker checker;

    @Autowired
    private RedisCache redisCache;

    @BeforeEach
    void cleanUp() {
        Set<String> keys = redisCache.keys("*");
        keys.forEach(k -> redisCache.delete(k));
    }

    @Nested
    @DisplayName("isDuplicate 方法")
    class IsDuplicateTests {

        @Test
        @DisplayName("新事件返回 false")
        void newEvent_returnsFalse() {
            boolean duplicate = checker.isDuplicate("OrderCreated", "evt-001");
            assertThat(duplicate).isFalse();
        }

        @Test
        @DisplayName("重复事件返回 true")
        void repeatedEvent_returnsTrue() {
            checker.markProcessed("OrderCreated", "evt-001");
            boolean duplicate = checker.isDuplicate("OrderCreated", "evt-001");
            assertThat(duplicate).isTrue();
        }

        @Test
        @DisplayName("不同事件类型不冲突")
        void differentEventType_notDuplicate() {
            checker.markProcessed("OrderCreated", "evt-001");
            boolean duplicate = checker.isDuplicate("OrderPaid", "evt-001");
            assertThat(duplicate).isFalse();
        }

        @Test
        @DisplayName("null 参数返回 false")
        void nullParams_returnsFalse() {
            assertThat(checker.isDuplicate(null, "evt-001")).isFalse();
            assertThat(checker.isDuplicate("OrderCreated", null)).isFalse();
            assertThat(checker.isDuplicate(null, null)).isFalse();
        }
    }

    @Nested
    @DisplayName("tryMark 方法")
    class TryMarkTests {

        @Test
        @DisplayName("首次标记成功返回 true")
        void firstMark_returnsTrue() {
            boolean marked = checker.tryMark("OrderCreated", "evt-002");
            assertThat(marked).isTrue();
        }

        @Test
        @DisplayName("重复标记返回 false")
        void duplicateMark_returnsFalse() {
            assertThat(checker.tryMark("OrderCreated", "evt-003")).isTrue();
            assertThat(checker.tryMark("OrderCreated", "evt-003")).isFalse();
        }

        @Test
        @DisplayName("null 参数返回 true（不检查）")
        void nullParams_returnsTrue() {
            assertThat(checker.tryMark(null, "evt-001")).isTrue();
            assertThat(checker.tryMark("OrderCreated", null)).isTrue();
        }
    }

    @Nested
    @DisplayName("markProcessed 方法")
    class MarkProcessedTests {

        @Test
        @DisplayName("标记后 isDuplicate 返回 true")
        void afterMark_isDuplicate() {
            checker.markProcessed("OrderCreated", "evt-004");
            assertThat(checker.isDuplicate("OrderCreated", "evt-004")).isTrue();
        }

        @Test
        @DisplayName("null 参数不抛异常")
        void nullParams_noException() {
            checker.markProcessed(null, "evt-001");
            checker.markProcessed("OrderCreated", null);
        }
    }

    @Nested
    @DisplayName("remove 方法")
    class RemoveTests {

        @Test
        @DisplayName("移除后 isDuplicate 返回 false")
        void afterRemove_notDuplicate() {
            checker.markProcessed("OrderCreated", "evt-005");
            assertThat(checker.isDuplicate("OrderCreated", "evt-005")).isTrue();

            checker.remove("OrderCreated", "evt-005");
            assertThat(checker.isDuplicate("OrderCreated", "evt-005")).isFalse();
        }

        @Test
        @DisplayName("null 参数不抛异常")
        void nullParams_noException() {
            checker.remove(null, "evt-001");
            checker.remove("OrderCreated", null);
        }
    }

    @Nested
    @DisplayName("幂等 Key 格式")
    class KeyFormatTests {

        @Test
        @DisplayName("幂等 key 格式包含事件类型和 ID")
        void keyFormat_containsEventTypeAndId() {
            checker.markProcessed("OrderShipped", "ship-123");
            boolean duplicate = checker.isDuplicate("OrderShipped", "ship-123");
            assertThat(duplicate).isTrue();

            // Key should be stored under "event:idempotency:OrderShipped:ship-123"
            Set<String> keys = redisCache.keys("event:idempotency:OrderShipped:ship-123");
            assertThat(keys).isNotEmpty();
        }
    }
}
