package com.cartethyia.easyorange.framework.exception;

import com.cartethyia.easyorange.common.enums.IResultCode;
import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.common.exception.validation.ParamValidationException;
import com.cartethyia.easyorange.common.result.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handle(Exception e) {
        return switch (e) {
            case ParamValidationException p -> {
                log.warn("action=validate_error, errors={}", p.getFieldErrors());
                yield badRequest(p.getFirstErrorMessage());
            }
            case BaseBusinessException b -> {
                log.warn("业务异常[code={}, type={}]: {}", b.getCode(), b.getClass().getSimpleName(), b.getMessage());
                yield response(resolveHttpStatus(b.getCode()), b.getCode(), b.getMessage());
            }
            case AccessDeniedException _ -> response(FORBIDDEN, ResultCode.FORBIDDEN);
            case HttpRequestMethodNotSupportedException _ -> response(METHOD_NOT_ALLOWED, ResultCode.METHOD_NOT_ALLOWED);
            case MethodArgumentNotValidException _, BindException _ ->
                    handleBindingErrors(getBindingResult(e));
            case ConstraintViolationException c -> {
                var msg = c.getConstraintViolations().stream()
                        .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                        .collect(Collectors.joining("; "));
                log.warn("action=constraint_error, msg={}", msg);
                yield badRequest(msg);
            }
            case MissingServletRequestParameterException m -> {
                log.warn("action=missing_param, name={}", m.getParameterName());
                yield badRequest("缺少必填参数：" + m.getParameterName());
            }
            case HttpMessageNotReadableException _ -> {
                log.warn("action=body_parse_error");
                yield badRequest("请求体格式错误");
            }
            case MethodArgumentTypeMismatchException m -> {
                log.warn("参数类型转换失败[name={}, value={}]: {}", m.getName(), m.getValue(), m.getMessage());
                yield badRequest("参数 '" + m.getName() + "' 类型错误");
            }
            case NoHandlerFoundException n -> {
                log.warn("请求地址不存在: {}", n.getRequestURL());
                yield response(NOT_FOUND, ResultCode.NOT_FOUND);
            }
            case NoResourceFoundException n -> {
                log.debug("静态资源不存在: {}", n.getResourcePath());
                yield response(NOT_FOUND, ResultCode.NOT_FOUND);
            }
            case IllegalArgumentException a -> {
                log.warn("非法参数: {}", a.getMessage());
                yield badRequest(a.getMessage());
            }
            case DuplicateKeyException _ -> badRequest("数据已存在，请检查输入");
            default -> {
                log.error("action=system_error, type={}", e.getClass().getName(), e);
                yield response(INTERNAL_SERVER_ERROR, ResultCode.INTERNAL_SERVER_ERROR);
            }
        };
    }

    private ResponseEntity<Result<Void>> handleBindingErrors(BindingResult br) {
        var msg = extractAllErrors(br);
        log.warn("action=validation_error, msg={}", msg);
        return badRequest(msg);
    }

    private static BindingResult getBindingResult(Exception e) {
        return e instanceof MethodArgumentNotValidException m
               ? m.getBindingResult()
               : ((BindException) e).getBindingResult();
    }

    private static String extractAllErrors(BindingResult br) {
        var fieldPart = br.getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        var globalPart = br.getGlobalErrors().stream()
                .map(ObjectError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        if (fieldPart.isEmpty()) return globalPart;
        if (globalPart.isEmpty()) return fieldPart;
        return fieldPart + "; " + globalPart;
    }

    private static <T> ResponseEntity<Result<T>> response(HttpStatus status, IResultCode code) {
        return ResponseEntity.status(status).body(Result.error(code));
    }

    private static <T> ResponseEntity<Result<T>> response(HttpStatus status, String code, String msg) {
        return ResponseEntity.status(status).body(Result.error(code, msg));
    }

    private static <T> ResponseEntity<Result<T>> badRequest(String msg) {
        return ResponseEntity.status(BAD_REQUEST).body(Result.error(ResultCode.VALIDATE_FAILED, msg));
    }

    private static HttpStatus resolveHttpStatus(String errorCode) {
        if (errorCode == null || errorCode.isEmpty()) {
            return BAD_REQUEST;
        }
        if (errorCode.charAt(0) != 'A') {
            return switch (errorCode.charAt(0)) {
                case 'C' -> INTERNAL_SERVER_ERROR;
                case 'D' -> BAD_GATEWAY;
                default -> BAD_REQUEST;
            };
        }
        // A 前缀: 从最后 3 位推导 HTTP 状态码
        if (errorCode.length() < 5) return BAD_REQUEST;
        return switch (errorCode.substring(errorCode.length() - 3)) {
            case "401", "402" -> UNAUTHORIZED;
            case "403" -> FORBIDDEN;
            case "404" -> NOT_FOUND;
            case "405" -> METHOD_NOT_ALLOWED;
            case "500" -> BAD_REQUEST;
            default -> OK;
        };
    }
}
