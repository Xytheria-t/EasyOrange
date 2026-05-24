package com.cartethyia.easyorange.admin.controller;

import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.admin.dto.request.ResetPasswordRequest;
import com.cartethyia.easyorange.admin.dto.request.UserRoleRequest;
import com.cartethyia.easyorange.admin.dto.request.UserUnlockRequest;
import com.cartethyia.easyorange.admin.dto.response.ResetPasswordResponse;
import com.cartethyia.easyorange.admin.service.AdminUserServiceExtension;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserControllerExtension {

    private final AdminUserServiceExtension adminUserServiceExtension;

    @PutMapping("/{id}/unlock")
    public Result<Void> unlockUser(
        @PathVariable Long id,
        @Valid @RequestBody UserUnlockRequest request
    ) {
        adminUserServiceExtension.unlockUser(id, request);
        return Result.success();
    }

    @PutMapping("/{id}/reset-password")
    public Result<ResetPasswordResponse> resetPassword(
        @PathVariable Long id,
        @Valid @RequestBody ResetPasswordRequest request
    ) {
        return Result.success(adminUserServiceExtension.resetPassword(id, request));
    }

    @PutMapping("/{id}/force-logout")
    public Result<Void> forceLogout(@PathVariable Long id) {
        adminUserServiceExtension.forceLogout(id);
        return Result.success();
    }

    @PutMapping("/{id}/role")
    public Result<Void> changeUserRole(
        @PathVariable Long id,
        @Valid @RequestBody UserRoleRequest request
    ) {
        adminUserServiceExtension.changeUserRole(id, request);
        return Result.success();
    }
}
