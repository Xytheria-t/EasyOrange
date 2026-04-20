package com.cartethyia.easyorange.user.enums;

public enum ClientType {
    WEB("web"),
    MINI("mini");

    private final String value;

    ClientType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ClientType fromValue(String value) {
        if (value == null) {
            return MINI;
        }
        for (ClientType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return MINI;
    }
}