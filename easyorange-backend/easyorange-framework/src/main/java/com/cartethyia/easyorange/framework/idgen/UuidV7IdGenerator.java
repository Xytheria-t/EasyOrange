package com.cartethyia.easyorange.framework.idgen;

import java.security.SecureRandom;
import java.util.Random;
import java.util.UUID;

/**
 * UUID v7 实现（RFC 9562）— 时间有序 + 随机后缀
 * <p>
 * 布局：48-bit Unix 毫秒时间戳 | 4-bit 版本号(7) | 12-bit 随机
 *       | 2-bit 变体(10) | 62-bit 随机
 * <p>
 * 优势：零协调、无 WorkerId 管理、时间有序、128-bit 全局唯一、纯内存生成
 * 是 Snowflake 的现代替代方案（RFC 标准）。
 */
public class UuidV7IdGenerator implements IdGenerator {

    private static final long CUSTOM_EPOCH = 0L; // Unix epoch（1970-01-01）

    private final Random random;

    public UuidV7IdGenerator() {
        this.random = new SecureRandom();
    }

    UuidV7IdGenerator(Random random) {
        this.random = random;
    }

    @Override
    public String generateId() {
        return generate().toString();
    }

    /**
     * 生成一个 RFC 9562 UUID v7
     */
    public UUID generate() {
        long timestamp = System.currentTimeMillis();

        // MSB: 48-bit 时间戳 | 4-bit 版本 (0x7) | 12-bit 随机
        long msb = (timestamp - CUSTOM_EPOCH) << 16
                 | (0x7L << 12)                    // version = 7
                 | (random.nextLong() & 0x0FFFL);  // 12-bit rand_a

        // LSB: 2-bit 变体 (10) | 62-bit 随机
        long lsb = (0x2L << 62)                     // variant = 10xx
                 | (random.nextLong() & 0x3FFFFFFFFFFFFFFFL); // 62-bit rand_b

        return new UUID(msb, lsb);
    }
}
