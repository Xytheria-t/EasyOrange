package com.cartethyia.easyorange.user.controller;

import com.cartethyia.easyorange.common.annotation.RateLimiter;
import com.cartethyia.easyorange.common.annotation.RepeatSubmit;
import com.cartethyia.easyorange.common.enums.LimitType;
import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import com.cartethyia.easyorange.framework.service.TokenService;
import com.cartethyia.easyorange.user.dto.request.LoginRequest;
import com.cartethyia.easyorange.user.dto.response.LoginResponse;
import com.cartethyia.easyorange.user.service.strategy.LoginDispatcher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginDispatcher loginDispatcher;
    private final TokenService tokenService;
    private final JwtProperties jwtProperties;

    @RateLimiter(key = "auth:login", count = 10, time = 60, limitType = LimitType.IP)
    @RepeatSubmit(interval = 5000, message = "请勿重复提交登录请求")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return Result.success(loginDispatcher.login(loginRequest));
    }

    @RepeatSubmit(interval = 3000, message = "请勿重复提交")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null) {
            return Result.success();
        }
        BizRequire.isTrue(authHeader.startsWith(jwtProperties.getTokenPrefix()), ResultCode.UNAUTHORIZED);

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
