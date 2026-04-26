package com.cartethyia.easyorange.user.controller;

import com.cartethyia.easyorange.common.annotation.RateLimiter;
import com.cartethyia.easyorange.common.annotation.RepeatSubmit;
import com.cartethyia.easyorange.common.enums.LimitType;
import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.common.util.SecurityContextUtil;
import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import com.cartethyia.easyorange.framework.service.TokenService;
import com.cartethyia.easyorange.user.dto.request.LoginDTO;
import com.cartethyia.easyorange.user.dto.response.LoginResponse;
import com.cartethyia.easyorange.user.service.LoginStrategyContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginStrategyContext loginStrategyContext;
    private final TokenService tokenService;
    private final JwtProperties jwtProperties;

    @RateLimiter(key = "auth:login", count = 10, time = 60, limitType = LimitType.IP)
    @RepeatSubmit(interval = 5000, message = "请勿重复提交登录请求")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginDTO loginDTO) {
        return Result.success(loginStrategyContext.login(loginDTO));
    }

    @RepeatSubmit(interval = 3000, message = "请勿重复提交")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null) {
            return Result.success();
        }
        BizRequire.isTrue(authHeader.startsWith(jwtProperties.getTokenPrefix()), ResultCode.UNAUTHORIZED);
        
        Long userId = SecurityContextUtil.getCurrentUserId().orElse(null);
        log.info("action=logout, userId={}", userId);
        
        tokenService.delToken(extractToken(authHeader));
        
        SecurityContextUtil.clearContext();
        
        return Result.success();
    }

    @RateLimiter(key = "auth:refresh", count = 20, time = 60, limitType = LimitType.IP)
    @RepeatSubmit(interval = 5000, message = "请勿重复刷新令牌")
    @PostMapping("/refresh")
    public Result<String> refreshToken(@RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader);
        String newToken = tokenService.refreshToken(token);
        BizRequire.notNull(newToken, ResultCode.UNAUTHORIZED);
        return Result.success(newToken);
    }

    private String extractToken(String authHeader) {
        BizRequire.notBlank(authHeader, ResultCode.UNAUTHORIZED);
        BizRequire.isTrue(authHeader.startsWith(jwtProperties.getTokenPrefix()), ResultCode.UNAUTHORIZED);
        return authHeader.substring(jwtProperties.getTokenPrefix().length());
    }
}
