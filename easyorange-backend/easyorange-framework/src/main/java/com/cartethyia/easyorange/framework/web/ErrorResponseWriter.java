package com.cartethyia.easyorange.framework.web;

import com.cartethyia.easyorange.common.enums.IResultCode;
import com.cartethyia.easyorange.common.result.Result;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 统一错误响应写入器 — Filter 层（SecurityConfig / RateLimitFilter / TokenRevocationFilter /
 * RefreshCsrfFilter 等）在进入 MVC 异常处理之前直接向 {@link HttpServletResponse} 写出
 * {@link Result} JSON 信封，此处收敛序列化逻辑，消除四处重复的
 * {@code setStatus + setContentType + setCharacterEncoding + objectMapper.writeValue}。
 */
@Component
public class ErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public ErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, HttpStatusCode status, IResultCode code, String message)
            throws IOException {
        write(response, status.value(), Result.error(code, message));
    }

    public void write(HttpServletResponse response, int status, IResultCode code, String message) throws IOException {
        write(response, status, Result.error(code, message));
    }

    public void write(HttpServletResponse response, int status, String code, String message) throws IOException {
        write(response, status, Result.error(code, message));
    }

    public void write(HttpServletResponse response, int status, Result<?> result) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), result);
    }
}
