package com.cartethyia.easyorange.common.exception;

import com.cartethyia.easyorange.common.enums.ResultCode;

import java.util.Map;

/**
 * 参数校验异常
 * <p>
 * 当 Jakarta Validation 校验失败时抛出，携带字段 → 错误消息的映射。
 * 便于 GlobalExceptionHandler 返回结构化的校验错误响应。
 * </p>
 *
 * @author cartethyia
 */
public class ParamValidationException extends BaseBusinessException {

    /**
     * 字段名 → 错误消息的映射
     */
    private final Map<String, String> fieldErrors;

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    @SuppressWarnings("unused")
    public ParamValidationException(Map<String, String> fieldErrors) {
        super(ResultCode.VALIDATE_FAILED, "参数校验失败");
        this.fieldErrors = fieldErrors != null
                ? Map.copyOf(fieldErrors)
                : Map.of();
    }

    @SuppressWarnings("unused")
    public ParamValidationException(String message, Map<String, String> fieldErrors) {
        super(ResultCode.VALIDATE_FAILED, message);
        this.fieldErrors = fieldErrors != null
                ? Map.copyOf(fieldErrors)
                : Map.of();
    }

    @Override
    protected String defaultCode() {
        return ResultCode.VALIDATE_FAILED.getCode();
    }

    /**
     * 获取第一个字段的错误消息
     */
    public String getFirstErrorMessage() {
        return fieldErrors.values().stream().findFirst().orElse(getMessage());
    }
}
