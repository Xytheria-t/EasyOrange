package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.ResetPasswordRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.UserRoleRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ResetPasswordResponse;
import com.cartethyia.easyorange.admin.service.AdminUserSecurityService;
import com.cartethyia.easyorange.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserControllerExtension {

    private final AdminUserSecurityService adminUserSecurityService;

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
