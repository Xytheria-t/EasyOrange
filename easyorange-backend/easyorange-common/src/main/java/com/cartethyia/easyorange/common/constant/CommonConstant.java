package com.cartethyia.easyorange.common.constant;

public class CommonConstant {

    private CommonConstant() {}

    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static final int FILE_STATUS_NORMAL = 0;

    public static final int FILE_MAX_SIZE = 10 * 1024 * 1024;

    /** 中国大陆手机号（11 位，1 开头 + 3-9 第二位）— 全站统一校验口径。 */
    public static final String PHONE_REGEX = "^1[3-9]\\d{9}$";
}
