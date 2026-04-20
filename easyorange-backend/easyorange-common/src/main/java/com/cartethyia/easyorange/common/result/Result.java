package com.cartethyia.easyorange.common.result;

import com.cartethyia.easyorange.common.enums.IResultCode;
import com.cartethyia.easyorange.common.enums.ResultCode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private String code;
    private String message;
    private T data;
    private long timestamp;

    public static <T> Result<T> success() {
        return success(null, ResultCode.SUCCESS.getMessage());
    }

    public static <T> Result<T> success(T data) {
        return success(data, ResultCode.SUCCESS.getMessage());
    }

    public static <T> Result<T> success(T data, String message) {
        return Result.<T>builder()
                .code(ResultCode.SUCCESS.getCode())
                .message(message)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> Result<T> error(IResultCode resultCode) {
        if (resultCode.isSuccess()) {
            throw new IllegalArgumentException("不能使用成功状态码创建错误响应: code=" + resultCode.getCode());
        }
        return error(resultCode.getCode(), resultCode.getMessage());
    }

    public static <T> Result<T> error(String message) {
        return Result.<T>builder()
                .code(ResultCode.FAIL.getCode())
                .message(message)
                .data(null)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> Result<T> error(IResultCode resultCode, String message) {
        return error(resultCode.getCode(), message);
    }

    public static <T> Result<T> error(String code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @JsonIgnore
    public boolean isSuccess() {
        return ResultCode.SUCCESS.getCode().equals(code);
    }

    public Result<T> withMessage(String message) {
        return Result.<T>builder()
                .code(this.code)
                .message(message)
                .data(this.data)
                .timestamp(this.timestamp)
                .build();
    }
}
