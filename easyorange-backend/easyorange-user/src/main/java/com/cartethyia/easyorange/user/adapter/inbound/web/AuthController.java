package com.cartethyia.easyorange.user.adapter.inbound.web;

import com.cartethyia.easyorange.common.annotation.RateLimiter;
import com.cartethyia.easyorange.common.annotation.RepeatSubmit;
import com.cartethyia.easyorange.common.enums.LimitType;
import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.LoginRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.RefreshTokenRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.RegisterRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.password.ForgotPasswordRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.LoginResponse;
import com.cartethyia.easyorange.user.application.service.auth.ForgotPasswordAppService;
import com.cartethyia.easyorange.user.application.service.auth.LoginAppService;
import com.cartethyia.easyorange.user.application.service.auth.RegisterAppService;
import com.cartethyia.easyorange.user.application.service.verification.SmsCodeAppService;
import com.cartethyia.easyorange.user.domain.constant.UserConstant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterAppService registerAppService;
    private final LoginAppService loginAppService;
    private final ForgotPasswordAppService forgotPasswordAppService;
    private final SmsCodeAppService smsCodeAppService;
    private final JwtProperties jwtProperties;

    @PostMapping("/register")
    @RateLimiter(key = "user:register", count = 5, limitType = LimitType.IP)
    @RepeatSubmit(interval = 5000, message = "请勿重复提交注册请求")
    public Result<Long> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(registerAppService.register(request));
    }

    @PostMapping("/login")
    @RateLimiter(key = "auth:login", count = 10, limitType = LimitType.IP)
    @RepeatSubmit(interval = 5000, message = "请勿重复提交登录请求")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return Result.success(loginAppService.login(loginRequest));
    }

    @PostMapping("/logout")
    @RepeatSubmit(message = "请勿重复提交")
    public Result<Void> logout(
            @RequestHeader(value = "Authorization") String authHeader,
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshTokenHeader) {
        BizRequire.notBlank(authHeader, ResultCode.UNAUTHORIZED);
        BizRequire.requireTrue(authHeader.startsWith(jwtProperties.getTokenPrefix()), ResultCode.UNAUTHORIZED);

        String accessToken = extractToken(authHeader);
        String refreshToken = null;

        if (refreshTokenHeader != null && refreshTokenHeader.startsWith(jwtProperties.getTokenPrefix())) {
            refreshToken = extractToken(refreshTokenHeader);
        }

        loginAppService.logout(accessToken, refreshToken);
        return Result.success();
    }

    @PostMapping("/refresh")
    @RateLimiter(key = "auth:refresh", count = 20, limitType = LimitType.IP)
    @RepeatSubmit(interval = 5000, message = "请勿重复刷新令牌")
    public Result<String> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return Result.success(loginAppService.refreshToken(request.refreshToken()));
    }

    @PostMapping("/sms-code")
    @RateLimiter(key = "auth:sms_code", count = 1, limitType = LimitType.IP)
    @RepeatSubmit(interval = 60000, message = "请勿频繁发送验证码")
    public Result<Void> sendSmsCode(
            @NotBlank(message = "手机号不能为空") @Pattern(regexp = UserConstant.PHONE_REGEX, message = "手机号格式不正确")
            @RequestParam String phone) {
        smsCodeAppService.sendSmsCode(phone);
        return Result.success();
    }

    @PostMapping("/password-reset")
    @RateLimiter(key = "user:forgot_password", count = 3, time = 3600, limitType = LimitType.IP)
    public Result<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        forgotPasswordAppService.forgotPassword(request);
        return Result.success();
    }

    private String extractToken(String authHeader) {
        BizRequire.notBlank(authHeader, ResultCode.UNAUTHORIZED);
        BizRequire.requireTrue(authHeader.startsWith(jwtProperties.getTokenPrefix()), ResultCode.UNAUTHORIZED);
        return authHeader.substring(jwtProperties.getTokenPrefix().length());
    }
}
