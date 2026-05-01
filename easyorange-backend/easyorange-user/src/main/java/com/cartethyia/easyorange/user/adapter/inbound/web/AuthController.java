package com.cartethyia.easyorange.user.adapter.inbound.web;

import com.cartethyia.easyorange.common.annotation.RateLimiter;
import com.cartethyia.easyorange.common.annotation.RepeatSubmit;
import com.cartethyia.easyorange.common.enums.LimitType;
import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.ForgotPasswordRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.LoginRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.RefreshTokenRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.RegisterRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.LoginResponse;
import com.cartethyia.easyorange.user.application.service.AuthAppService;
import com.cartethyia.easyorange.user.domain.shared.constant.UserConstant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthAppService authAppService;
    private final JwtProperties jwtProperties;

    @PostMapping("/register")
    @RateLimiter(key = "user:register", count = 5, limitType = LimitType.IP)
    @RepeatSubmit(interval = 5000, message = "请勿重复提交注册请求")
    public Result<Long> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authAppService.register(request));
    }

    @PostMapping("/login")
    @RateLimiter(key = "auth:login", count = 10, limitType = LimitType.IP)
    @RepeatSubmit(interval = 5000, message = "请勿重复提交登录请求")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return Result.success(authAppService.login(loginRequest));
    }

    @PostMapping("/logout")
    @RepeatSubmit(message = "请勿重复提交")
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
        authAppService.logout(accessToken, refreshToken);
        return Result.success();
    }

    @PostMapping("/refresh")
    @RateLimiter(key = "auth:refresh", count = 20, limitType = LimitType.IP)
    @RepeatSubmit(interval = 5000, message = "请勿重复刷新令牌")
    public Result<String> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return Result.success(authAppService.refreshToken(request.refreshToken()));
    }

    @PostMapping("/sms-code")
    @RateLimiter(key = "auth:sms_code", count = 1, limitType = LimitType.IP)
    @RepeatSubmit(interval = 60000, message = "请勿频繁发送验证码")
    public Result<Void> sendSmsCode(
            @NotBlank(message = "手机号不能为空") @Pattern(regexp = UserConstant.PHONE_REGEX, message = "手机号格式不正确")
            @RequestParam String phone) {
        authAppService.sendSmsCode(phone);
        return Result.success();
    }

    @PostMapping("/password-reset")
    @RateLimiter(key = "user:forgot_password", count = 3, time = 3600, limitType = LimitType.IP)
    public Result<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authAppService.forgotPassword(request);
        return Result.success();
    }

    private String extractToken(String authHeader) {
        BizRequire.notBlank(authHeader, ResultCode.UNAUTHORIZED);
        BizRequire.requireTrue(authHeader.startsWith(jwtProperties.getTokenPrefix()), ResultCode.UNAUTHORIZED);
        return authHeader.substring(jwtProperties.getTokenPrefix().length());
    }
}
