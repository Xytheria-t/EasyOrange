package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import com.cartethyia.easyorange.admin.adapter.inbound.web.assembler.AdminUserAssembler;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminUserQueryRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.ResetPasswordRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.UserRoleRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.UserStatusRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminUserResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ResetPasswordResponse;
import com.cartethyia.easyorange.admin.application.service.AdminUserAppService;
import com.cartethyia.easyorange.admin.application.service.AdminUserSecurityAppService;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "用户分页；支持关键词 / 用户类型 / 状态 / 注册时间范围过滤")
    public Result<PageResult<AdminUserResponse>> listUsers(AdminUserQueryRequest request) {
        return Result.success(assembler.toPageResponse(adminUserService.listUsers(assembler.toCondition(request))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "用户档案与登录信息；不存在时报 admin 侧 userNotFound")
    public Result<AdminUserResponse> getUserDetail(@PathVariable String id) {
        return Result.success(assembler.toResponse(adminUserService.getUserDetail(id)));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "改用户状态（NORMAL / DISABLED / LOCKED），reason 与操作人落库")
    public Result<Void> updateUserStatus(
            @AuthenticationPrincipal AuthUser operator,
            @PathVariable String id,
            @Valid @RequestBody UserStatusRequest request) {
        adminUserService.updateUserStatus(id, request.status(), request.reason(), operator.userId());
        return Result.success();
    }

    @PutMapping("/{id}/unlock")
    @Operation(summary = "解锁用户（仅 LOCKED / DISABLED 可解为 NORMAL），操作人落库")
    public Result<Void> unlockUser(@AuthenticationPrincipal AuthUser operator, @PathVariable String id) {
        adminUserSecurityService.unlockUser(id, operator.userId());
        return Result.success();
    }

    @PutMapping("/{id}/reset-password")
    @Operation(summary = "重置为随机新密码（明文仅此响应返回一次），并吊销该用户全部会话")
    public Result<ResetPasswordResponse> resetPassword(
            @AuthenticationPrincipal AuthUser operator,
            @PathVariable String id,
            @Valid @RequestBody ResetPasswordRequest request) {
        String newPassword = adminUserSecurityService.resetPassword(id, request.reason(), operator.userId());
        return Result.success(assembler.toResetPasswordResponse(newPassword));
    }

    @PutMapping("/{id}/force-logout")
    @Operation(summary = "强制该用户下线：吊销其全部有效会话")
    public Result<Void> forceLogout(@PathVariable String id) {
        adminUserSecurityService.forceLogout(id);
        return Result.success();
    }

    @PutMapping("/{id}/role")
    @Operation(summary = "变更用户角色（01 普通用户 / 02 管理员），生效后吊销其全部会话")
    public Result<Void> changeUserRole(
            @AuthenticationPrincipal AuthUser operator,
            @PathVariable String id,
            @Valid @RequestBody UserRoleRequest request) {
        adminUserSecurityService.changeUserRole(id, request.role(), request.reason(), operator.userId());
        return Result.success();
    }
}
