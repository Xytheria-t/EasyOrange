package com.cartethyia.easyorange.framework.cache;

import com.cartethyia.easyorange.framework.exception.CacheTypeMismatchException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

@Testcontainers
@SpringBootTest(classes = RedisCacheImplIntegrationTest.TestRedisConfig.class)
@Tag("integration")
@DisplayName("RedisCache 集成测试")
class RedisCacheImplIntegrationTest {

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7.4-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @TestConfiguration
    @Import(RedisCache.class)
    static class TestRedisConfig {
        @Bean
        public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);
            template.setKeySerializer(new StringRedisSerializer());
            template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
            template.setHashKeySerializer(new StringRedisSerializer());
            template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
            return template;
        }

        @Bean
        public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
            return new StringRedisTemplate(connectionFactory);
        }
    }

    private static final String TEST_PREFIX = "test:integration:";

    @Autowired
    private RedisCache cache;

    @AfterEach
    void cleanUp() {
        Set<String> keys = cache.keys(TEST_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            cache.delete(keys);
        }
    }

    private String key(String suffix) {
        return TEST_PREFIX + suffix + ":" + UUID.randomUUID();
    }

    // ==================== Basic KV ====================

    @Nested
    @DisplayName("KV 操作")
    class KvOperations {

        @Test
        void setAndGet() {
            var k = key("kv");
            cache.set(k, "hello");
            assertThat((Object) cache.get(k)).isEqualTo("hello");
        }

        @Test
        void getWithType() {
            var k = key("typed");
            cache.set(k, 42);
            assertThat(cache.get(k, Integer.class)).isEqualTo(42);
        }

        @Test
        void getNonExistent() {
            assertThat((Object) cache.get("nonexistent:" + UUID.randomUUID())).isNull();
        }

        @Test
        void delete() {
            var k = key("del");
            cache.set(k, "v");
            assertThat(cache.delete(k)).isTrue();
            assertThat((Object) cache.get(k)).isNull();
        }

        @Test
        void deleteCollection() {
            var k1 = key("delc");
            var k2 = key("delc");
            cache.set(k1, "a");
            cache.set(k2, "b");
            assertThat(cache.delete(List.of(k1, k2))).isEqualTo(2);
        }

        @Test
        void deleteEmptyCollection() {
            assertThat(cache.delete(List.of())).isZero();
        }

        @Test
        void setWithExpiry() {
            var k = key("exp");
            cache.set(k, "x", 1, TimeUnit.SECONDS);
            assertThat((Object) cache.get(k)).isEqualTo("x");
            sleep(1100);
            assertThat((Object) cache.get(k)).isNull();
        }

        @Test
        void setNullThrows() {
            assertThatThrownBy(() -> cache.set(key("null"), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ==================== Key Lifecycle ====================

    @Nested
    @DisplayName("键生命周期")
    class KeyLifecycle {

        @Test
        void expire() {
            var k = key("exp");
            cache.set(k, "v");
            assertThat(cache.expire(k, 10, TimeUnit.SECONDS)).isTrue();
            assertThat(cache.getExpire(k, TimeUnit.SECONDS)).isBetween(1L, 11L);
        }

        @Test
        void expireNonExistent() {
            assertThat(cache.expire("no:" + UUID.randomUUID(), 10, TimeUnit.SECONDS)).isFalse();
        }

        @Test
        void expireNegativeThrows() {
            var k = key("neg");
            cache.set(k, "v");
            assertThatThrownBy(() -> cache.expire(k, -1, TimeUnit.SECONDS))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void hasKey() {
            var k = key("hk");
            assertThat(cache.hasKey(k)).isFalse();
            cache.set(k, "x");
            assertThat(cache.hasKey(k)).isTrue();
        }
    }

    // ==================== Increment ====================

    @Nested
    @DisplayName("自增操作")
    class IncrementOperations {

        @Test
        void incrementDefault() {
            var k = key("inc");
            assertThat(cache.increment(k)).isEqualTo(1L);
            assertThat(cache.increment(k)).isEqualTo(2L);
        }

        @Test
        void incrementWithDelta() {
            var k = key("incd");
            assertThat(cache.increment(k, 5)).isEqualTo(5L);
            assertThat(cache.increment(k, 3)).isEqualTo(8L);
        }
    }

    // ==================== Lock ====================

    @Nested
    @DisplayName("分布式锁")
    class LockOperations {

        @Test
        void tryLockAndUnlock() {
            var k = key("lock");
            assertThat(cache.tryLock(k, "owner", 10, TimeUnit.SECONDS)).isTrue();
            assertThat(cache.unlock(k, "owner")).isTrue();
        }

        @Test
        void lockContention() {
            var k = key("lock_c");
            assertThat(cache.tryLock(k, "a", 10, TimeUnit.SECONDS)).isTrue();
            assertThat(cache.tryLock(k, "b", 10, TimeUnit.SECONDS)).isFalse();
            cache.unlock(k, "a");
        }

        @Test
        void unlockWrongOwner() {
            var k = key("lock_w");
            cache.tryLock(k, "owner", 10, TimeUnit.SECONDS);
            assertThat(cache.unlock(k, "wrong")).isFalse();
            cache.unlock(k, "owner");
        }
    }

    // ==================== Lua ====================

    @Nested
    @DisplayName("Lua 脚本")
    class LuaScript {

        @Test
        void executeLua() {
            var k = key("lua");
            cache.set(k, 5);
            var rs = new org.springframework.data.redis.core.script.DefaultRedisScript<Long>(
                    "return redis.call('INCR', KEYS[1])", Long.class);
            assertThat(cache.executeLuaScript(rs, List.of(k))).isEqualTo(6L);
        }
    }

    // ==================== Type Mismatch ====================

    @Nested
    @DisplayName("类型转换异常")
    class TypeMismatch {

        @Test
        void typeMismatch() {
            var k = key("tm");
            cache.set(k, "not_a_number");
            assertThatThrownBy(() -> cache.get(k, Long.class))
                    .isInstanceOf(CacheTypeMismatchException.class);
        }
    }

    // ==================== SCAN ====================

    @Nested
    @DisplayName("SCAN 通配符")
    class KeysPattern {

        @Test
        void scanPattern() {
            var prefix = TEST_PREFIX + "keys:" + UUID.randomUUID() + ":";
            cache.set(prefix + "a", "1");
            cache.set(prefix + "b", "2");
            assertThat(cache.keys(prefix + "*"))
                    .containsExactlyInAnyOrder(prefix + "a", prefix + "b");
        }
    }

    // ==================== NX ====================

    @Nested
    @DisplayName("SetIfAbsent")
    class SetIfAbsent {

        @Test
        void setIfAbsentNew() {
            var k = key("sia");
            assertThat(cache.setIfAbsent(k, "first", -1, TimeUnit.SECONDS)).isTrue();
            assertThat((Object) cache.get(k)).isEqualTo("first");
        }

        @Test
        void setIfAbsentExisting() {
            var k = key("sia_ex");
            cache.set(k, "original");
            assertThat(cache.setIfAbsent(k, "second", -1, TimeUnit.SECONDS)).isFalse();
        }

        @Test
        void setIfAbsentWithTtl() {
            var k = key("sia_ttl");
            assertThat(cache.setIfAbsent(k, "val", 3, TimeUnit.SECONDS)).isTrue();
            assertThat(cache.getExpire(k, TimeUnit.SECONDS)).isBetween(1L, 4L);
        }
    }

    // ==================== Hash ====================

    @Nested
    @DisplayName("Hash 操作")
    class HashOperations {

        @Test
        void hashPutAll() {
            var k = key("hash");
            cache.hashPutAll(k, Map.of("a", "1", "b", "2"));
            assertThat(cache.hasKey(k)).isTrue();
        }
    }

    private static void sleep(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
