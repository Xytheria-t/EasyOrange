package com.cartethyia.easyorange.framework.exception;

import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.common.exception.validation.ParamValidationException;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.framework.util.RequestUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseBusinessException.class)
    public ResponseEntity<Result<Void>> handleBaseBusinessException(BaseBusinessException e) {
        log.warn("业务异常[code={}, type={}]: {}", e.getCode(), e.getClass().getSimpleName(), e.getMessage());
        return ResponseEntity
                .status(e.httpStatus())
                .body(Result.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(ParamValidationException.class)
    public Result<Map<String, String>> handleParamValidationException(ParamValidationException e) {
        log.warn("action=validate_error, errors={}", e.getFieldErrors());
        return Result.error(ResultCode.VALIDATE_FAILED, e.getFirstErrorMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("权限不足[path={}]: {}", RequestUtil.getRequestPath(), e.getMessage());
        return Result.error(ResultCode.FORBIDDEN);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持[method={}]: {}", e.getMethod(), e.getMessage());
        return Result.error(ResultCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = extractAllErrors(e.getBindingResult());
        log.warn("action=validate_failed, msg={}", message);
        return Result.error(ResultCode.VALIDATE_FAILED, message);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = extractAllErrors(e.getBindingResult());
        log.warn("action=bind_error, msg={}", message);
        return Result.error(ResultCode.VALIDATE_FAILED, message);
    }

    private String extractFieldErrors(List<org.springframework.validation.FieldError> errors) {
        return errors.stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
    }

    private String extractAllErrors(org.springframework.validation.BindingResult bindingResult) {
        StringBuilder sb = new StringBuilder();
        
        List<org.springframework.validation.FieldError> fieldErrors = bindingResult.getFieldErrors();
        if (!fieldErrors.isEmpty()) {
            sb.append(extractFieldErrors(fieldErrors));
        }
        
        List<org.springframework.validation.ObjectError> globalErrors = bindingResult.getGlobalErrors();
        if (!globalErrors.isEmpty()) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            String globalMessages = globalErrors.stream()
                    .map(org.springframework.validation.ObjectError::getDefaultMessage)
                    .collect(Collectors.joining("; "));
            sb.append(globalMessages);
        }
        
        return sb.toString();
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("action=constraint_error, msg={}", message);
        return Result.error(ResultCode.VALIDATE_FAILED, message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("action=missing_param, name={}", e.getParameterName());
        return Result.error(ResultCode.VALIDATE_FAILED, "缺少必填参数：" + e.getParameterName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("action=body_parse_error");
        return Result.error(ResultCode.VALIDATE_FAILED, "请求体格式错误");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型转换失败[name={}, value={}]: {}", e.getName(), e.getValue(), e.getMessage());
        return Result.error(ResultCode.VALIDATE_FAILED, "参数 '" + e.getName() + "' 类型错误");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.warn("请求地址不存在: {}", e.getRequestURL());
        return Result.error(ResultCode.NOT_FOUND);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoResourceFoundException(NoResourceFoundException e) {
        log.debug("静态资源不存在: {}", e.getResourcePath());
        return Result.error(ResultCode.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return Result.error(ResultCode.VALIDATE_FAILED, e.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKeyException(DuplicateKeyException e) {
        log.warn("action=duplicate_key, path={}, msg={}", RequestUtil.getRequestPath(), e.getMessage());
        String userMessage = extractDuplicateFieldMessage(e.getMessage());
        return Result.error(ResultCode.VALIDATE_FAILED, userMessage);
    }

    private String extractDuplicateFieldMessage(String errorMessage) {
        if (errorMessage == null) {
            return "数据已存在，请检查输入";
        }
        if (errorMessage.contains("uk_eo_user_email")) {
            return "邮箱已被注册";
        }
        if (errorMessage.contains("uk_eo_user_phone")) {
            return "手机号已被注册";
        }
        if (errorMessage.contains("uk_eo_user_student_id")) {
            return "学号已被注册";
        }
        if (errorMessage.contains("uk_eo_user_username")) {
            return "用户名已存在";
        }
        if (errorMessage.contains("uk_eo_favorite_user_product_del")) {
            return "已收藏过该商品";
        }
        return "数据已存在，请检查输入";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("action=system_error, path={}", RequestUtil.getRequestPath(), e);
        return Result.error(ResultCode.INTERNAL_SERVER_ERROR);
    }
}
