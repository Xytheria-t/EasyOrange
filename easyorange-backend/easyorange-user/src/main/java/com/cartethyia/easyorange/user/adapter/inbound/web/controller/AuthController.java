package com.cartethyia.easyorange.user.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.framework.auth.TokenRefreshResult;
import com.cartethyia.easyorange.user.adapter.inbound.web.assembler.UserAssembler;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.ChangePasswordRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.PasswordLoginRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.PasswordResetRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.RefreshTokenRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.RegisterRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.SmsLoginRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.LoginResult;
import com.cartethyia.easyorange.user.application.service.AuthAppService;
import com.cartethyia.easyorange.user.domain.constant.UserConstant;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证授权", description = "登录/注册/刷新令牌/密码重置")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthAppService authAppService;
    private final UserAssembler userAssembler;

    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authAppService.register(request.username(), request.password()));
    }

    @PostMapping("/login")
    public Result<LoginResult> login(@Valid @RequestBody PasswordLoginRequest request) {
        var ctx = authAppService.login(request.toCredential());
        return Result.success(userAssembler.toLoginResult(ctx.user(), ctx.accessToken(), ctx.refreshToken()));
    }

    @PostMapping("/sms-login")
    public Result<LoginResult> smsLogin(@Valid @RequestBody SmsLoginRequest request) {
        var ctx = authAppService.login(request.toCredential());
        return Result.success(userAssembler.toLoginResult(ctx.user(), ctx.accessToken(), ctx.refreshToken()));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authAppService.logout(request.refreshToken());
        return Result.success();
    }

    @PostMapping("/refresh")
    public Result<TokenRefreshResult> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return Result.success(authAppService.refreshToken(request.refreshToken()));
    }

    // ==== SMS Code ====

    @PostMapping("/sms-code")
    public Result<Void> sendSmsCode(
            @NotBlank(message = "手机号不能为空") @Pattern(regexp = UserConstant.PHONE_REGEX, message = "手机号格式不正确") @RequestParam String phone) {
        authAppService.sendSmsCode(phone);
        return Result.success();
    }

    // ==== Password Management ====

    @PostMapping("/password/reset")
    public Result<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authAppService.resetPassword(request.phone(), request.verifyCode(), request.newPassword());
        return Result.success();
    }

    @PutMapping("/password/change")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authAppService.changePassword(request.oldPassword(), request.newPassword());
        return Result.success();
    }
}
