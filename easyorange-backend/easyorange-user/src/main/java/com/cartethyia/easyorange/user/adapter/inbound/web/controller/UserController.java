package com.cartethyia.easyorange.user.adapter.inbound.web.controller;


import com.cartethyia.easyorange.common.dto.AuthUser;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.user.adapter.inbound.web.assembler.UserAssembler;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.profile.UpdateProfileRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.UserProfileResponse;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.UserResponse;
import com.cartethyia.easyorange.user.application.service.ProfileAppService;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final ProfileAppService profileAppService;
    private final UserAssembler userAssembler;

    @GetMapping("/me")
    public Result<UserProfileResponse> getCurrentUser() {
        AuthUser authUser = SecurityContextUtil.getUserContextOrThrow();
        User user = profileAppService.getCurrentUser();
        UserProfileResponse response = userAssembler.toProfileResponse(
            user, authUser.roles(), authUser.permissions(), authUser.loginTime());
        return Result.success(response);
    }

    @PutMapping("/me")
    public Result<UserResponse> updateUserInfo(@Valid @RequestBody UpdateProfileRequest request) {
        profileAppService.updateUserInfo(
            request.nickname(), request.email(), request.phone(),
            request.gender(), request.realName(), request.studentId());
        return Result.success(userAssembler.toResponse(profileAppService.getCurrentUser()));
    }

    @PostMapping("/avatar")
    public Result<UserResponse> uploadAvatar(@RequestParam("avatar") MultipartFile avatar) {
        try {
            byte[] content = avatar.getBytes();
            String contentType = avatar.getContentType();
            String originalFilename = avatar.getOriginalFilename();
            profileAppService.uploadAvatar(content, contentType, originalFilename);
            return Result.success(userAssembler.toResponse(profileAppService.getCurrentUser()));
        } catch (java.io.IOException e) {
            throw new RuntimeException("头像读取失败", e);
        }
    }
}
