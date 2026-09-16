package com.cartethyia.easyorange.common.idgen;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * RFC 9562 UUID v7 静态工具 — 时间有序 + 随机后缀。
 * <p>
 * 布局：48-bit Unix 毫秒时间戳 | 4-bit 版本号(7) | 12-bit 随机
 *       | 2-bit 变体(10) | 62-bit 随机
 * <p>
 * 优势：零协调、无 WorkerId 管理、时间有序、128-bit 全局唯一、纯内存生成。
 * <p>
 * 有序性只到毫秒：同一毫秒内的多个 ID 彼此顺序随机；系统时钟回拨时，新 ID 可能排在
 * 旧 ID 之前。唯一性由 74 位随机后缀保证，不依赖时钟。
 * <p>
 * 随机源使用 {@link ThreadLocalRandom} 而非 {@link java.security.SecureRandom}：
 * 这里生成的 74 位随机后缀（rand_a 12 + rand_b 62）不承担安全职责——ID 不参与鉴权，
 * 越权由属主校验拦截；{@code ThreadLocalRandom} 的状态每线程独立，由已观测 ID 无法
 * 外推其他线程的下一条。它无锁、无熵阻塞，适合热路径。
 * <p>
 * 领域事件 ID 在聚合根内静态生成（纯算法、无外部协调，无需经过 Port 注入）；
 * 实体 ID 仍通过 {@link IdGenerator} Port 由应用层注入。
 */
public final class UuidV7 {

    private UuidV7() {}

    /**
     * 生成一个 RFC 9562 UUID v7
     */
    private static UUID generate() {
        long timestamp = System.currentTimeMillis();
        var rng = ThreadLocalRandom.current();

        // MSB: 48-bit 时间戳 | 4-bit 版本 (0x7) | 12-bit 随机
        long msb = (timestamp << 16)
                | (0x7L << 12) // version = 7
                | (rng.nextLong() & 0x0FFFL); // 12-bit rand_a

        // LSB: 2-bit 变体 (10) | 62-bit 随机
        long lsb = (0x2L << 62) // variant = 10xx
                | (rng.nextLong() & 0x3FFFFFFFFFFFFFFFL); // 62-bit rand_b

        return new UUID(msb, lsb);
    }

    /**
     * 生成 UUID v7 字符串（36 位，与 {@link IdGenerator#generateId()} 格式一致）
     */
    public static String generateId() {
        return generate().toString();
    }
}
