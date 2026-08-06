package com.cartethyia.easyorange.user.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.annotation.SkipRepeatSubmit;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.framework.auth.TokenRefreshResult;
import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import com.cartethyia.easyorange.framework.web.cookie.RefreshCookie;
import com.cartethyia.easyorange.user.adapter.inbound.web.assembler.UserAssembler;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.ChangePasswordRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.PasswordLoginRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.PasswordResetRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.RegisterRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.SmsLoginRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.LoginResult;
import com.cartethyia.easyorange.user.application.service.AuthAppService;
import com.cartethyia.easyorange.user.application.service.CredentialAppService;
import com.cartethyia.easyorange.user.domain.constant.UserConstant;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    private final CredentialAppService credentialAppService;
    private final UserAssembler userAssembler;
    private final RefreshCookie refreshCookie;
    private final JwtProperties jwtProperties;

    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authAppService.register(request.username(), request.password()));
    }

    @PostMapping("/login")
    public Result<LoginResult> login(@Valid @RequestBody PasswordLoginRequest request, HttpServletResponse response) {
        var ctx = authAppService.login(request.toCredential());
        refreshCookie.write(response, ctx.refreshToken());
        return Result.success(userAssembler.toLoginResult(ctx.user(), ctx.accessToken()));
    }

    @PostMapping("/sms-login")
    public Result<LoginResult> smsLogin(@Valid @RequestBody SmsLoginRequest request, HttpServletResponse response) {
        var ctx = authAppService.login(request.toCredential());
        refreshCookie.write(response, ctx.refreshToken());
        return Result.success(userAssembler.toLoginResult(ctx.user(), ctx.accessToken()));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        // logout 需认证（已从 ignore-paths 移除），access 过期时由前端先刷新再调用；
        // RefreshCsrfFilter 仍校验 X-Client-Type 防 CSRF。吊销 access(黑名单) + refresh + 清 cookie。
        authAppService.logout(readBearerToken(request), readRefreshTokenCookie(request));
        refreshCookie.clear(response);
        return Result.success();
    }

    /**
     * 刷新令牌。跳过防重提交：token 刷新是幂等的受信交换，访问令牌过期时客户端会
     * 在窗口内合法地连续调用，硬防重（3s）会误伤并级联登出；Redis 限流仍兜底防滥用。
     */
    @SkipRepeatSubmit
    @PostMapping("/refresh")
    public Result<TokenRefreshResult> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        var refreshToken = readRefreshTokenCookie(request);
        if (refreshToken == null) {
            return Result.error("刷新令牌缺失，请重新登录");
        }
        var result = authAppService.refreshToken(refreshToken);
        refreshCookie.write(response, result.refreshToken());
        return Result.success(new TokenRefreshResult(result.accessToken()));
    }

    // ==== SMS Code ====

    @PostMapping("/sms-code")
    public Result<Void> sendSmsCode(
            @NotBlank(message = "手机号不能为空")
                    @Pattern(regexp = UserConstant.PHONE_REGEX, message = "手机号格式不正确")
                    @RequestParam
                    String phone) {
        authAppService.sendSmsCode(phone);
        return Result.success();
    }

    // ==== Password Management ====

    @PostMapping("/password/reset")
    public Result<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        credentialAppService.resetPassword(request.phone(), request.verifyCode(), request.newPassword());
        return Result.success();
    }

    @PutMapping("/password/change")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        credentialAppService.changePassword(request.oldPassword(), request.newPassword());
        return Result.success();
    }

    // ==================== Helpers ====================

    private String readRefreshTokenCookie(HttpServletRequest request) {
        var cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        var name = jwtProperties.getRefreshCookieName();
        for (var c : cookies) {
            if (name.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    private String readBearerToken(HttpServletRequest request) {
        var header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring("Bearer ".length());
        }
        return null;
    }
}
