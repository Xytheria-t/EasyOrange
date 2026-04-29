package com.cartethyia.easyorange.user.controller;

import com.cartethyia.easyorange.common.annotation.RateLimiter;
import com.cartethyia.easyorange.common.annotation.RepeatSubmit;
import com.cartethyia.easyorange.common.enums.LimitType;
import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import com.cartethyia.easyorange.user.converter.UserConverter;
import com.cartethyia.easyorange.user.dto.bo.ForgotPasswordBo;
import com.cartethyia.easyorange.user.dto.bo.RegisterBo;
import com.cartethyia.easyorange.user.dto.request.ForgotPasswordRequest;
import com.cartethyia.easyorange.user.dto.request.LoginRequest;
import com.cartethyia.easyorange.user.dto.request.RefreshTokenRequest;
import com.cartethyia.easyorange.user.dto.request.RegisterRequest;
import com.cartethyia.easyorange.user.dto.response.LoginResponse;
import com.cartethyia.easyorange.user.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;
    private final UserConverter userConverter;

    @PostMapping("/register")
    @RateLimiter(key = "user:register", count = 5, time = 60, limitType = LimitType.IP)
    @RepeatSubmit(interval = 5000, message = "请勿重复提交注册请求")
    public Result<Long> register(@Valid @RequestBody RegisterRequest request) {
        RegisterBo bo = userConverter.toBo(request);
        return Result.success(authService.register(bo));
    }

    @PostMapping("/login")
    @RateLimiter(key = "auth:login", count = 10, time = 60, limitType = LimitType.IP)
    @RepeatSubmit(interval = 5000, message = "请勿重复提交登录请求")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return Result.success(authService.login(loginRequest));
    }

    @PostMapping("/logout")
    @RepeatSubmit(interval = 3000, message = "请勿重复提交")
    public Result<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshTokenHeader) {
        String accessToken = null;
        String refreshToken = null;
        if (authHeader != null) {
            BizRequire.requireTrue(authHeader.startsWith(jwtProperties.getTokenPrefix()), ResultCode.UNAUTHORIZED);
            accessToken = extractToken(authHeader);
            if (refreshTokenHeader != null && refreshTokenHeader.startsWith(jwtProperties.getTokenPrefix())) {
                refreshToken = extractToken(refreshTokenHeader);
            }
        }
        authService.logout(accessToken, refreshToken);
        return Result.success();
    }

    @PostMapping("/refresh")
    @RateLimiter(key = "auth:refresh", count = 20, time = 60, limitType = LimitType.IP)
    @RepeatSubmit(interval = 5000, message = "请勿重复刷新令牌")
    public Result<String> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return Result.success(authService.refreshToken(request.getRefreshToken()));
    }

    @PostMapping("/password-reset")
    @RateLimiter(key = "user:forgot_password", count = 3, time = 3600, limitType = LimitType.IP)
    public Result<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        ForgotPasswordBo bo = userConverter.toBo(request);
        authService.forgotPassword(bo);
        return Result.success();
    }

    private String extractToken(String authHeader) {
        BizRequire.notBlank(authHeader, ResultCode.UNAUTHORIZED);
        BizRequire.requireTrue(authHeader.startsWith(jwtProperties.getTokenPrefix()), ResultCode.UNAUTHORIZED);
        return authHeader.substring(jwtProperties.getTokenPrefix().length());
    }
}