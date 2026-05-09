package com.cartethyia.easyorange.common.util;

public class MaskUtils {

    private MaskUtils() {}

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String name = parts[0];
        if (name.length() <= 1) {
            return "****@" + parts[1];
        }
        return name.charAt(0) + "****@" + parts[1];
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
        return bankCard.substring(0, 4) + "****" + bankCard.substring(bankCard.length() - 4);
    }

    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.length() == 1) {
            return name;
        }
        if (name.length() == 2) {
            return name.charAt(0) + "*";
        }
        if (name.length() == 3) {
            return name.charAt(0) + "*" + name.charAt(2);
        }
        return name.substring(0, 2) + "****";
    }

    public static String maskAddress(String address) {
        return maskAddress(address, null);
    }

    public static String maskAddress(String address, Integer visibleChars) {
        if (address == null || address.isEmpty()) {
            return address;
        }
        int visible = (visibleChars != null) ? visibleChars : 6;
        if (address.length() <= visible) {
            return address;
        }
        return address.substring(0, visible) + "***";
    }

    public static String mask(String value, int keepFront, int keepEnd) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.length() <= keepFront + keepEnd) {
            return value;
        }
        return value.substring(0, keepFront) + "****" + value.substring(value.length() - keepEnd);
    }
}