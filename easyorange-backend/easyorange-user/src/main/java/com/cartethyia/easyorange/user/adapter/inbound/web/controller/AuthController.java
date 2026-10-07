package com.cartethyia.easyorange.user.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.annotation.SkipRepeatSubmit;
import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.security.AuthUser;
import com.cartethyia.easyorange.framework.auth.TokenRefreshResult;
import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import com.cartethyia.easyorange.framework.web.cookie.RefreshCookie;
import com.cartethyia.easyorange.user.adapter.inbound.web.assembler.UserAssembler;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.ChangePasswordRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.PasswordLoginRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.PasswordResetRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.RegisterRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.SmsCodeVerifyRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.SmsLoginRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.LoginResult;
import com.cartethyia.easyorange.user.application.service.AuthAppService;
import com.cartethyia.easyorange.user.application.service.CredentialAppService;
import com.cartethyia.easyorange.user.domain.constant.UserConstant;
import com.cartethyia.easyorange.user.domain.valueobject.LoginCredential;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    @Operation(summary = "手机号 + 验证码注册，先查重再消费验证码；昵称默认取用户名")
    public Result<String> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(
                authAppService.register(request.username(), request.password(), request.phone(), request.verifyCode()));
    }

    // 登录豁免防重：防重按「同请求体 3s」拦，会把演示里的连续输错计数打断（第 2-5 次错误密码被 429 吞掉，
    // 5 次锁定永不触发）；爆破防护由 5 次失败锁定 + 限流承担，重复同密码本就无爆破价值
    @SkipRepeatSubmit
    @PostMapping("/login")
    @Operation(summary = "密码登录；连错 5 次锁定 30 分钟，Refresh 走 HttpOnly Cookie")
    public Result<LoginResult> login(@Valid @RequestBody PasswordLoginRequest request, HttpServletResponse response) {
        return doLogin(request.toCredential(), response);
    }

    @SkipRepeatSubmit
    @PostMapping("/sms-login")
    @Operation(summary = "短信验证码登录（消费验证码）；手机号未注册或账号禁用一律拒绝")
    public Result<LoginResult> smsLogin(@Valid @RequestBody SmsLoginRequest request, HttpServletResponse response) {
        return doLogin(request.toCredential(), response);
    }

    private Result<LoginResult> doLogin(LoginCredential credential, HttpServletResponse response) {
        var ctx = authAppService.login(credential);
        refreshCookie.write(response, ctx.refreshToken());
        return Result.success(userAssembler.toLoginResult(ctx.user(), ctx.accessToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "登出：吊销 access 黑名单与 Refresh 会话、清 Cookie；缺令牌也返回成功（幂等）")
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
    @Operation(summary = "刷新令牌：轮换 Refresh 并做复用检测；账号不存在或禁用则吊销该用户全部会话并 401")
    public Result<TokenRefreshResult> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        var refreshToken = readRefreshTokenCookie(request);
        if (refreshToken == null) {
            // 刷新令牌缺失 = 未认证：401 + A0401，前端按 HTTP 401 触发重新登录流程
            throw BusinessException.of(ResultCode.UNAUTHORIZED, "刷新令牌缺失，请重新登录");
        }
        var result = authAppService.refreshToken(refreshToken);
        refreshCookie.write(response, result.refreshToken());
        return Result.success(new TokenRefreshResult(result.accessToken()));
    }

    // ── 短信验证码 ──

    @PostMapping("/sms-code")
    @Operation(summary = "下发短信验证码（5 分钟有效）；60 秒内重复请求与超每日 10 条上限被拒")
    public Result<Void> sendSmsCode(
            @NotBlank(message = "手机号不能为空")
                    @Pattern(regexp = UserConstant.PHONE_REGEX, message = "手机号格式不正确")
                    @RequestParam
                    String phone) {
        authAppService.sendSmsCode(phone);
        return Result.success();
    }

    /** 验证码预检（不消费）— 忘记密码流程第二步即时校验，最终重置时再真正消费。 */
    @PostMapping("/sms-code/verify")
    @Operation(summary = "验证码预检（不消费），供忘记密码第二步即时反馈；最终重置时再消费")
    public Result<Void> verifySmsCode(@Valid @RequestBody SmsCodeVerifyRequest request) {
        authAppService.verifySmsCode(request.phone(), request.verifyCode());
        return Result.success();
    }

    // ── 密码管理 ──

    @PostMapping("/password/reset")
    @Operation(summary = "验证码找回密码：消费验证码后落库，并吊销该用户全部会话（不要求登录态）")
    public Result<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        credentialAppService.resetPassword(request.phone(), request.verifyCode(), request.newPassword());
        return Result.success();
    }

    @PutMapping("/password/change")
    @Operation(summary = "登录态自助改密：校验旧密码、新旧不得相同；成功后吊销全部会话并清 Refresh")
    public Result<Void> changePassword(
            @AuthenticationPrincipal AuthUser user, @Valid @RequestBody ChangePasswordRequest request) {
        credentialAppService.changePassword(user.userId(), request.oldPassword(), request.newPassword());
        return Result.success();
    }

    // ── 辅助方法 ──

    private String readRefreshTokenCookie(HttpServletRequest request) {
        var cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        var name = jwtProperties.refreshCookieName();
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
