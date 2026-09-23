package com.cartethyia.easyorange.framework.config.web;

import com.cartethyia.easyorange.common.result.Result;
import jakarta.annotation.Nullable;
import java.util.Objects;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.Resource;
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
        return !alreadyEnveloped(returnType) && !isBinaryResourceResponse(returnType);
    }

    /**
     * 二进制 Resource 响应（file 下载 / view / thumbnail / responsive）不进封套。
     * <p>
     * converter 是按<b>原始</b> Resource 体选中的（ResourceHttpMessageConverter），
     * {@link #beforeBodyWrite} 再把体换成 {@link Result} 会触发
     * {@code Result cannot be cast to Resource} 的 ClassCastException —— 这几个端点恒 500
     * （TD-018 验收时实测 download/view/thumbnail/responsive 全挂）；二进制流本身也不是 API 信封。
     */
    private static boolean isBinaryResourceResponse(MethodParameter returnType) {
        ResolvableType type = ResolvableType.forMethodParameter(returnType);
        if (Resource.class.isAssignableFrom(type.toClass())) {
            return true;
        }
        ResolvableType body = type.getGeneric(0);
        return body.resolve() != null && Resource.class.isAssignableFrom(body.toClass());
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
