package com.cartethyia.easyorange.user.controller;

import com.cartethyia.easyorange.common.annotation.RateLimiter;
import com.cartethyia.easyorange.common.annotation.RepeatSubmit;
import com.cartethyia.easyorange.common.enums.LimitType;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.user.application.command.ChangePasswordCommand;
import com.cartethyia.easyorange.user.application.command.ForgotPasswordCommand;
import com.cartethyia.easyorange.user.application.command.RegisterUserCommand;
import com.cartethyia.easyorange.user.application.command.UpdateUserCommand;
import com.cartethyia.easyorange.user.application.handler.ChangePasswordHandler;
import com.cartethyia.easyorange.user.application.handler.ForgotPasswordHandler;
import com.cartethyia.easyorange.user.application.handler.GetUserHandler;
import com.cartethyia.easyorange.user.application.handler.RegisterUserHandler;
import com.cartethyia.easyorange.user.application.handler.UpdateUserHandler;
import com.cartethyia.easyorange.user.application.query.GetUserQuery;
import com.cartethyia.easyorange.user.domain.valueobject.UserId;
import com.cartethyia.easyorange.user.dto.request.ChangePasswordRequest;
import com.cartethyia.easyorange.user.dto.request.ForgotPasswordRequest;
import com.cartethyia.easyorange.user.dto.request.RegisterRequest;
import com.cartethyia.easyorange.user.dto.request.UpdateUserRequest;
import com.cartethyia.easyorange.user.dto.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final RegisterUserHandler registerUserHandler;
    private final UpdateUserHandler updateUserHandler;
    private final ChangePasswordHandler changePasswordHandler;
    private final ForgotPasswordHandler forgotPasswordHandler;
    private final GetUserHandler getUserHandler;

    @GetMapping("/info")
    public Result<UserVO> getUserInfo() {
        GetUserQuery query = new GetUserQuery();
        return Result.success(getUserHandler.handle(query));
    }

    @RateLimiter(key = "user:register", count = 5, time = 60, limitType = LimitType.IP)
    @RepeatSubmit(interval = 5000, message = "请勿重复提交注册请求")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        RegisterUserCommand command = RegisterUserCommand.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .build();
        registerUserHandler.handle(command);
        return Result.success();
    }

    @RepeatSubmit(interval = 3000, message = "请勿重复提交")
    @PutMapping("/info")
    public Result<UserVO> updateUserInfo(@Valid @RequestBody UpdateUserRequest request) {
        UpdateUserCommand command = UpdateUserCommand.builder()
                .email(request.getEmail())
                .phone(request.getPhone())
                .gender(request.getGender())
                .build();
        return Result.success(updateUserHandler.handle(command));
    }

    @RepeatSubmit(interval = 3000, message = "请勿重复提交")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        ChangePasswordCommand command = ChangePasswordCommand.builder()
                .oldPassword(request.getOldPassword())
                .newPassword(request.getNewPassword())
                .build();
        changePasswordHandler.handle(command);
        return Result.success();
    }

    @RateLimiter(key = "forgot_password", count = 3, time = 3600, limitType = LimitType.IP)
    @PostMapping("/forgotPassword")
    public Result<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        ForgotPasswordCommand command = ForgotPasswordCommand.builder()
                .phone(request.getPhone())
                .newPassword(request.getNewPassword())
                .build();
        forgotPasswordHandler.handle(command);
        return Result.success();
    }
}