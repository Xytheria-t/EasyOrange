package com.cartethyia.easyorange.user.adapter.inbound.web.controller;


import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.password.ChangePasswordRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.profile.UpdateUserRequest;
import com.cartethyia.easyorange.user.application.command.ChangePasswordCommand;
import com.cartethyia.easyorange.user.application.command.UpdateUserCommand;
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

    @PutMapping("/me")
    public Result<UserVO> updateUserInfo(@Valid @RequestBody UpdateUserRequest request) {
        UpdateUserCommand command = new UpdateUserCommand(
            request.nickname(), request.email(), request.phone(),
            request.gender(), request.realName(), request.studentId()
        );
        return Result.success(profileAppService.updateUserInfo(command));
    }

    @PutMapping("/me/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        ChangePasswordCommand command = new ChangePasswordCommand(
            request.oldPassword(), request.newPassword()
        );
        changePasswordAppService.changePassword(command);
        return Result.success();
    }

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
