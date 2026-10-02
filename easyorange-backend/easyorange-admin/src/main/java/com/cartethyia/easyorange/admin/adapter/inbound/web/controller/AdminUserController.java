package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import com.cartethyia.easyorange.admin.adapter.inbound.web.assembler.AdminUserAssembler;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminUserQueryRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.ResetPasswordRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.UpdateStatusRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.UserRoleRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminUserResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ResetPasswordResponse;
import com.cartethyia.easyorange.admin.application.service.AdminUserAppService;
import com.cartethyia.easyorange.admin.application.service.AdminUserSecurityAppService;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.security.AuthUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理后台-用户", description = "用户管理")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserAppService adminUserService;
    private final AdminUserSecurityAppService adminUserSecurityService;
    private final AdminUserAssembler assembler;

    @GetMapping
    public Result<PageResult<AdminUserResponse>> listUsers(AdminUserQueryRequest request) {
        return Result.success(assembler.toPageResponse(adminUserService.listUsers(assembler.toCondition(request))));
    }

    @GetMapping("/{id}")
    public Result<AdminUserResponse> getUserDetail(@PathVariable String id) {
        return Result.success(assembler.toResponse(adminUserService.getUserDetail(id)));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateUserStatus(
            @AuthenticationPrincipal AuthUser operator,
            @PathVariable String id,
            @Valid @RequestBody UpdateStatusRequest request) {
        adminUserService.updateUserStatus(id, request.status(), request.reason(), operator.userId());
        return Result.success();
    }

    @PutMapping("/{id}/unlock")
    public Result<Void> unlockUser(@AuthenticationPrincipal AuthUser operator, @PathVariable String id) {
        adminUserSecurityService.unlockUser(id, operator.userId());
        return Result.success();
    }

    @PutMapping("/{id}/reset-password")
    public Result<ResetPasswordResponse> resetPassword(
            @AuthenticationPrincipal AuthUser operator,
            @PathVariable String id,
            @Valid @RequestBody ResetPasswordRequest request) {
        String newPassword = adminUserSecurityService.resetPassword(id, request.reason(), operator.userId());
        return Result.success(assembler.toResetPasswordResponse(newPassword));
    }

    @PutMapping("/{id}/force-logout")
    public Result<Void> forceLogout(@PathVariable String id) {
        adminUserSecurityService.forceLogout(id);
        return Result.success();
    }

    @PutMapping("/{id}/role")
    public Result<Void> changeUserRole(
            @AuthenticationPrincipal AuthUser operator,
            @PathVariable String id,
            @Valid @RequestBody UserRoleRequest request) {
        adminUserSecurityService.changeUserRole(id, request.role(), request.reason(), operator.userId());
        return Result.success();
    }
}
