package com.cartethyia.easyorange.framework.bloom;

import com.cartethyia.easyorange.framework.cache.RedisCache;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class RedisBitmapBloomFilter implements BloomFilter {

    private static final String BLOOM_PUT_SCRIPT = """
            local key = KEYS[1]
            local offsets = ARGV
            for i = 1, #offsets do
                redis.call('SETBIT', key, tonumber(offsets[i]), 1)
            end
            return 1
            """;

    private static final String BLOOM_CHECK_SCRIPT = """
            local key = KEYS[1]
            local offsets = ARGV
            for i = 1, #offsets do
                if redis.call('GETBIT', key, tonumber(offsets[i])) == 0 then
                    return 0
                end
            end
            return 1
            """;

    private static final DefaultRedisScript<Long> PUT_SCRIPT = script(BLOOM_PUT_SCRIPT);
    private static final DefaultRedisScript<Long> CHECK_SCRIPT = script(BLOOM_CHECK_SCRIPT);
    private static final HashFunction MURMUR3_128 = Hashing.murmur3_128();

    private static DefaultRedisScript<Long> script(String text) {
        var s = new DefaultRedisScript<Long>();
        s.setScriptText(text);
        s.setResultType(Long.class);
        return s;
    }

    private final RedisCache redisCache;
    private final long bitSize;
    private final int numHashFunctions;

    public RedisBitmapBloomFilter(RedisCache redisCache) {
        this(redisCache, 1_000_000L, 0.01);
    }

    public RedisBitmapBloomFilter(RedisCache redisCache, long expectedInsertions, double fpp) {
        this.redisCache = redisCache;
        this.bitSize = optimalBitSize(expectedInsertions, fpp);
        this.numHashFunctions = optimalHashFunctions(expectedInsertions, this.bitSize);
    }

    @Override
    public void put(String filterKey, String element) {
        execute(filterKey, element, PUT_SCRIPT);
    }

    @Override
    public boolean mightContain(String filterKey, String element) {
        Long result = execute(filterKey, element, CHECK_SCRIPT);
        return result != null && result == 1L;
    }

    private Long execute(String filterKey, String element, DefaultRedisScript<Long> script) {
        long[] offsets = hash(element);
        Object[] args = new Object[offsets.length];
        for (int i = 0; i < offsets.length; i++) {
            args[i] = String.valueOf(offsets[i]);
        }
        return redisCache.executeLuaScript(script, List.of(filterKey), args);
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