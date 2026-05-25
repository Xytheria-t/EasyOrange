package com.cartethyia.easyorange.framework.bloom;

import com.cartethyia.easyorange.framework.redis.RedisCache;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    private static final DefaultRedisScript<Long> PUT_SCRIPT;
    private static final DefaultRedisScript<Long> CHECK_SCRIPT;

    static {
        PUT_SCRIPT = new DefaultRedisScript<>();
        PUT_SCRIPT.setScriptText(BLOOM_PUT_SCRIPT);
        PUT_SCRIPT.setResultType(Long.class);

        CHECK_SCRIPT = new DefaultRedisScript<>();
        CHECK_SCRIPT.setScriptText(BLOOM_CHECK_SCRIPT);
        CHECK_SCRIPT.setResultType(Long.class);
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
        long[] offsets = hash(element);
        List<String> keys = List.of(filterKey);
        Object[] args = new Object[offsets.length];
        for (int i = 0; i < offsets.length; i++) {
            args[i] = String.valueOf(offsets[i]);
        }
        redisCache.executeLuaScript(PUT_SCRIPT, keys, args);
    }

    @Override
    public boolean mightContain(String filterKey, String element) {
        long[] offsets = hash(element);
        List<String> keys = List.of(filterKey);
        Object[] args = new Object[offsets.length];
        for (int i = 0; i < offsets.length; i++) {
            args[i] = String.valueOf(offsets[i]);
        }
        Long result = redisCache.executeLuaScript(CHECK_SCRIPT, keys, args);
        return result != null && result == 1;
    }

    @Override
    public void rebuild(String filterKey) {
        redisCache.delete(filterKey);
    }

    @Override
    public long bitSize() {
        return bitSize;
    }

    @Override
    public int numHashFunctions() {
        return numHashFunctions;
    }

    private long[] hash(String element) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(element.getBytes(StandardCharsets.UTF_8));

            long h1 = 0;
            long h2 = 0;
            for (int i = 0; i < 8; i++) {
                h1 = (h1 << 8) | (digest[i] & 0xFFL);
            }
            for (int i = 8; i < 16; i++) {
                h2 = (h2 << 8) | (digest[i] & 0xFFL);
            }

            h1 = h1 & Long.MAX_VALUE;
            h2 = h2 & Long.MAX_VALUE;

            long[] offsets = new long[numHashFunctions];
            for (int i = 0; i < numHashFunctions; i++) {
                offsets[i] = Math.floorMod(h1 + i * h2, bitSize);
            }
            return offsets;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }

    static long optimalBitSize(long expectedInsertions, double fpp) {
        if (fpp <= 0 || fpp >= 1) {
            throw new IllegalArgumentException("False positive rate must be between 0 and 1");
        }
        if (expectedInsertions <= 0) {
            throw new IllegalArgumentException("Expected insertions must be positive");
        }
        return (long) (-expectedInsertions * Math.log(fpp) / (Math.log(2) * Math.log(2)));
    }

    static int optimalHashFunctions(long expectedInsertions, long bitSize) {
        if (bitSize <= 0 || expectedInsertions <= 0) {
            return 1;
        }
        return Math.max(1, (int) Math.round((double) bitSize / expectedInsertions * Math.log(2)));
    }
}