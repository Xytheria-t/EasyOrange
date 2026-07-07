package com.cartethyia.easyorange.common.util;

public class MaskUtils {

    private static final String MASK = "****";

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

    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 15) {
            return idCard;
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
    }

    public static String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.length() < 8) {
            return bankCard;
        }
        return bankCard.substring(0, 4) + MASK + bankCard.substring(bankCard.length() - 4);
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
        return address.substring(0, visibleChars) + "***";
    }

    public static String mask(String value, int keepFront, int keepEnd) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.length() <= keepFront + keepEnd) {
            return value;
        }
        return value.substring(0, keepFront) + MASK + value.substring(value.length() - keepEnd);
    }
}