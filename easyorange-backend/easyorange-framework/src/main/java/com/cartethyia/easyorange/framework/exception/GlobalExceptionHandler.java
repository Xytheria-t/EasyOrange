package com.cartethyia.easyorange.framework.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;

import com.cartethyia.easyorange.common.enums.IResultCode;
import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.common.exception.validation.ParamValidationException;
import com.cartethyia.easyorange.common.result.Result;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理。
 * <p>
 * 统一返回 {@link Result} 信封（与 Controller 正常响应和所有 Filter 一致），
 * HTTP 状态码由错误码映射（{@link IResultCode#resolveStatus(String)}，单一来源）：
 * A 段取码内数字推导 4xx（A0401/A04011→401 / A0403→403 / A0404→404 / A0405→405 / A0429→429，
 * 其余 A 归 400），B→400，C→500，D→502。校验类错误统一返回 400。业务异常实现 Spring {@code ErrorResponse}，
 * 状态码直接取自 {@link BaseBusinessException#getStatusCode()}。
 * <p>
 * 非 {@link BaseBusinessException} 子类的 RuntimeException（如 IllegalArgumentException）
 * 一律落入 500 兜底，提示编程错误而非客户端参数错误（见 AGENTS.md 异常规则）。
 */
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
                yield ResponseEntity.status(b.getStatusCode()).body(Result.error(b.getCode(), b.getMessage()));
            }
            case AccessDeniedException _ -> response(FORBIDDEN, ResultCode.FORBIDDEN);
            case AuthenticationException a -> {
                log.warn("认证异常[type={}]: {}", a.getClass().getSimpleName(), a.getMessage());
                yield response(UNAUTHORIZED, ResultCode.UNAUTHORIZED);
            }
            case HttpRequestMethodNotSupportedException _ ->
                response(METHOD_NOT_ALLOWED, ResultCode.METHOD_NOT_ALLOWED);
            case MethodArgumentNotValidException _, BindException _ -> handleValidation(getBindingResult(e));
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
            case HttpMediaTypeNotSupportedException m -> {
                log.warn("action=media_type_not_supported, content_type={}", m.getContentType());
                yield response(UNSUPPORTED_MEDIA_TYPE, ResultCode.PARAM_ERROR, "不支持的媒体类型：" + m.getContentType());
            }
            case DuplicateKeyException _ -> badRequest("数据已存在，请检查输入");
            case NoResourceFoundException ignored -> {
                log.debug("静态资源不存在: {}", ignored.getResourcePath());
                yield response(NOT_FOUND, ResultCode.NOT_FOUND);
            }
            default -> {
                log.error("action=system_error, type={}", e.getClass().getName(), e);
                yield response(INTERNAL_SERVER_ERROR, ResultCode.INTERNAL_SERVER_ERROR);
            }
        };
    }

    private ResponseEntity<Result<Void>> handleValidation(BindingResult br) {
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

    private static ResponseEntity<Result<Void>> response(HttpStatusCode status, IResultCode code) {
        return response(status, code, code.getMessage());
    }

    private static ResponseEntity<Result<Void>> response(HttpStatusCode status, IResultCode code, String message) {
        return ResponseEntity.status(status).body(Result.error(code, message));
    }

    private static ResponseEntity<Result<Void>> badRequest(String msg) {
        return ResponseEntity.status(BAD_REQUEST).body(Result.error(ResultCode.VALIDATE_FAILED.getCode(), msg));
    }
}
