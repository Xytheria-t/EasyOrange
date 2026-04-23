package com.cartethyia.easyorange.user.controller;

import com.cartethyia.easyorange.common.annotation.RateLimiter;
import com.cartethyia.easyorange.common.annotation.RepeatSubmit;
import com.cartethyia.easyorange.common.enums.LimitType;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.user.dto.request.ChangePasswordRequest;
import com.cartethyia.easyorange.user.dto.request.ForgotPasswordRequest;
import com.cartethyia.easyorange.user.dto.request.RegisterRequest;
import com.cartethyia.easyorange.user.dto.request.UpdateUserRequest;
import com.cartethyia.easyorange.user.dto.vo.UserVO;
import com.cartethyia.easyorange.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/info")
    public Result<UserVO> getUserInfo() {
        return Result.success(userService.getUserInfo());
    }

    @RateLimiter(key = "user:register", count = 5, time = 60, limitType = LimitType.IP)
    @RepeatSubmit(interval = 5000, message = "请勿重复提交注册请求")
    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(userService.register(request));
    }

    @RepeatSubmit(interval = 3000, message = "请勿重复提交")
    @PutMapping("/info")
    public Result<UserVO> updateUserInfo(@Valid @RequestBody UpdateUserRequest request) {
        return Result.success(userService.updateUserInfo(request));
    }

    @RepeatSubmit(interval = 3000, message = "请勿重复提交")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return Result.success();
    }

    @RateLimiter(key = "forgot_password", count = 3, time = 3600, limitType = LimitType.IP)
    @PostMapping("/forgotPassword")
    public Result<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        userService.forgotPassword(request);
        return Result.success();
    }
}