package com.cartethyia.easyorange.framework.config.security;

import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.util.SecurityContextUtil;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonLogoutSuccessHandler implements LogoutSuccessHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void onLogoutSuccess(@NonNull HttpServletRequest request, HttpServletResponse response,
                                org.springframework.security.core.Authentication authentication)
            throws IOException {
        Long userId = SecurityContextUtil.getCurrentUserId().orElse(null);
        log.info("action=logout_success, userId={}", userId);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("utf-8");
        objectMapper.writeValue(response.getOutputStream(), Result.success("退出成功"));
    }
}
