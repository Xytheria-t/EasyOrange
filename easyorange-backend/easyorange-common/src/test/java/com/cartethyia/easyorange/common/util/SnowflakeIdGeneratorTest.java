package com.cartethyia.easyorange.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SnowflakeIdGenerator 单元测试
 */
@DisplayName("SnowflakeIdGenerator 单元测试")
class SnowflakeIdGeneratorTest {

    private static final long WORKER_ID = 1L;
    private static final long DATA_CENTER_ID = 1L;

    @Nested
    @DisplayName("构造器验证")
    class ConstructorTests {

        @Test
        @DisplayName("合法参数构造成功")
        void validParams_constructSuccess() {
            var generator = new SnowflakeIdGenerator(0, 0);
            assertNotNull(generator);
            assertTrue(generator.nextId() > 0);
        }

        @Test
        @DisplayName("workerId 超过最大值抛出异常")
        void workerIdTooLarge_throwsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> new SnowflakeIdGenerator(32, 1));
        }

        @Test
        @DisplayName("workerId 为负数抛出异常")
        void workerIdNegative_throwsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> new SnowflakeIdGenerator(-1, 1));
        }

        @Test
        @DisplayName("dataCenterId 超过最大值抛出异常")
        void dataCenterIdTooLarge_throwsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> new SnowflakeIdGenerator(1, 32));
        }

        @Test
        @DisplayName("dataCenterId 为负数抛出异常")
        void dataCenterIdNegative_throwsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> new SnowflakeIdGenerator(1, -1));
        }
    }

    @Nested
    @DisplayName("nextId 方法")
    class NextIdTests {

        @Test
        @DisplayName("生成的 ID 为正数")
        void generatedId_positive() {
            var generator = new SnowflakeIdGenerator(WORKER_ID, DATA_CENTER_ID);
            long id = generator.nextId();
            assertTrue(id > 0);
        }

        @Test
        @DisplayName("连续生成的 ID 单调递增")
        void consecutiveIds_monotonicallyIncreasing() {
            var generator = new SnowflakeIdGenerator(WORKER_ID, DATA_CENTER_ID);
            long prev = generator.nextId();
            for (int i = 0; i < 1000; i++) {
                long current = generator.nextId();
                assertTrue(current > prev, "ID should be strictly increasing");
                prev = current;
            }
        }

        @Test
        @DisplayName("同一毫秒内生成的 ID 不重复")
        void idsWithinSameMillisecond_unique() {
            var generator = new SnowflakeIdGenerator(WORKER_ID, DATA_CENTER_ID);
            Set<Long> ids = new HashSet<>();
            for (int i = 0; i < 1000; i++) {
                long id = generator.nextId();
                assertTrue(ids.add(id), "Duplicate ID generated: " + id);
            }
        }

        @RepeatedTest(5)
        @DisplayName("批量生成 ID 无重复")
        void batchIds_noDuplicates() {
            var generator = new SnowflakeIdGenerator(WORKER_ID, DATA_CENTER_ID);
            int count = 2000;
            Set<Long> ids = new HashSet<>();
            IntStream.range(0, count).forEach(i -> ids.add(generator.nextId()));
            assertEquals(count, ids.size());
        }

        @Test
        @DisplayName("不同实例生成的 ID 不重复")
        void differentInstances_uniqueIds() {
            var gen1 = new SnowflakeIdGenerator(1, 1);
            var gen2 = new SnowflakeIdGenerator(2, 1);
            Set<Long> ids = new HashSet<>();
            for (int i = 0; i < 500; i++) {
                assertTrue(ids.add(gen1.nextId()));
                assertTrue(ids.add(gen2.nextId()));
            }
        }

        @Test
        @DisplayName("不同 workerId 生成不同的 ID")
        void differentWorkerIds_differentIds() {
            var gen1 = new SnowflakeIdGenerator(1, 1);
            var gen2 = new SnowflakeIdGenerator(2, 2);
            long id1 = gen1.nextId();
            long id2 = gen2.nextId();
            assertNotEquals(id1, id2);
        }
    }

    @Nested
    @DisplayName("静态便捷方法")
    class StaticConvenienceTests {

        @Test
        @DisplayName("next 返回非空字符串")
        void next_returnsNonEmptyString() {
            String id = SnowflakeIdGenerator.next();
            assertNotNull(id);
            assertFalse(id.isEmpty());
            assertTrue(Long.parseLong(id) > 0);
        }

        @Test
        @DisplayName("nextLong 返回正数")
        void nextLong_returnsPositive() {
            long id = SnowflakeIdGenerator.nextLong();
            assertTrue(id > 0);
        }

        @Test
        @DisplayName("next 返回的值可解析为正数")
        void next_parsableAsPositiveLong() {
            for (int i = 0; i < 100; i++) {
                String id = SnowflakeIdGenerator.next();
                assertTrue(Long.parseLong(id) > 0);
            }
        }
    }

    @Nested
    @DisplayName("单例模式")
    class SingletonTests {

        @Test
        @DisplayName("getInstance 返回相同实例")
        void getInstance_sameInstance() {
            var instance1 = SnowflakeIdGenerator.getInstance();
            var instance2 = SnowflakeIdGenerator.getInstance();
            assertSame(instance1, instance2);
        }

        @Test
        @DisplayName("单例实例能正常生成 ID")
        void singleton_generatesValidId() {
            var instance = SnowflakeIdGenerator.getInstance();
            long id = instance.nextId();
            assertTrue(id > 0);
        }

        @Test
        @DisplayName("单例实例生成的 ID 不重复")
        void singleton_generatesUniqueIds() {
            var instance = SnowflakeIdGenerator.getInstance();
            Set<Long> ids = new HashSet<>();
            for (int i = 0; i < 1000; i++) {
                assertTrue(ids.add(instance.nextId()));
            }
        }
    }

    @Nested
    @DisplayName("时钟回退保护")
    class ClockBackwardsTests {

        @Test
        @DisplayName("构造后立即生成 ID 成功")
        void freshInstance_generatesSuccessfully() {
            var generator = new SnowflakeIdGenerator(WORKER_ID, DATA_CENTER_ID);
            assertDoesNotThrow(generator::nextId);
        }
    }
}