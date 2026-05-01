package com.cartethyia.easyorange.user.adapter.inbound.web;

import com.cartethyia.easyorange.common.annotation.RateLimiter;
import com.cartethyia.easyorange.common.annotation.RepeatSubmit;
import com.cartethyia.easyorange.common.enums.LimitType;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.ChangePasswordRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.UpdateUserRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.UserProfileVO;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.UserVO;
import com.cartethyia.easyorange.user.application.service.UserAppService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserAppService userAppService;

    @GetMapping("/me")
    public Result<UserProfileVO> getCurrentUser() {
        return Result.success(userAppService.getUserInfo());
    }

    @RepeatSubmit(interval = 3000, message = "请勿重复提交")
    @PutMapping("/me")
    public Result<UserVO> updateUserInfo(@Valid @RequestBody UpdateUserRequest request) {
        return Result.success(userAppService.updateUserInfo(request));
    }

    @RateLimiter(key = "user:change_password", count = 5, time = 60, limitType = LimitType.IP)
    @RepeatSubmit(interval = 3000, message = "请勿重复提交")
    @PutMapping("/me/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userAppService.changePassword(request);
        return Result.success();
    }

    @RepeatSubmit(interval = 3000, message = "请勿重复提交")
    @PostMapping("/avatar")
    public Result<UserVO> uploadAvatar(@RequestParam("avatar") MultipartFile avatar) {
        return Result.success(userAppService.uploadAvatar(avatar));
    }
}
