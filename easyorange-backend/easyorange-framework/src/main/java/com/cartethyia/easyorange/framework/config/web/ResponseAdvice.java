package com.cartethyia.easyorange.framework.config.web;

import com.cartethyia.easyorange.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import jakarta.annotation.Nullable;

import java.util.Objects;

@RestControllerAdvice(basePackages = "com.cartethyia.easyorange")
public class ResponseAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    public ResponseAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, @Nullable Class<? extends HttpMessageConverter<?>> converterType) {
        return !Result.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    @Nullable
    public Object beforeBodyWrite(@Nullable Object body, @Nullable MethodParameter returnType,
                                  @Nullable MediaType selectedContentType,
                                  @Nullable Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  @Nullable ServerHttpRequest request, @Nullable ServerHttpResponse response) {
        switch (body) {
            case null -> {
                return Result.success();
            }
            case Result<?> ignored -> {
                return body;
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
