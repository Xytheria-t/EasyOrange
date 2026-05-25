package com.cartethyia.easyorange.user.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.annotation.RateLimiter;
import com.cartethyia.easyorange.common.annotation.RepeatSubmit;
import com.cartethyia.easyorange.common.enums.LimitType;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.password.ChangePasswordRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.profile.UpdateUserRequest;
import com.cartethyia.easyorange.user.application.dto.UserProfileVO;
import com.cartethyia.easyorange.user.application.dto.UserVO;
import com.cartethyia.easyorange.user.application.service.password.ChangePasswordAppService;
import com.cartethyia.easyorange.user.application.service.profile.ProfileAppService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final ProfileAppService profileAppService;
    private final ChangePasswordAppService changePasswordAppService;

    @GetMapping("/me")
    public Result<UserProfileVO> getCurrentUser() {
        return Result.success(profileAppService.getUserInfo());
    }

    @RepeatSubmit(interval = 3000, message = "请勿重复提交")
    @PutMapping("/me")
    public Result<UserVO> updateUserInfo(@Valid @RequestBody UpdateUserRequest request) {
        return Result.success(profileAppService.updateUserInfo(request));
    }

    @RateLimiter(key = "user:change_password", count = 5, time = 60, limitType = LimitType.IP)
    @RepeatSubmit(interval = 3000, message = "请勿重复提交")
    @PutMapping("/me/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        changePasswordAppService.changePassword(request);
        return Result.success();
    }

    @RepeatSubmit(interval = 3000, message = "请勿重复提交")
    @PostMapping("/avatar")
    public Result<UserVO> uploadAvatar(@RequestParam("avatar") MultipartFile avatar) {
        try {
            byte[] content = avatar.getBytes();
            String contentType = avatar.getContentType();
            String originalFilename = avatar.getOriginalFilename();
            return Result.success(profileAppService.uploadAvatar(content, contentType, originalFilename));
        } catch (java.io.IOException e) {
            throw new RuntimeException("头像读取失败", e);
        }
    }
}