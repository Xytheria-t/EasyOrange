package com.cartethyia.easyorange.framework.exception;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

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

/**
 * 全局异常处理。
 * <p>
 * 统一返回 {@link Result<T>} 信封（与 Controller 正常响应和所有 Filter 一致），
 * HTTP 状态码按错误码前缀映射：A0401→401 / A0403→403 / A0404→404 / A0405→405，
 * 其余 A 前缀归 400（客户端语义），B→400，C→500，D→502，未知码一律 400。
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
                yield ResponseEntity.status(resolveHttpStatus(b.getCode()))
                        .body(Result.error(b.getCode(), b.getMessage()));
            }
            case AccessDeniedException _ -> response(FORBIDDEN, ResultCode.FORBIDDEN);
            case HttpRequestMethodNotSupportedException _ -> response(METHOD_NOT_ALLOWED, ResultCode.METHOD_NOT_ALLOWED);
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
            case DuplicateKeyException _ -> badRequest("数据已存在，请检查输入");
            case IllegalArgumentException a -> {
                log.warn("非法参数: {}", a.getMessage());
                yield badRequest(a.getMessage());
            }
            case NoHandlerFoundException n -> {
                log.warn("请求地址不存在: {}", n.getRequestURL());
                yield response(NOT_FOUND, ResultCode.NOT_FOUND);
            }
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
        return ResponseEntity.status(status).body(Result.error(code, code.getMessage()));
    }

    private static ResponseEntity<Result<Void>> badRequest(String msg) {
        return ResponseEntity.status(BAD_REQUEST).body(Result.error(ResultCode.VALIDATE_FAILED.getCode(), msg));
    }

    /**
     * 按错误码前缀映射 HTTP 状态码：A0401→401 / A0403→403 / A0404→404 / A0405→405，
     * 其余 A 前缀归 400（客户端语义），B→400，C→500，D→502。未知码一律 400。
     */
    private static HttpStatusCode resolveHttpStatus(String errorCode) {
        if (errorCode == null || errorCode.isEmpty()) {
            return BAD_REQUEST;
        }
        return switch (errorCode.charAt(0)) {
            case 'A' -> switch (errorCode) {
                case "A0401", "A0402" -> UNAUTHORIZED;
                case "A0403" -> FORBIDDEN;
                case "A0404" -> NOT_FOUND;
                case "A0405" -> METHOD_NOT_ALLOWED;
                default -> BAD_REQUEST;
            };
            case 'C' -> INTERNAL_SERVER_ERROR;
            case 'D' -> BAD_GATEWAY;
            default -> BAD_REQUEST;
        };
    }
}