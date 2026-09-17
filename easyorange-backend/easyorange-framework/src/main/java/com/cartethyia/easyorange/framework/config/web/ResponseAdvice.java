package com.cartethyia.easyorange.framework.config.web;

import com.cartethyia.easyorange.common.result.Result;
import jakarta.annotation.Nullable;
import java.util.Objects;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import tools.jackson.databind.ObjectMapper;

@RestControllerAdvice(basePackages = "com.cartethyia.easyorange")
public class ResponseAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    public ResponseAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(
            MethodParameter returnType, @Nullable Class<? extends HttpMessageConverter<?>> converterType) {
        return !alreadyEnveloped(returnType);
    }

    /**
     * 响应体是否已经是 {@link Result} 封套（是则跳过包装）。
     * <p>
     * 只看 {@code returnType.getParameterType()} 不够：{@code @ExceptionHandler} 的返回类型是
     * {@code ResponseEntity<Result<Void>>}，外层类型是 ResponseEntity，会被误判成「非 Result」，
     * 于是错误封套被再包一层 {@code Result.success(...)} —— 外层 code 变成 A0000/成功，真实错误码
     * 缩进 data（前端取外层 message 会把失败提示显示成「成功」）。因此还要看泛型参数，
     * {@code ResponseEntity<Result<T>>} / {@code HttpEntity<Result<T>>} 等包装类型一并覆盖。
     */
    private static boolean alreadyEnveloped(MethodParameter returnType) {
        ResolvableType type = ResolvableType.forMethodParameter(returnType);
        if (Result.class.isAssignableFrom(type.toClass())) {
            return true;
        }
        ResolvableType body = type.getGeneric(0);
        return body.resolve() != null && Result.class.isAssignableFrom(body.toClass());
    }

    @Override
    @Nullable
    public Object beforeBodyWrite(
            @Nullable Object body,
            @Nullable MethodParameter returnType,
            @Nullable MediaType selectedContentType,
            @Nullable Class<? extends HttpMessageConverter<?>> selectedConverterType,
            @Nullable ServerHttpRequest request,
            @Nullable ServerHttpResponse response) {
        switch (body) {
            case null -> {
                return Result.success();
            }
            case String str -> {
                Objects.requireNonNull(response).getHeaders().setContentType(MediaType.APPLICATION_JSON);
                try {
                    return objectMapper.writeValueAsString(Result.success(str));
                } catch (Exception e) {
                    throw new IllegalStateException("序列化响应失败", e);
                }
            }
            default -> {
                return Result.success(body);
            }
        }
    }
}
