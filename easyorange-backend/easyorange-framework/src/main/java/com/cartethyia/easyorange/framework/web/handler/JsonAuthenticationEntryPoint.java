package com.cartethyia.easyorange.framework.web.handler;

import tools.jackson.databind.ObjectMapper;
import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String ERROR_MESSAGE = "认证失败，请重新登录";

    private final ObjectMapper objectMapper;

    public JsonAuthenticationEntryPoint(@Lazy ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull AuthenticationException authException) throws IOException {
        
        log.warn("认证失败 - method={}, uri={}, reason={}", 
                request.getMethod(), 
                request.getRequestURI(), 
                authException.getMessage());
        
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), Result.error(ResultCode.UNAUTHORIZED, ERROR_MESSAGE));
    }
}
