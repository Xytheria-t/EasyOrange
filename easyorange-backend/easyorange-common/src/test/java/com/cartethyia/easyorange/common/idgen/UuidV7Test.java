package com.cartethyia.easyorange.common.idgen;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("UuidV7 生成器测试")
class UuidV7Test {

    private static final long RAND_A_MASK = 0x0FFFL;

    @Nested
    @DisplayName("RFC 9562 布局")
    class LayoutTests {

        @Test
        @DisplayName("version 固定为 7，variant 固定为 IETF(2)")
        void fixesVersionAndVariant() {
            UUID uuid = UUID.fromString(UuidV7.generateId());

            assertThat(uuid.version()).isEqualTo(7);
            assertThat(uuid.variant()).isEqualTo(2);
        }

        @Test
        @DisplayName("字符串为 36 位小写规范格式，可被 UUID.fromString 还原")
        void stringFormatIsCanonical() {
            String id = UuidV7.generateId();

            assertThat(id).hasSize(36).matches("[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
            assertThat(UUID.fromString(id).toString()).isEqualTo(id);
        }

        @Test
        @DisplayName("rand_a 用满 12 位且不污染 version 位")
        void randAOccupiesTwelveBits() {
            Set<Long> randAs = new HashSet<>();
            for (int i = 0; i < 500; i++) {
                randAs.add(UUID.fromString(UuidV7.generateId()).getMostSignificantBits() & RAND_A_MASK);
            }

            assertThat(randAs).hasSizeGreaterThan(100);
        }
    }

    @Nested
    @DisplayName("时间戳")
    class TimestampTests {

        @Test
        @DisplayName("高 48 位落在本次调用前后的墙钟区间内")
        void encodesCurrentTimeMillis() {
            long before = System.currentTimeMillis();
            UUID uuid = UUID.fromString(UuidV7.generateId());
            long after = System.currentTimeMillis();

            assertThat(uuid.getMostSignificantBits() >>> 16).isBetween(before, after);
        }

        @Test
        @DisplayName("跨毫秒生成的两个 ID 按字符串排序与时间一致")
        void idsAcrossMillisSortAscending() throws InterruptedException {
            String earlier = UuidV7.generateId();
            long startedAt = System.currentTimeMillis();
            while (System.currentTimeMillis() == startedAt) {
                Thread.sleep(1);
            }

            assertThat(UuidV7.generateId()).isGreaterThan(earlier);
        }
    }

    @Nested
    @DisplayName("唯一性")
    class UniquenessTests {

        @Test
        @DisplayName("连续生成的 ID 不重复")
        void noCollisions() {
            int count = 10_000;
            Set<String> ids = new HashSet<>(count * 2);

            for (int i = 0; i < count; i++) {
                ids.add(UuidV7.generateId());
            }

            assertThat(ids).hasSize(count);
        }
    }
}
