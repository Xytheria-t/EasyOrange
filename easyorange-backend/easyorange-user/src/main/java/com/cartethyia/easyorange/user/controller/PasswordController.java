package com.cartethyia.easyorange.user.controller;

import com.cartethyia.easyorange.common.annotation.RateLimiter;
import com.cartethyia.easyorange.common.annotation.RepeatSubmit;
import com.cartethyia.easyorange.common.enums.LimitType;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.user.converter.UserConverter;
import com.cartethyia.easyorange.user.dto.bo.ChangePasswordBo;
import com.cartethyia.easyorange.user.dto.bo.ForgotPasswordBo;
import com.cartethyia.easyorange.user.dto.bo.RegisterBo;
import com.cartethyia.easyorange.user.dto.request.ChangePasswordRequest;
import com.cartethyia.easyorange.user.dto.request.ForgotPasswordRequest;
import com.cartethyia.easyorange.user.dto.request.RegisterRequest;
import com.cartethyia.easyorange.user.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/password")
@RequiredArgsConstructor
public class PasswordController {

    private final UserService userService;
    private final UserConverter userConverter;

    @RateLimiter(key = "user:register", count = 5, time = 60, limitType = LimitType.IP)
    @RepeatSubmit(interval = 5000, message = "请勿重复提交注册请求")
    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody RegisterRequest request) {
        RegisterBo bo = userConverter.toBo(request);
        return Result.success(userService.register(bo));
    }

    @RateLimiter(key = "user:forgot_password", count = 3, time = 3600, limitType = LimitType.IP)
    @PostMapping("/forgot")
    public Result<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        ForgotPasswordBo bo = userConverter.toBo(request);
        userService.forgotPassword(bo);
        return Result.success();
    }

    @RateLimiter(key = "user:change_password", count = 5, time = 60, limitType = LimitType.IP)
    @RepeatSubmit(interval = 3000, message = "请勿重复提交")
    @PutMapping("/change")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        ChangePasswordBo bo = userConverter.toBo(request);
        userService.changePassword(bo);
        return Result.success();
    }
}
