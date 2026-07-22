package com.cartethyia.easyorange.framework.bloom;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.springframework.data.redis.core.RedisTemplate;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * 基于 Redis bitmap 的布隆过滤器实现。
 * <p>
 * 使用 {@code ValueOperations.setBit/getBit} 直接操作 Redis bitmap，
 * key 由 {@link RedisTemplate} 的 keySerializer 序列化（全局配置为
 * {@code StringRedisSerializer}，可读且不破坏 Lua 参数）。
 */
public class RedisBitmapBloomFilter implements BloomFilter {

    private static final HashFunction MURMUR3_128 = Hashing.murmur3_128();

    private final RedisTemplate<Object, Object> redisTemplate;
    private final long bitSize;
    private final int numHashFunctions;

    public RedisBitmapBloomFilter(RedisTemplate<Object, Object> redisTemplate) {
        this(redisTemplate, 1_000_000L, 0.01);
    }

    public RedisBitmapBloomFilter(RedisTemplate<Object, Object> redisTemplate, long expectedInsertions, double fpp) {
        this.redisTemplate = redisTemplate;
        this.bitSize = optimalBitSize(expectedInsertions, fpp);
        this.numHashFunctions = optimalHashFunctions(expectedInsertions, this.bitSize);
    }

    @Override
    public void put(String filterKey, String element) {
        long[] offsets = hash(element);
        var ops = redisTemplate.opsForValue();
        for (long offset : offsets) {
            ops.setBit(filterKey, offset, true);
        }
    }

    @Override
    public boolean mightContain(String filterKey, String element) {
        long[] offsets = hash(element);
        var ops = redisTemplate.opsForValue();
        for (long offset : offsets) {
            if (!Boolean.TRUE.equals(ops.getBit(filterKey, offset))) {
                return false;
            }
        }
        return true;
    }

    private long[] hash(String element) {
        var buf = ByteBuffer.wrap(MURMUR3_128.hashString(element, StandardCharsets.UTF_8).asBytes());
        long h1 = buf.getLong() & Long.MAX_VALUE;
        long h2 = buf.getLong() & Long.MAX_VALUE;

        long[] offsets = new long[numHashFunctions];
        for (int i = 0; i < numHashFunctions; i++) {
            offsets[i] = Math.floorMod(h1 + i * h2, bitSize);
        }
        return offsets;
    }

    private static long optimalBitSize(long expectedInsertions, double fpp) {
        if (fpp <= 0 || fpp >= 1) {
            throw new IllegalArgumentException("False positive rate must be between 0 and 1");
        }
        if (expectedInsertions <= 0) {
            throw new IllegalArgumentException("Expected insertions must be positive");
        }
        return (long) (-expectedInsertions * Math.log(fpp) / (Math.log(2) * Math.log(2)));
    }

    private static int optimalHashFunctions(long expectedInsertions, long bitSize) {
        if (bitSize <= 0 || expectedInsertions <= 0) {
            return 1;
        }
        return Math.max(1, (int) Math.round((double) bitSize / expectedInsertions * Math.log(2)));
    }
}
