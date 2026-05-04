package com.cartethyia.easyorange.common.enums;

public interface IResultCode {

    String getCode();

    String getMessage();

    default boolean isSuccess() {
        return "A0000".equals(getCode());
    }

    static int mapToHttpStatus(String code) {
        return 200;
    }
}
