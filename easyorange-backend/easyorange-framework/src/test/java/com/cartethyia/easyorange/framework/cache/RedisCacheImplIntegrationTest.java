package com.cartethyia.easyorange.framework.cache;

import com.cartethyia.easyorange.framework.exception.CacheTypeMismatchException;
import com.cartethyia.easyorange.framework.cache.impl.RedisCacheImpl;
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
@DisplayName("RedisCacheImpl 集成测试")
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
    @Import(RedisCacheImpl.class)
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
    private RedisCache redisCache;

    @AfterEach
    void cleanUp() {
        Set<String> keys = redisCache.keys(TEST_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisCache.delete(keys);
        }
    }

    private String key(String suffix) {
        return TEST_PREFIX + suffix + ":" + UUID.randomUUID();
    }

    // ==================== Basic Set/Get/Delete ====================

    @Nested
    @DisplayName("基础操作: set/get/delete")
    class BasicOperations {

        @Test
        @DisplayName("set 和 get 字符串")
        void setAndGetString() {
            String k = key("str");
            redisCache.set(k, "hello");
            String val = redisCache.get(k);
            assertThat(val).isEqualTo("hello");
        }

        @Test
        @DisplayName("set 和 get 整数")
        void setAndGetInteger() {
            String k = key("int");
            redisCache.set(k, 42);
            Integer val = redisCache.get(k);
            assertThat(val).isEqualTo(42);
        }

        @Test
        @DisplayName("get 带类型参数")
        void getWithType() {
            String k = key("typed");
            redisCache.set(k, "world");
            String val = redisCache.get(k, String.class);
            assertThat(val).isEqualTo("world");
        }

        @Test
        @DisplayName("get 不存在的键返回 null")
        void getNonExistent() {
            String val = redisCache.get("nonexistent:" + UUID.randomUUID());
            assertThat(val).isNull();
        }

        @Test
        @DisplayName("delete 单个键")
        void deleteSingle() {
            String k = key("del");
            redisCache.set(k, "value");
            assertThat(redisCache.delete(k)).isTrue();
            assertThat((Object) redisCache.get(k)).isNull();
        }

        @Test
        @DisplayName("delete 集合")
        void deleteCollection() {
            String k1 = key("delc1");
            String k2 = key("delc2");
            redisCache.set(k1, "a");
            redisCache.set(k2, "b");
            Long count = redisCache.delete(List.of(k1, k2));
            assertThat(count).isEqualTo(2);
            assertThat((Object) redisCache.get(k1)).isNull();
            assertThat((Object) redisCache.get(k2)).isNull();
        }

        @Test
        @DisplayName("delete 空集合返回 0")
        void deleteEmptyCollection() {
            assertThat(redisCache.delete(List.of())).isZero();
        }

        @Test
        @DisplayName("set 使用过期时间")
        void setWithExpiry() {
            String k = key("exp_set");
            redisCache.set(k, "expires", 1, TimeUnit.SECONDS);
            assertThat((Object) redisCache.get(k)).isEqualTo("expires");
            sleep(1100);
            assertThat((Object) redisCache.get(k)).isNull();
        }

        @Test
        @DisplayName("set 零或负过期时间不设 TTL")
        void setWithNonPositiveTimeout() {
            String k = key("no_ttl");
            redisCache.set(k, "persistent", -1, TimeUnit.SECONDS);
            assertThat(redisCache.getExpire(k, TimeUnit.SECONDS)).isLessThan(0L);
        }

        @Test
        @DisplayName("set null 值抛出异常")
        void setNullThrows() {
            assertThatThrownBy(() -> redisCache.set(key("null"), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ==================== Expire / TTL ====================

    @Nested
    @DisplayName("过期操作: expire/getExpire/hasKey")
    class ExpireOperations {

        @Test
        @DisplayName("expire 设置过期时间")
        void expire() {
            String k = key("exp");
            redisCache.set(k, "value");
            assertThat(redisCache.expire(k, 10, TimeUnit.SECONDS)).isTrue();
            assertThat(redisCache.getExpire(k, TimeUnit.SECONDS)).isBetween(1L, 11L);
        }

        @Test
        @DisplayName("expire 不存在的键返回 false")
        void expireNonExistent() {
            Boolean result = redisCache.expire("no_key:" + UUID.randomUUID(), 10, TimeUnit.SECONDS);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("expire 超时参数为负抛出异常")
        void expireNegativeTimeout() {
            String k = key("neg");
            redisCache.set(k, "v");
            assertThatThrownBy(() -> redisCache.expire(k, -1, TimeUnit.SECONDS))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("hasKey 返回 true/false")
        void hasKey() {
            String k = key("hk");
            assertThat(redisCache.hasKey(k)).isFalse();
            redisCache.set(k, "x");
            assertThat(redisCache.hasKey(k)).isTrue();
        }
    }

    // ==================== Increment / Decrement ====================

    @Nested
    @DisplayName("增减操作: increment/decrement")
    class IncrementDecrement {

        @Test
        @DisplayName("increment 无初始值默认 1")
        void incrementDefault() {
            String k = key("inc");
            Long r = redisCache.increment(k);
            assertThat(r).isEqualTo(1L);
            assertThat(redisCache.increment(k)).isEqualTo(2L);
        }

        @Test
        @DisplayName("increment 指定步长")
        void incrementWithDelta() {
            String k = key("incd");
            assertThat(redisCache.increment(k, 5)).isEqualTo(5L);
            assertThat(redisCache.increment(k, 3)).isEqualTo(8L);
        }

        @Test
        @DisplayName("decrement 无初始值默认 -1")
        void decrementDefault() {
            String k = key("dec");
            assertThat(redisCache.decrement(k)).isEqualTo(-1L);
        }

        @Test
        @DisplayName("decrement 指定步长")
        void decrementWithDelta() {
            String k = key("decd");
            redisCache.set(k, 10);
            assertThat(redisCache.decrement(k, 3)).isEqualTo(7L);
        }
    }

    // ==================== MultiGet / MultiSet ====================

    @Nested
    @DisplayName("批量操作: multiGet/multiSet")
    class MultiOperations {

        @Test
        @DisplayName("multiSet 和 multiGet")
        void multiSetAndMultiGet() {
            String k1 = key("mg1");
            String k2 = key("mg2");
            Map<String, String> map = new HashMap<>();
            map.put(k1, "a");
            map.put(k2, "b");
            redisCache.multiSet(map);

            Map<String, String> result = redisCache.multiGet(List.of(k1, k2));
            assertThat(result).hasSize(2);
            assertThat(result).containsEntry(k1, "a").containsEntry(k2, "b");
        }

        @Test
        @DisplayName("multiGet 不存在字段返回空 map")
        void multiGetEmptyKeys() {
            Map<String, String> result = redisCache.multiGet(List.of());
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("multiGet 部分存在")
        void multiGetPartial() {
            String k1 = key("mgp1");
            String k2 = key("mgp2");
            redisCache.set(k1, "exists");
            Map<String, String> result = redisCache.multiGet(List.of(k1, k2));
            assertThat(result).containsOnlyKeys(k1);
        }

        @Test
        @DisplayName("multiSet空map抛出异常")
        void multiSetEmptyThrows() {
            assertThatThrownBy(() -> redisCache.multiSet(Map.of()))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ==================== Hash ====================

    @Nested
    @DisplayName("Hash 操作")
    class HashOperations {

        @Test
        @DisplayName("hashPut / hashGet")
        void hashPutAndGet() {
            String k = key("hash");
            redisCache.hashPut(k, "field1", "value1");
            String val = redisCache.hashGet(k, "field1");
            assertThat(val).isEqualTo("value1");
        }

        @Test
        @DisplayName("hashPutAll / hashGetAll")
        void hashPutAllAndGetAll() {
            String k = key("hash_all");
            Map<String, String> map = Map.of("a", "1", "b", "2");
            redisCache.hashPutAll(k, map);
            Map<String, String> all = redisCache.hashGetAll(k);
            assertThat(all).containsAllEntriesOf(map);
        }

        @Test
        @DisplayName("hashHasKey")
        void hashHasKey() {
            String k = key("hash_hk");
            redisCache.hashPut(k, "f", "v");
            assertThat(redisCache.hashHasKey(k, "f")).isTrue();
            assertThat(redisCache.hashHasKey(k, "x")).isFalse();
        }

        @Test
        @DisplayName("hashDelete")
        void hashDelete() {
            String k = key("hash_del");
            redisCache.hashPut(k, "f1", "v1");
            redisCache.hashPut(k, "f2", "v2");
            Long removed = redisCache.hashDelete(k, "f1", "f2");
            assertThat(removed).isEqualTo(2);
            assertThat(redisCache.hashSize(k)).isZero();
        }

        @Test
        @DisplayName("hashPutIfAbsent")
        void hashPutIfAbsent() {
            String k = key("hash_pia");
            assertThat(redisCache.hashPutIfAbsent(k, "f", "v")).isTrue();
            assertThat(redisCache.hashPutIfAbsent(k, "f", "v2")).isFalse();
        }

        @Test
        @DisplayName("hashIncrement")
        void hashIncrement() {
            String k = key("hash_inc");
            Long r = redisCache.hashIncrement(k, "counter", 1);
            assertThat(r).isEqualTo(1L);
            assertThat(redisCache.hashIncrement(k, "counter", 2)).isEqualTo(3L);
        }

        @Test
        @DisplayName("hashSize")
        void hashSize() {
            String k = key("hash_sz");
            assertThat(redisCache.hashSize(k)).isZero();
            redisCache.hashPut(k, "a", "1");
            redisCache.hashPut(k, "b", "2");
            assertThat(redisCache.hashSize(k)).isEqualTo(2);
        }
    }

    // ==================== List ====================

    @Nested
    @DisplayName("List 操作")
    class ListOperations {

        @Test
        @DisplayName("listPush / listRange")
        void listPushAndRange() {
            String k = key("list");
            redisCache.listPush(k, "a");
            redisCache.listPush(k, "b");
            List<String> range = redisCache.listRange(k, 0, -1);
            assertThat(range).containsExactly("a", "b");
        }

        @Test
        @DisplayName("listPopLeft")
        void listPopLeft() {
            String k = key("list_pop");
            redisCache.listPush(k, "x");
            redisCache.listPush(k, "y");
            String val = redisCache.listPop(k, String.class);
            assertThat(val).isEqualTo("x");
        }

        @Test
        @DisplayName("listPopRight")
        void listPopRight() {
            String k = key("list_pr");
            redisCache.listPush(k, "1");
            redisCache.listPush(k, "2");
            String val = redisCache.listPopRight(k, String.class);
            assertThat(val).isEqualTo("2");
        }

        @Test
        @DisplayName("listPushLeft")
        void listPushLeft() {
            String k = key("list_pl");
            redisCache.listPushLeft(k, "a");
            redisCache.listPushLeft(k, "b");
            List<String> range = redisCache.listRange(k, 0, -1);
            assertThat(range).containsExactly("b", "a");
        }

        @Test
        @DisplayName("listSize")
        void listSize() {
            String k = key("list_sz");
            assertThat(redisCache.listSize(k)).isZero();
            redisCache.listPush(k, "a");
            redisCache.listPush(k, "b");
            assertThat(redisCache.listSize(k)).isEqualTo(2);
        }
    }

    // ==================== Set ====================

    @Nested
    @DisplayName("Set 操作")
    class SetOperations {

        @Test
        @DisplayName("setAdd / setMembers / setSize")
        void setAddAndMembers() {
            String k = key("set");
            redisCache.setAdd(k, "a", "b", "c");
            Set<String> members = redisCache.setMembers(k, String.class);
            assertThat(members).containsExactlyInAnyOrder("a", "b", "c");
            assertThat(redisCache.setSize(k)).isEqualTo(3);
        }

        @Test
        @DisplayName("setIsMember")
        void setIsMember() {
            String k = key("set_im");
            redisCache.setAdd(k, "x");
            assertThat(redisCache.setIsMember(k, "x")).isTrue();
            assertThat(redisCache.setIsMember(k, "y")).isFalse();
        }

        @Test
        @DisplayName("setRemove")
        void setRemove() {
            String k = key("set_rm");
            redisCache.setAdd(k, "a", "b", "c");
            Long removed = redisCache.setRemove(k, "a", "b");
            assertThat(removed).isEqualTo(2);
            assertThat(redisCache.setSize(k)).isEqualTo(1);
        }

        @Test
        @DisplayName("setAdd空值抛出异常")
        void setAddEmptyThrows() {
            assertThatThrownBy(() -> redisCache.setAdd(key("set_e"), new String[0]))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ==================== ZSet ====================

    @Nested
    @DisplayName("ZSet 操作")
    class ZSetOperations {

        @Test
        @DisplayName("zsetAdd / zsetRangeByScore")
        void zsetAddAndRangeByScore() {
            String k = key("zset");
            redisCache.zsetAdd(k, 1.0, "a");
            redisCache.zsetAdd(k, 2.0, "b");
            redisCache.zsetAdd(k, 3.0, "c");
            Set<String> range = redisCache.zsetRangeByScore(k, 2.0, 3.0, String.class);
            assertThat(range).containsExactlyInAnyOrder("b", "c");
        }

        @Test
        @DisplayName("zsetScore")
        void zsetScore() {
            String k = key("zset_sc");
            redisCache.zsetAdd(k, 4.5, "item");
            Double score = redisCache.zsetScore(k, "item");
            assertThat(score).isEqualTo(4.5);
        }

        @Test
        @DisplayName("zsetRemove")
        void zsetRemove() {
            String k = key("zset_rm");
            redisCache.zsetAdd(k, 1.0, "x");
            redisCache.zsetAdd(k, 2.0, "y");
            Long removed = redisCache.zsetRemove(k, "x");
            assertThat(removed).isEqualTo(1);
            assertThat(redisCache.zsetSize(k)).isEqualTo(1);
        }

        @Test
        @DisplayName("zsetSize")
        void zsetSize() {
            String k = key("zset_sz");
            assertThat(redisCache.zsetSize(k)).isZero();
            redisCache.zsetAdd(k, 1.0, "a");
            assertThat(redisCache.zsetSize(k)).isEqualTo(1);
        }
    }

    // ==================== Lock ====================

    @Nested
    @DisplayName("分布式锁")
    class LockOperations {

        @Test
        @DisplayName("tryLock / unlock")
        void tryLockAndUnlock() {
            String k = key("lock");
            Boolean locked = redisCache.tryLock(k, "owner", 10, TimeUnit.SECONDS);
            assertThat(locked).isTrue();
            Boolean unlocked = redisCache.unlock(k, "owner");
            assertThat(unlocked).isTrue();
        }

        @Test
        @DisplayName("锁竞争 - 同一键二次加锁失败")
        void lockContention() {
            String k = key("lock_cont");
            assertThat(redisCache.tryLock(k, "client1", 10, TimeUnit.SECONDS)).isTrue();
            assertThat(redisCache.tryLock(k, "client2", 10, TimeUnit.SECONDS)).isFalse();
            redisCache.unlock(k, "client1");
        }

        @Test
        @DisplayName("unlock 错误持有者不释放")
        void unlockWrongOwner() {
            String k = key("lock_wr");
            redisCache.tryLock(k, "owner", 10, TimeUnit.SECONDS);
            Boolean result = redisCache.unlock(k, "wrong_owner");
            assertThat(result).isFalse();
            redisCache.unlock(k, "owner");
        }

        @Test
        @DisplayName("unlockIfValueMatches 匹配时释放")
        void unlockIfValueMatches() {
            String k = key("lock_uvm");
            redisCache.tryLock(k, "token", 10, TimeUnit.SECONDS);
            assertThat(redisCache.unlockIfValueMatches(k, "token")).isTrue();
        }

        @Test
        @DisplayName("unlockIfValueMatches 不匹配时不释放")
        void unlockIfValueMatchesWrong() {
            String k = key("lock_uvw");
            redisCache.tryLock(k, "token", 10, TimeUnit.SECONDS);
            assertThat(redisCache.unlockIfValueMatches(k, "wrong")).isFalse();
            redisCache.unlock(k, "token");
        }
    }

    // ==================== Lua Script ====================

    @Nested
    @DisplayName("Lua 脚本执行")
    class LuaScript {

        @Test
        @DisplayName("executeLuaScript")
        void executeLuaScript() {
            String k = key("lua");
            redisCache.set(k, 5);
            String script = "return redis.call('INCR', KEYS[1])";
            org.springframework.data.redis.core.script.DefaultRedisScript<Long> rs =
                    new org.springframework.data.redis.core.script.DefaultRedisScript<>(script, Long.class);
            Long result = redisCache.executeLuaScript(rs, List.of(k));
            assertThat(result).isEqualTo(6L);
        }
    }

    // ==================== Type Mismatch ====================

    @Nested
    @DisplayName("类型转换异常")
    class TypeMismatch {

        @Test
        @DisplayName("get 类型不匹配抛出 CacheTypeMismatchException")
        void typeMismatch() {
            String k = key("tm");
            redisCache.set(k, "not_a_number");
            assertThatThrownBy(() -> redisCache.get(k, Long.class))
                    .isInstanceOf(CacheTypeMismatchException.class);
        }

        @Test
        @DisplayName("listPop 类型不匹配抛出 CacheTypeMismatchException")
        void listPopTypeMismatch() {
            String k = key("tm_list");
            redisCache.listPush(k, "not_a_number");
            assertThatThrownBy(() -> redisCache.listPop(k, Long.class))
                    .isInstanceOf(CacheTypeMismatchException.class);
        }
    }

    // ==================== Keys ====================

    @Nested
    @DisplayName("Keys 通配符")
    class KeysPattern {

        @Test
        @DisplayName("keys 通配符查询")
        void keysPattern() {
            String prefix = TEST_PREFIX + "keys:" + UUID.randomUUID() + ":";
            redisCache.set(prefix + "a", "1");
            redisCache.set(prefix + "b", "2");
            Set<String> found = redisCache.keys(prefix + "*");
            assertThat(found).containsExactlyInAnyOrder(prefix + "a", prefix + "b");
        }
    }

    // ==================== SetIfAbsent ====================

    @Nested
    @DisplayName("SetIfAbsent")
    class SetIfAbsent {

        @Test
        @DisplayName("setIfAbsent 不存在时成功")
        void setIfAbsentNew() {
            String k = key("sia");
            assertThat((Boolean) redisCache.setIfAbsent(k, "first")).isTrue();
            assertThat((Object) redisCache.get(k)).isEqualTo("first");
        }

        @Test
        @DisplayName("setIfAbsent 已存在时失败")
        void setIfAbsentExisting() {
            String k = key("sia_ex");
            redisCache.set(k, "original");
            assertThat(redisCache.setIfAbsent(k, "second")).isFalse();
        }

        @Test
        @DisplayName("setIfAbsent 带过期时间")
        void setIfAbsentWithTtl() {
            String k = key("sia_ttl");
            assertThat(redisCache.setIfAbsent(k, "val", 3, TimeUnit.SECONDS)).isTrue();
            assertThat(redisCache.getExpire(k, TimeUnit.SECONDS)).isBetween(1L, 4L);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
