package com.cartethyia.easyorange.common.util;

public final class MaskUtils {

    private static final String MASK = "****";
    private static final String ID_CARD_MASK = "********";

    private MaskUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + MASK + phone.substring(phone.length() - 4);
    }

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

    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 10) {
            return idCard;
        }
        return idCard.substring(0, 6) + ID_CARD_MASK + idCard.substring(idCard.length() - 4);
    }

    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        int len = name.length();
        if (len == 1) {
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

    public static String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.length() < 8) {
            return bankCard;
        }
        return bankCard.substring(0, 4) + MASK + bankCard.substring(bankCard.length() - 4);
    }

    public static String maskAddress(String address) {
        return maskAddress(address, 6);
    }

    public static String maskAddress(String address, int keepPrefix) {
        if (address == null || address.length() <= keepPrefix) {
            return address;
        }
        return address.substring(0, keepPrefix) + "***";
    }

    public static String mask(String value, int head, int tail) {
        return mask(value, head, tail, MASK);
    }

    public static String mask(String value, int head, int tail, String maskChars) {
        if (value == null || value.length() <= head + tail) {
            return value;
        }
        return value.substring(0, head) + maskChars + value.substring(value.length() - tail);
    }
}
