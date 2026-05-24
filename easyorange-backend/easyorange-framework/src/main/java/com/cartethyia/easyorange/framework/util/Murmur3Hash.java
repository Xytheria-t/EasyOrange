package com.cartethyia.easyorange.framework.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class Murmur3Hash {

    private static final long C1 = 0x87c37b91114253d5L;
    private static final long C2 = 0x4cf5ad432745937fL;

    private Murmur3Hash() {}

    public static long high64(byte[] data, int seed) {
        return hash128(data, seed).h1;
    }

    public static long low64(byte[] data, int seed) {
        return hash128(data, seed).h2;
    }

    public static long hashLong(long value, int seed) {
        ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);
        buf.putLong(value);
        return hash128(buf.array(), seed).h1;
    }

    public static long hashString(String value, int seed) {
        byte[] data = value.getBytes(StandardCharsets.UTF_8);
        return hash128(data, seed).h1;
    }

    private static Hash128 hash128(byte[] data, int seed) {
        int length = data.length;
        long h1 = seed & 0xffffffffL;
        long h2 = seed & 0xffffffffL;

        int nBlocks = length / 16;
        for (int i = 0; i < nBlocks; i++) {
            long k1 = readLongLittleEndian(data, i * 16);
            long k2 = readLongLittleEndian(data, i * 16 + 8);

            k1 *= C1;
            k1 = Long.rotateLeft(k1, 31);
            k1 *= C2;
            h1 ^= k1;

            h1 = Long.rotateLeft(h1, 27);
            h1 += h2;
            h1 = h1 * 5 + 0x52dce729;

            k2 *= C2;
            k2 = Long.rotateLeft(k2, 33);
            k2 *= C1;
            h2 ^= k2;

            h2 = Long.rotateLeft(h2, 31);
            h2 += h1;
            h2 = h2 * 5 + 0x38495ab5;
        }

        int offset = nBlocks * 16;
        long k1 = 0;
        long k2 = 0;

        int tail = length & 15;
        if (tail >= 15) k2 ^= (long) (data[offset + 14] & 0xff) << 48;
        if (tail >= 14) k2 ^= (long) (data[offset + 13] & 0xff) << 40;
        if (tail >= 13) k2 ^= (long) (data[offset + 12] & 0xff) << 32;
        if (tail >= 12) k2 ^= (long) (data[offset + 11] & 0xff) << 24;
        if (tail >= 11) k2 ^= (long) (data[offset + 10] & 0xff) << 16;
        if (tail >= 10) k2 ^= (long) (data[offset + 9] & 0xff) << 8;
        if (tail >= 9) k2 ^= (long) (data[offset + 8] & 0xff);
        if (tail >= 9) {
            k2 *= C2;
            k2 = Long.rotateLeft(k2, 33);
            k2 *= C1;
            h2 ^= k2;
        }

        if (tail >= 8) k1 ^= (long) (data[offset + 7] & 0xff) << 56;
        if (tail >= 7) k1 ^= (long) (data[offset + 6] & 0xff) << 48;
        if (tail >= 6) k1 ^= (long) (data[offset + 5] & 0xff) << 40;
        if (tail >= 5) k1 ^= (long) (data[offset + 4] & 0xff) << 32;
        if (tail >= 4) k1 ^= (long) (data[offset + 3] & 0xff) << 24;
        if (tail >= 3) k1 ^= (long) (data[offset + 2] & 0xff) << 16;
        if (tail >= 2) k1 ^= (long) (data[offset + 1] & 0xff) << 8;
        if (tail >= 1) k1 ^= (long) (data[offset] & 0xff);

        if (tail > 0) {
            k1 *= C1;
            k1 = Long.rotateLeft(k1, 31);
            k1 *= C2;
            h1 ^= k1;
        }

        h1 ^= length;
        h2 ^= length;

        h1 += h2;
        h2 += h1;

        h1 = fmix64(h1);
        h2 = fmix64(h2);

        h1 += h2;
        h2 += h1;

        return new Hash128(h1, h2);
    }

    private static long readLongLittleEndian(byte[] data, int offset) {
        return ((long) data[offset] & 0xff)
                | ((long) data[offset + 1] & 0xff) << 8
                | ((long) data[offset + 2] & 0xff) << 16
                | ((long) data[offset + 3] & 0xff) << 24
                | ((long) data[offset + 4] & 0xff) << 32
                | ((long) data[offset + 5] & 0xff) << 40
                | ((long) data[offset + 6] & 0xff) << 48
                | ((long) data[offset + 7] & 0xff) << 56;
    }

    private static long fmix64(long k) {
        k ^= k >>> 33;
        k *= 0xff51afd7ed558ccdL;
        k ^= k >>> 33;
        k *= 0xc4ceb9fe1a85ec53L;
        k ^= k >>> 33;
        return k;
    }

    private record Hash128(long h1, long h2) {}
}