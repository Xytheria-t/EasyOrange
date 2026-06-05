package com.cartethyia.easyorange.user.adapter.inbound.web.controller;


import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.framework.auth.TokenRefreshResult;
import com.cartethyia.easyorange.framework.auth.TokenService;
import com.cartethyia.easyorange.user.adapter.inbound.web.assembler.UserAssembler;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.ChangePasswordRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.PasswordLoginRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.PasswordResetRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.RefreshTokenRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.RegisterRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.SmsLoginRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.LoginResult;
import com.cartethyia.easyorange.user.adapter.inbound.web.validation.Phone;
import com.cartethyia.easyorange.user.application.service.AuthAppService;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.port.SmsCodePort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthAppService authAppService;
    private final SmsCodePort smsCodePort;
    private final TokenService tokenService;
    private final UserAssembler userAssembler;

    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authAppService.register(request.username(), request.password()));
    }

    @PostMapping("/login")
    public Result<LoginResult> login(@Valid @RequestBody PasswordLoginRequest request) {
        return Result.success(buildLoginResult(authAppService.login(request.toCredential())));
    }

    @PostMapping("/sms-login")
    public Result<LoginResult> smsLogin(@Valid @RequestBody SmsLoginRequest request) {
        return Result.success(buildLoginResult(authAppService.login(request.toCredential())));
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
            @NotBlank(message = "手机号不能为空") @Phone @RequestParam String phone) {
        if (!smsCodePort.send(phone)) {
            throw BusinessException.of(UserResultCode.SMS_CODE_SEND_TOO_FREQUENT);
        }
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
        authAppService.changePassword(request.verifyCode(), request.newPassword());
        return Result.success();
    }

    private LoginResult buildLoginResult(User user) {
        String accessToken = tokenService.createAccessToken(user.getId(), user.getUsername(), user.getUserType().getCode());
        String refreshToken = tokenService.createRefreshToken(user.getId(), user.getUsername(), user.getUserType().getCode());
        return userAssembler.toLoginResult(user, accessToken, refreshToken);
    }
}
