package com.cartethyia.easyorange.user.controller;

import com.cartethyia.easyorange.common.annotation.RateLimiter;
import com.cartethyia.easyorange.common.annotation.RepeatSubmit;
import com.cartethyia.easyorange.common.dto.AuthUser;
import com.cartethyia.easyorange.common.enums.LimitType;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.util.SecurityContextUtil;
import com.cartethyia.easyorange.user.converter.UserConverter;
import com.cartethyia.easyorange.user.dto.bo.*;
import com.cartethyia.easyorange.user.dto.request.ChangePasswordRequest;
import com.cartethyia.easyorange.user.dto.request.ForgotPasswordRequest;
import com.cartethyia.easyorange.user.dto.request.RegisterRequest;
import com.cartethyia.easyorange.user.dto.request.UpdateUserRequest;
import com.cartethyia.easyorange.user.dto.vo.UserVO;
import com.cartethyia.easyorange.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserConverter userConverter;

    @GetMapping("/me")
    public Result<AuthUser> getCurrentUser() {
        AuthUser authUser = SecurityContextUtil.getUserContextOrThrow();
        return Result.success(authUser);
    }

    @GetMapping("/info")
    public Result<UserVO> getUserInfo() {
        return Result.success(userService.getUserInfo());
    }

    @RateLimiter(key = "user:register", count = 5, time = 60, limitType = LimitType.IP)
    @RepeatSubmit(interval = 5000, message = "请勿重复提交注册请求")
    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody RegisterRequest request) {
        RegisterBo bo = userConverter.toBo(request);
        return Result.success(userService.register(bo));
    }

    @RepeatSubmit(interval = 3000, message = "请勿重复提交")
    @PutMapping("/info")
    public Result<UserVO> updateUserInfo(@Valid @RequestBody UpdateUserRequest request) {
        UpdateUserBo bo = userConverter.toBo(request);
        return Result.success(userService.updateUserInfo(bo));
    }

    @RepeatSubmit(interval = 3000, message = "请勿重复提交")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        ChangePasswordBo bo = userConverter.toBo(request);
        userService.changePassword(bo);
        return Result.success();
    }

    @RateLimiter(key = "forgot_password", count = 3, time = 3600, limitType = LimitType.IP)
    @PostMapping("/forgotPassword")
    public Result<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        ForgotPasswordBo bo = userConverter.toBo(request);
        userService.forgotPassword(bo);
        return Result.success();
    }

    @GetMapping("/check-auth")
    public Result<Boolean> checkAuthentication() {
        boolean isAuthenticated = SecurityContextUtil.isAuthenticated();
        return Result.success(isAuthenticated);
    }

    @GetMapping("/check-role/{role}")
    public Result<Boolean> checkRole(@PathVariable String role) {
        boolean hasRole = SecurityContextUtil.hasRole(role);
        return Result.success(hasRole);
    }

    @GetMapping("/check-authority/{authority}")
    public Result<Boolean> checkAuthority(@PathVariable String authority) {
        boolean hasAuthority = SecurityContextUtil.hasAuthority(authority);
        return Result.success(hasAuthority);
    }

    @PostMapping("/admin-only-action")
    public Result<Void> adminOnlyAction() {
        if (!SecurityContextUtil.hasRole("ADMIN")) {
            throw BusinessException.of("需要管理员权限才能执行此操作");
        }
        return Result.success();
    }

    @RepeatSubmit(interval = 3000, message = "请勿重复提交")
    @PostMapping("/avatar")
    public Result<UserVO> uploadAvatar(@RequestParam("avatar") MultipartFile avatar) {
        UploadAvatarBo bo = userConverter.toBo(avatar);
        return Result.success(userService.uploadAvatar(bo));
    }
}
