package com.cartethyia.easyorange.framework.idgen;

import com.cartethyia.easyorange.common.idgen.IdGenerator;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * UUID v7 实现（RFC 9562）— 时间有序 + 随机后缀
 * <p>
 * 布局：48-bit Unix 毫秒时间戳 | 4-bit 版本号(7) | 12-bit 随机
 *       | 2-bit 变体(10) | 62-bit 随机
 * <p>
 * 优势：零协调、无 WorkerId 管理、时间有序、128-bit 全局唯一、纯内存生成。
 * <p>
 * 随机源使用 {@link ThreadLocalRandom} 而非 {@link java.security.SecureRandom}：
 * UUID v7 的 122 位随机后缀不需要加密安全强度，{@code ThreadLocalRandom}
 * 无锁、无熵阻塞、每线程独立种子，性能更高。
 */
public class UuidV7IdGenerator implements IdGenerator {

    private static final long CUSTOM_EPOCH = 0L; // Unix epoch（1970-01-01）

    @Override
    public String generateId() {
        return generate().toString();
    }

    /**
     * 生成一个 RFC 9562 UUID v7
     */
    public static UUID generate() {
        long timestamp = System.currentTimeMillis();
        var rng = ThreadLocalRandom.current();

        // MSB: 48-bit 时间戳 | 4-bit 版本 (0x7) | 12-bit 随机
        long msb = (timestamp - CUSTOM_EPOCH) << 16
                 | (0x7L << 12)                    // version = 7
                 | (rng.nextLong() & 0x0FFFL);     // 12-bit rand_a

        // LSB: 2-bit 变体 (10) | 62-bit 随机
        long lsb = (0x2L << 62)                     // variant = 10xx
                 | (rng.nextLong() & 0x3FFFFFFFFFFFFFFFL); // 62-bit rand_b

        return new UUID(msb, lsb);
    }
}
