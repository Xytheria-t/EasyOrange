package com.cartethyia.easyorange.common.result;

import com.cartethyia.easyorange.common.enums.IResultCode;
import com.cartethyia.easyorange.common.enums.ResultCode;
import com.fasterxml.jackson.annotation.JsonIgnore;

public record Result<T>(String code, String message, T data, long timestamp) {

    public static <T> Result<T> success() {
        return success(null, ResultCode.SUCCESS.getMessage());
    }

    public static <T> Result<T> success(T data) {
        return success(data, ResultCode.SUCCESS.getMessage());
    }

    public static <T> Result<T> success(T data, String message) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data, System.currentTimeMillis());
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(ResultCode.FAIL.getCode(), message, null, System.currentTimeMillis());
    }

    public static <T> Result<T> error(IResultCode resultCode) {
        if (resultCode.isSuccess()) {
            throw new IllegalArgumentException("不能使用成功状态码创建错误响应: code=" + resultCode.getCode());
        }
        return error(resultCode.getCode(), resultCode.getMessage());
    }

    public static <T> Result<T> error(IResultCode resultCode, String message) {
        return error(resultCode.getCode(), message);
    }

    public static <T> Result<T> error(String code, String message) {
        return new Result<>(code, message, null, System.currentTimeMillis());
    }

    @JsonIgnore
    public boolean isSuccess() {
        return ResultCode.SUCCESS.getCode().equals(code);
    }

    public Result<T> withMessage(String message) {
        return new Result<>(this.code, message, this.data, this.timestamp);
    }
}
