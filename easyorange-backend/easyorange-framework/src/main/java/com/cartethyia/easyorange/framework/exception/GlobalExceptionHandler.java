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
                .status(mapToHttpStatus(e.getCode()))
                .body(Result.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(ParamValidationException.class)
    public ResponseEntity<Result<Map<String, String>>> handleParamValidationException(ParamValidationException e) {
        log.warn("action=validate_error, errors={}", e.getFieldErrors());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.error(ResultCode.VALIDATE_FAILED, e.getFirstErrorMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("权限不足[path={}]: {}", RequestUtil.getRequestPath(), e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Result.error(ResultCode.FORBIDDEN));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持[method={}]: {}", e.getMethod(), e.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(Result.error(ResultCode.METHOD_NOT_ALLOWED));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = extractAllErrors(e.getBindingResult());
        log.warn("action=validate_failed, msg={}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.error(ResultCode.VALIDATE_FAILED, message));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException e) {
        String message = extractAllErrors(e.getBindingResult());
        log.warn("action=bind_error, msg={}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.error(ResultCode.VALIDATE_FAILED, message));
    }

    private String extractFieldErrors(List<org.springframework.validation.FieldError> errors) {
        return errors.stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
    }

    private String extractAllErrors(org.springframework.validation.BindingResult bindingResult) {
        var sb = new StringBuilder();
        
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
    public ResponseEntity<Result<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("action=constraint_error, msg={}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.error(ResultCode.VALIDATE_FAILED, message));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("action=missing_param, name={}", e.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.error(ResultCode.VALIDATE_FAILED, "缺少必填参数：" + e.getParameterName()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("action=body_parse_error");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.error(ResultCode.VALIDATE_FAILED, "请求体格式错误"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型转换失败[name={}, value={}]: {}", e.getName(), e.getValue(), e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.error(ResultCode.VALIDATE_FAILED, "参数 '" + e.getName() + "' 类型错误"));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Result<Void>> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.warn("请求地址不存在: {}", e.getRequestURL());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Result.error(ResultCode.NOT_FOUND));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.debug("静态资源不存在: {}", e.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Result.error(ResultCode.NOT_FOUND));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.error(ResultCode.VALIDATE_FAILED, e.getMessage()));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> handleDuplicateKeyException(DuplicateKeyException e) {
        log.warn("action=duplicate_key, path={}, msg={}", RequestUtil.getRequestPath(), e.getMessage());
        String userMessage = extractDuplicateFieldMessage(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.error(ResultCode.VALIDATE_FAILED, userMessage));
    }

    private String extractDuplicateFieldMessage(String errorMessage) {
        return "数据已存在，请检查输入";
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("action=system_error, path={}", RequestUtil.getRequestPath(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Result.error(ResultCode.INTERNAL_SERVER_ERROR));
    }

    /**
     * 根据错误码前缀映射到 HTTP 状态码
     *
     * <p>A - 成功/客户端语义: A0401/A0402→401, A0403→403, A0404→404, A0405→405, A0500→400, 其余A→200</p>
     * <p>B - 业务错误: 400</p>
     * <p>C - 系统错误: 500</p>
     * <p>D - 第三方错误: 502</p>
     * <p>未知前缀: 400</p>
     * <p>null: 500</p>
     *
     * @param code 业务错误码
     * @return 映射后的 HTTP 状态码
     */
    private int mapToHttpStatus(String code) {
        if (code == null) {
            return 500;
        }
        if (code.isEmpty()) {
            return 400;
        }
        return switch (code.charAt(0)) {
            case 'A' -> {
                if (code.length() >= 5) {
                    yield switch (code.substring(2, 5)) {
                        case "401", "402" -> 401;
                        case "403"        -> 403;
                        case "404"        -> 404;
                        case "405"        -> 405;
                        case "500"        -> 400;
                        default           -> 200;
                    };
                }
                yield 200;
            }
            case 'C' -> 500;
            case 'D' -> 502;
            default  -> 400;
        };
    }
}
