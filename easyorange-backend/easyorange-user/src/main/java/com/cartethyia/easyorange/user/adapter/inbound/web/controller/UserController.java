package com.cartethyia.easyorange.user.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.security.AuthUser;
import com.cartethyia.easyorange.user.adapter.inbound.web.assembler.UserAssembler;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.profile.UpdateProfileRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.UserProfileResponse;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.UserResponse;
import com.cartethyia.easyorange.user.application.service.ProfileAppService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "用户中心", description = "用户资料管理")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final ProfileAppService profileAppService;
    private final UserAssembler userAssembler;

    @GetMapping("/me")
    public Result<UserProfileResponse> getCurrentUser(@AuthenticationPrincipal AuthUser authUser) {
        return Result.success(userAssembler.toProfileResponse(profileAppService.getCurrentUser(authUser.userId())));
    }

    @PutMapping("/me")
    public Result<UserResponse> updateUserInfo(
            @AuthenticationPrincipal AuthUser user, @Valid @RequestBody UpdateProfileRequest request) {
        var cmd = new ProfileAppService.UpdateCommand(
                request.nickname(),
                request.email(),
                request.phone(),
                request.gender(),
                request.realName(),
                request.studentId());
        return Result.success(userAssembler.toResponse(profileAppService.updateUserInfo(user.userId(), cmd)));
    }

    @PostMapping("/avatar")
    public Result<UserResponse> uploadAvatar(
            @AuthenticationPrincipal AuthUser user, @RequestParam("avatar") MultipartFile avatar) throws IOException {
        var updatedUser = profileAppService.uploadAvatar(
                user.userId(), avatar.getBytes(), avatar.getContentType(), avatar.getOriginalFilename());
        return Result.success(userAssembler.toResponse(updatedUser));
    }
}
