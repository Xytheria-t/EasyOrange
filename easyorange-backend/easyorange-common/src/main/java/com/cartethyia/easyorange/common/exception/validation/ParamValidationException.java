package com.cartethyia.easyorange.common.exception.validation;

import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import lombok.Getter;

import java.util.Map;

@Getter
public class ParamValidationException extends BaseBusinessException {

    private final Map<String, String> fieldErrors;

    public ParamValidationException(Map<String, String> fieldErrors) {
        this(ResultCode.VALIDATE_FAILED.getMessage(), fieldErrors);
    }

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

    public String getFirstErrorMessage() {
        return fieldErrors.values().stream().findFirst().orElse(getMessage());
    }
}