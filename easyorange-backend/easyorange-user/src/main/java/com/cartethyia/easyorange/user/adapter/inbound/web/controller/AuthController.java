package com.cartethyia.easyorange.user.adapter.inbound.web.controller;


import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.framework.service.TokenRefreshResult;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.LoginRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.RefreshTokenRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.RegisterRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.password.ForgotPasswordRequest;
import com.cartethyia.easyorange.user.application.command.ForgotPasswordCommand;
import com.cartethyia.easyorange.user.application.command.LoginResult;
import com.cartethyia.easyorange.user.application.command.RegisterCommand;
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

    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody RegisterRequest request) {
        RegisterCommand command = new RegisterCommand(
                request.username(), request.password()
        );
        return Result.success(registerAppService.register(command));
    }

    @PostMapping("/login")
    public Result<LoginResult> login(@Valid @RequestBody LoginRequest loginRequest) {
        return Result.success(loginAppService.login(loginRequest.toCommand()));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        loginAppService.logout(request.refreshToken());
        return Result.success();
    }

    @PostMapping("/refresh")
    public Result<TokenRefreshResult> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        TokenRefreshResult result = loginAppService.refreshToken(request.refreshToken());
        if (result == null) {
            return Result.error(ResultCode.UNAUTHORIZED, "刷新令牌已失效，请重新登录");
        }
        return Result.success(result);
    }

    @PostMapping("/sms-code")
    public Result<Void> sendSmsCode(
            @NotBlank(message = "手机号不能为空") @Pattern(regexp = UserConstant.PHONE_REGEX, message = "手机号格式不正确")
            @RequestParam String phone) {
        smsCodeAppService.sendSmsCode(phone);
        return Result.success();
    }

    @PostMapping("/password-reset")
    public Result<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        ForgotPasswordCommand command = new ForgotPasswordCommand(
                request.phone(), request.verifyCode(), request.newPassword()
        );
        forgotPasswordAppService.forgotPassword(command);
        return Result.success();
    }

}
