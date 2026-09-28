package com.cartethyia.easyorange.common.util;

public class MaskUtils {

    private static final String MASK = "****";

    /**
     * 地址专用遮罩：3 颗星。地址本身已按「保留前 N 字」处理，遮罩位数不影响判读，
     * 单独成常量是为了让「地址用 3 颗星」成为显式契约而不是裸字面量 ——
     * 该形态已被 order / application 的脱敏断言锁定，不能随手并到 {@link #MASK}。
     */
    private static final String ADDRESS_MASK = "***";

    private MaskUtils() {}

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + MASK + phone.substring(phone.length() - 4);
    }

    public static String maskEmail(String email) {
        if (email == null) return null;
        var at = email.indexOf('@');
        if (at == -1) return email;
        var local = email.substring(0, at);
        return switch (local.length()) {
            case 0, 1 -> MASK + email.substring(at);
            default -> local.charAt(0) + MASK + email.substring(at);
        };
    }

    public static String maskName(String name) {
        if (name == null || name.isEmpty()) return name;
        return switch (name.length()) {
            case 1 -> name;
            case 2 -> name.charAt(0) + "*";
            case 3 -> name.charAt(0) + "*" + name.charAt(2);
            default -> name.substring(0, 2) + MASK;
        };
    }

    public static String maskAddress(String address) {
        return maskAddress(address, 6);
    }

    public static String maskAddress(String address, int visibleChars) {
        if (address == null || address.isEmpty()) {
            return address;
        }
        if (address.length() <= visibleChars) {
            return address;
        }
        return address.substring(0, visibleChars) + ADDRESS_MASK;
    }
}
