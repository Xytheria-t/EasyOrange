package com.cartethyia.easyorange.common.enums;

public enum ResultCode implements IResultCode {
    SUCCESS("0", "操作成功"),
    FAIL("1", "操作失败"),
    VALIDATE_FAILED("1007", "参数校验失败"),
    BUSINESS_ERROR("1000", "业务异常"),
    PARAM_ERROR("1001", "参数错误"),
    UNAUTHORIZED("1002", "未授权"),
    FORBIDDEN("1003", "禁止访问"),
    NOT_FOUND("1004", "资源不存在"),
    INTERNAL_SERVER_ERROR("1005", "内部服务器错误"),
    SERVICE_UNAVAILABLE("1006", "服务不可用"),
    METHOD_NOT_ALLOWED("1008", "请求方法不允许"),
    FILE_TOO_LARGE("2001", "文件大小超过限制"),
    INVALID_FILE_TYPE("2002", "无效的文件类型"),
    FILE_UPLOAD_FAILED("2003", "文件上传失败");

    private final String code;
    private final String message;

    ResultCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}