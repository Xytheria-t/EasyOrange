package com.cartethyia.easyorange.common.util;

/**
 * 数据脱敏工具类
 * <p>
 * 提供手机号、邮箱、身份证、姓名等常见敏感信息的脱敏处理。
 * 适用于日志记录、接口返回等场景。
 * </p>
 *
 * <pre>{@code
 * // 用法示例
 * MaskUtils.maskPhone("13812345678");     // 138****5678
 * MaskUtils.maskEmail("user@example.com"); // u***@example.com
 * MaskUtils.maskIdCard("110101199001011234"); // 110101********1234
 * }</pre>
 *
 * @author cartethyia
 */
public final class MaskUtils {

    private static final String MASK = "****";
    private static final String ID_CARD_MASK = "********";

    private MaskUtils() {
        throw new IllegalStateException("Utility class");
    }

    // ==================== 手机号 ====================

    /**
     * 手机号脱敏：保留前 3 后 4，中间替换为 ****
     * <p>
     * 例：13812345678 → 138****5678
     * </p>
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + MASK + phone.substring(phone.length() - 4);
    }

    // ==================== 邮箱 ====================

    /**
     * 邮箱脱敏：保留首字符和 @ 之后的域名
     * <p>
     * 例：user@example.com → u***@example.com
     * </p>
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf("@");
        if (atIndex <= 1) {
            return MASK + email.substring(atIndex);
        }
        return email.charAt(0) + MASK + email.substring(atIndex);
    }

    // ==================== 身份证 ====================

    /**
     * 身份证号脱敏：保留前 6 后 4，中间替换为 ********
     * <p>
     * 例：110101199001011234 → 110101********1234
     * </p>
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 10) {
            return idCard;
        }
        return idCard.substring(0, 6) + ID_CARD_MASK + idCard.substring(idCard.length() - 4);
    }

    // ==================== 姓名 ====================

    /**
     * 姓名脱敏：
     * <ul>
     *   <li>2 个字：张* → 张*</li>
     *   <li>3 个字：张三丰 → 张*丰</li>
     *   <li>4 个字及以上：欧阳娜娜 → 欧阳**</li>
     * </ul>
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        int len = name.length();
        if (len <= 1) {
            return name;
        }
        if (len == 2) {
            return name.charAt(0) + "*";
        }
        if (len == 3) {
            return name.charAt(0) + "*" + name.charAt(2);
        }
        return name.substring(0, 2) + "****";
    }

    // ==================== 银行卡 ====================

    /**
     * 银行卡号脱敏：保留前 4 后 4，中间替换为 ****
     * <p>
     * 例：6222021234567890123 → 6222****0123
     * </p>
     */
    public static String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.length() < 8) {
            return bankCard;
        }
        return bankCard.substring(0, 4) + MASK + bankCard.substring(bankCard.length() - 4);
    }

    // ==================== 地址 ====================

    /**
     * 地址脱敏：保留前 6 个字符，后续替换为 ***
     * <p>
     * 例：北京市朝阳区xxx → 北京市朝阳区***
     * </p>
     */
    public static String maskAddress(String address) {
        return maskAddress(address, 6);
    }

    /**
     * 地址脱敏：保留前 N 个字符，后续替换为 ***
     *
     * @param address    原始地址
     * @param keepPrefix 保留前缀字符数
     */
    public static String maskAddress(String address, int keepPrefix) {
        if (address == null || address.length() <= keepPrefix) {
            return address;
        }
        return address.substring(0, keepPrefix) + "***";
    }

    // ==================== 通用 ====================

    /**
     * 通用脱敏：保留前 n 后 m，中间替换为 ****
     *
     * @param value 原始值
     * @param head  保留头部字符数
     * @param tail  保留尾部字符数
     */
    public static String mask(String value, int head, int tail) {
        if (value == null || value.length() <= head + tail) {
            return value;
        }
        return value.substring(0, head) + MASK + value.substring(value.length() - tail);
    }

    /**
     * 通用脱敏：保留前 n 后 m，中间替换为自定义掩码字符
     *
     * @param value     原始值
     * @param head      保留头部字符数
     * @param tail      保留尾部字符数
     * @param maskChars 掩码字符（如 "***", "****"）
     */
    public static String mask(String value, int head, int tail, String maskChars) {
        if (value == null || value.length() <= head + tail) {
            return value;
        }
        return value.substring(0, head) + maskChars + value.substring(value.length() - tail);
    }
}
