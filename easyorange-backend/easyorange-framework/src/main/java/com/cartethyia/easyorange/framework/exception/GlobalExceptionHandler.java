package com.cartethyia.easyorange.framework.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONTENT_TOO_LARGE;
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
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理。
 * <p>
 * 统一返回 {@link Result} 信封（与 Controller 正常响应和所有 Filter 一致），
 * HTTP 状态码由错误码映射（{@link IResultCode#resolveStatus(String)}，单一来源）：
 * A 段取码内数字推导 4xx（A0401/A04011→401 / A0403→403 / A0404→404 / A0405→405 / A0413→413 / A0429→429，
 * 其余 A 归 400），B→400，C→500，D→502。校验类错误统一返回 400。业务异常实现 Spring {@code ErrorResponse}，
 * 状态码直接取自 {@link BaseBusinessException#getStatusCode()}。
 * <p>
 * 方法参数级约束有两条路径，均已映射 400：控制器未标 {@code @Validated} 时由 MVC 内建校验抛
 * {@link HandlerMethodValidationException}，标了则走 AOP 抛 {@link ConstraintViolationException}。
 * multipart 超限（容器级限制先于应用层文件校验触发）映射 413 + A0413，multipart 解析失败映射 400；
 * 数据库死锁/锁等待超时映射 B0006 并发冲突、唯一键冲突映射 400 — 这些若落 500，会被前端
 * {@code isRetryable}（status>=500）当可重试错误自动重试，把客户端错误变成重试风暴。
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
            // 方法参数级约束（如 @RequestParam @Max/@Pattern）：控制器未标注 @Validated 时由 Spring MVC
            // 内建方法校验抛出本异常（控制器标了 @Validated 则内建校验被关掉、改抛 ConstraintViolationException，
            // 两条路径都必须映射，否则约束被违反会静默变 500）
            case HandlerMethodValidationException h -> {
                var msg = h.getParameterValidationResults().stream()
                        .flatMap(result -> result.getResolvableErrors().stream()
                                .map(err -> parameterPrefix(result) + err.getDefaultMessage()))
                        .collect(Collectors.joining("; "));
                log.warn("action=method_validation_error, msg={}", msg);
                yield badRequest(msg.isEmpty() ? ResultCode.VALIDATE_FAILED.getMessage() : msg);
            }
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
            // 容器级 multipart 限制（spring.servlet.multipart.*）先于应用层文件校验触发：
            // 必须早于 MultipartException 匹配（子类在前），并映射为 413 而非 500，
            // 免得"文件过大"被前端 isRetryable（>=500）当作可重试错误自动重试。
            // 413 用 CONTENT_TOO_LARGE 而非 PAYLOAD_TOO_LARGE：RFC 9110 已把
            // "Payload Too Large" 更名为 "Content Too Large"，Spring 7 起后者标记 @Deprecated
            case MaxUploadSizeExceededException m -> {
                log.warn("action=upload_too_large, limit={}", m.getMaxUploadSize());
                yield response(
                        CONTENT_TOO_LARGE,
                        ResultCode.CONTENT_TOO_LARGE,
                        ResultCode.CONTENT_TOO_LARGE.getMessage() + sizeSuffix(m.getMaxUploadSize()));
            }
            case MultipartException _ -> {
                log.warn("action=multipart_error");
                yield badRequest("文件上传请求格式错误");
            }
            // MySQL 死锁(1213)/锁等待超时(1205) 经 Spring 翻译为 CannotAcquireLockException：
            // 属并发竞争下的可重试冲突，与乐观锁冲突同码（B0006→400），而非落 500
            case CannotAcquireLockException _ -> {
                log.warn("action=lock_conflict, type={}", e.getClass().getSimpleName());
                yield response(BAD_REQUEST, ResultCode.CONCURRENT_UPDATE, "数据并发冲突，请重试");
            }
            case DuplicateKeyException _ -> {
                log.warn("action=duplicate_key");
                yield badRequest("数据已存在，请检查输入");
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

    /** 方法参数校验结果 → 消息前缀（参数名缺失时退化为纯消息，避免拼出 "null: ..."）。 */
    private static String parameterPrefix(ParameterValidationResult result) {
        var name = result.getMethodParameter().getParameterName();
        return name == null ? "" : name + ": ";
    }

    /** multipart 上限的人类可读后缀；上限未知（<=0）时不拼后缀。 */
    private static String sizeSuffix(long maxUploadSize) {
        if (maxUploadSize <= 0) {
            return "";
        }
        long mb = maxUploadSize / (1024 * 1024);
        return mb > 0 ? "，单个文件上限 " + mb + "MB" : "，单个文件上限 " + maxUploadSize / 1024 + "KB";
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
