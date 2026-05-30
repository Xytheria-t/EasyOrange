package com.cartethyia.easyorange.user.adapter.inbound.web.controller;


import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.framework.service.TokenRefreshResult;
import com.cartethyia.easyorange.framework.service.TokenService;
import com.cartethyia.easyorange.user.adapter.inbound.web.assembler.UserAssembler;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.PasswordLoginRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.RefreshTokenRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.SmsLoginRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.RegisterRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.ForgotPasswordRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.LoginResult;
import com.cartethyia.easyorange.user.application.service.AuthAppService;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.adapter.inbound.web.validation.Phone;
import com.cartethyia.easyorange.user.domain.service.SmsCodeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthAppService authAppService;
    private final SmsCodeService smsCodeService;
    private final TokenService tokenService;
    private final UserAssembler userAssembler;

    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody RegisterRequest request) {
        Long userId = authAppService.register(request.username(), request.password());
        return Result.success(userId);
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
        TokenRefreshResult result = authAppService.refreshToken(request.refreshToken());
        if (result == null) {
            return Result.error(ResultCode.UNAUTHORIZED, "刷新令牌已失效，请重新登录");
        }
        return Result.success(result);
    }

    @PostMapping("/sms-code")
    public Result<Void> sendSmsCode(
            @NotBlank(message = "手机号不能为空") @Phone @RequestParam String phone) {
        smsCodeService.sendCode(phone);
        return Result.success();
    }

    @PostMapping("/password-reset")
    public Result<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authAppService.forgotPassword(request.phone(), request.verifyCode(), request.newPassword());
        return Result.success();
    }

    private LoginResult buildLoginResult(User user) {
        String userTypeCode = user.getUserType() != null ? user.getUserType().getCode() : null;
        String accessToken = tokenService.createAccessToken(user.getId(), user.getUsername(), userTypeCode);
        String refreshToken = tokenService.createRefreshToken(user.getId(), user.getUsername(), userTypeCode);
        return userAssembler.toLoginResult(user, accessToken, refreshToken);
    }
}
