package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminUserQueryRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.ResetPasswordRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.UpdateStatusRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.UserRoleRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminUserResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ResetPasswordResponse;
import com.cartethyia.easyorange.admin.service.AdminUserSecurityService;
import com.cartethyia.easyorange.admin.service.AdminUserService;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理后台-用户", description = "用户管理")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final AdminUserSecurityService adminUserSecurityService;

    @GetMapping
    public Result<PageResult<AdminUserResponse>> listUsers(AdminUserQueryRequest request) {
        return Result.success(adminUserService.listUsers(request));
    }

    @GetMapping("/{id}")
    public Result<AdminUserResponse> getUserDetail(@PathVariable String id) {
        return Result.success(adminUserService.getUserDetail(id));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable String id, @Valid @RequestBody UpdateStatusRequest request) {
        adminUserService.updateUserStatus(id, request);
        return Result.success();
    }

    @PutMapping("/{id}/unlock")
    public Result<Void> unlockUser(@PathVariable String id) {
        adminUserSecurityService.unlockUser(id);
        return Result.success();
    }

    @PutMapping("/{id}/reset-password")
    public Result<ResetPasswordResponse> resetPassword(
            @PathVariable String id, @Valid @RequestBody ResetPasswordRequest request) {
        return Result.success(adminUserSecurityService.resetPassword(id));
    }

    @PutMapping("/{id}/force-logout")
    public Result<Void> forceLogout(@PathVariable String id) {
        adminUserSecurityService.forceLogout(id);
        return Result.success();
    }

    @PutMapping("/{id}/role")
    public Result<Void> changeUserRole(@PathVariable String id, @Valid @RequestBody UserRoleRequest request) {
        adminUserSecurityService.changeUserRole(id, request);
        return Result.success();
    }
}
