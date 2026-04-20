package com.cartethyia.easyorange.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 加密工具类
 * <p>
 * 提供 MD5、SHA-256、随机字符串等常用加密/哈希方法。
 * 注意：MD5 仅用于数据完整性校验，不适合密码存储（密码应使用 BCrypt）。
 * </p>
 *
 * @author cartethyia
 */
public final class EncryptUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private EncryptUtils() {
        throw new IllegalStateException("Utility class");
    }

    // ==================== MD5 ====================

    /**
     * 计算字符串的 MD5 哈希值（32 位小写十六进制）
     *
     * @param input 输入字符串
     * @return MD5 哈希值
     */
    public static String md5(String input) {
        if (input == null) {
            return null;
        }
        return hash(input, "MD5");
    }

    /**
     * 计算字节数组的 MD5 哈希值
     */
    public static String md5(byte[] input) {
        if (input == null) {
            return null;
        }
        return hashBytes(input, "MD5");
    }

    // ==================== SHA-256 ====================

    /**
     * 计算字符串的 SHA-256 哈希值
     */
    public static String sha256(String input) {
        if (input == null) {
            return null;
        }
        return hash(input, "SHA-256");
    }

    /**
     * 计算字节数组的 SHA-256 哈希值
     */
    public static String sha256(byte[] input) {
        if (input == null) {
            return null;
        }
        return hashBytes(input, "SHA-256");
    }

    // ==================== 随机生成 ====================

    /**
     * 生成指定长度的随机字母数字字符串
     *
     * @param length 字符串长度
     * @return 随机字符串
     */
    public static String randomAlphanumeric(int length) {
        if (length <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(SECURE_RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    /**
     * 生成指定长度的随机数字字符串
     */
    public static String randomNumeric(int length) {
        if (length <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(SECURE_RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    // ==================== Base64 ====================

    /**
     * Base64 编码
     */
    public static String base64Encode(byte[] input) {
        if (input == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(input);
    }

    /**
     * Base64 解码
     */
    public static byte[] base64Decode(String input) {
        if (input == null) {
            return null;
        }
        return Base64.getDecoder().decode(input);
    }

    // ==================== 内部方法 ====================

    private static String hash(String input, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("加密算法不可用: " + algorithm, e);
        }
    }

    private static String hashBytes(byte[] input, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hashBytes = digest.digest(input);
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("加密算法不可用: " + algorithm, e);
        }
    }
}
