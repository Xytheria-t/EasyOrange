package com.cartethyia.easyorange.user.controller;

import com.cartethyia.easyorange.common.annotation.RepeatSubmit;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.user.converter.UserConverter;
import com.cartethyia.easyorange.user.dto.bo.UpdateUserBo;
import com.cartethyia.easyorange.user.dto.bo.UploadAvatarBo;
import com.cartethyia.easyorange.user.dto.request.UpdateUserRequest;
import com.cartethyia.easyorange.user.dto.vo.UserProfileVO;
import com.cartethyia.easyorange.user.dto.vo.UserVO;
import com.cartethyia.easyorange.user.service.user.UserService;
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
    public Result<UserProfileVO> getCurrentUser() {
        return Result.success(userService.getUserInfo());
    }

    @RepeatSubmit(interval = 3000, message = "请勿重复提交")
    @PutMapping("/info")
    public Result<UserVO> updateUserInfo(@Valid @RequestBody UpdateUserRequest request) {
        UpdateUserBo bo = userConverter.toBo(request);
        return Result.success(userService.updateUserInfo(bo));
    }

    @RepeatSubmit(interval = 3000, message = "请勿重复提交")
    @PostMapping("/avatar")
    public Result<UserVO> uploadAvatar(@RequestParam("avatar") MultipartFile avatar) {
        UploadAvatarBo bo = userConverter.toBo(avatar);
        return Result.success(userService.uploadAvatar(bo));
    }
}
