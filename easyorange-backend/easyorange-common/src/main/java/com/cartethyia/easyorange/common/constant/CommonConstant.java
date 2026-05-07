package com.cartethyia.easyorange.common.constant;

public class CommonConstant {

    private CommonConstant() {}

    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static final String DATE_FORMAT = "yyyy-MM-dd";

    public static final String TIME_ZONE = "Asia/Shanghai";

    public static final int DEFAULT_PAGE_SIZE = 20;

    public static final int MAX_PAGE_SIZE = 100;

    public static final String REDIS_KEY_PREFIX = "easyorange:";

    public static final String TOKEN_PREFIX = "token:";

    public static final String APP_PREFIX = "app:";

    public static final int DEFAULT_TOKEN_EXPIRE_SECONDS = 86400;

    public static final String SUCCESS_CODE = "0";

    public static final String FAIL_CODE = "1";

    public static final int FILE_STATUS_NORMAL = 0;

    public static final int FILE_STATUS_DELETED = 1;

    public static final int FILE_MAX_SIZE = 10 * 1024 * 1024;

    public static final String[] DEFAULT_ALLOWED_EXTENSIONS = {
            "jpg", "jpeg", "png", "gif", "bmp",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "zip", "rar"
    };

    public static String rateLimitKey(String limitType, String key) {
        return REDIS_KEY_PREFIX + "rate:" + limitType + ":" + key;
    }

    public static String repeatSubmitKey(String userIdentifier, String uri, String paramsHash) {
        return REDIS_KEY_PREFIX + "repeat:" + userIdentifier + ":" + uri + ":" + paramsHash;
    }
}